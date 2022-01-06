package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyConfigurationPropertiesBindingPostProcessor;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanInitHandler {
	private static BeanInitHandler lazyBean = new BeanInitHandler();
	private BeanInitHandler() {}
	public static BeanInitHandler getInstance() {
		return lazyBean;
	}
	/**
	 * 对目标对象方法进行处理
	 * @param obj 目标对象
	 * @param ms 方法组
	 * @param sup 父类
	 * 
	 * 主要处理 
	 * 【1】PostConstruct注解方法
	 * 【2】setApplicationContext
	 * 
	 * 当目标对象存在父类时，遍历所有父类对相应方法进行处理
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public void processMethod(Object obj, Method[] ms,Class<?> sup) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(sup != null) {
			ms = sup.getDeclaredMethods();
			processMethod(obj, ms, sup.getSuperclass());
		}
		if(ScanUtil.isImple(obj.getClass(), ApplicationContextAware.class)) {
			for (Method m : ms) {
				if(m.getName().equals("setApplicationContext")//当对象方法存是setApplicationContext
						&& (sup == null || !sup.getName().contains("AbstractJUnit4SpringContextTests"))) {
					try {
						if(m!=null) {
							try {
								m.invoke(obj,TestUtil.getInstance().getApplicationContext());
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								log.error("不能注入applicationContext",e);
							}
						}
					} catch (SecurityException e) {
					}
				}
			}
		}
		processSpringAnnMethod(obj, ms);
	}
	protected void processSpringAnnMethod(Object obj, Method[] ms)
			throws IllegalAccessException, InvocationTargetException {
		if(AbastractLazyProxy.isProxy(obj))
			return;
		for (Method m : ms) {
		    Type[] paramTypes = m.getGenericParameterTypes();
			if (m.getAnnotation(PostConstruct.class) != null) {//当实际对象存在初始化方法时。
				try {
					if (!m.isAccessible()) {
						m.setAccessible(true);
					}
					m.invoke(obj, null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("初始化方法执行异常{}#{}",obj,m);
					log.error("初始化方法执行异常",e);
				}
			}else if(m.getAnnotation(Autowired.class) != null) {
			    String bName = m.getAnnotation(Qualifier.class)!=null?m.getAnnotation(Qualifier.class).value():null;
			    Object[] param = processParam(m, paramTypes, bName);
                Object tmp = m.invoke(obj, param);
//                if(tmp!=null) {
//                    util.getApplicationContext().registBean(bName, tmp, tmp.getClass());
//                }
			}else if(m.getAnnotation(Value.class) != null) {
			    Value aw = m.getAnnotation(Value.class);
			    if(paramTypes.length>0) {
			        Type type = paramTypes[0];
			        Object param = TestUtil.getInstance().value(aw.value(), (Class<?>)type);
			        m.invoke(obj, param);
			    }else {
			        m.invoke(obj);
			    }
			    
            }else if(m.getAnnotation(Resource.class) != null) {
                Resource aw = m.getAnnotation(Resource.class);
                Object[] param = processParam(m, paramTypes, aw.name());
                Object tmp = m.invoke(obj, param);
                if(tmp!=null) {
//                    util.getApplicationContext().registBean(aw.name(), tmp, tmp.getClass());
                }
            }else if(m.getAnnotation(Bean.class) != null) {
                Bean aw = m.getAnnotation(Bean.class);
                String beanName = null;
                if(aw.value().length>0) {
                	beanName = aw.value()[0];
                }else if(aw.name().length>0){
                	beanName = aw.name()[0];
                }else {
                	beanName = m.getName();
                }
                if(beanName.equals("buildConsumerConfig")) {
                	log.debug("短点");
                }
                Object exitBean = TestUtil.getInstance().getApplicationContext().getBean(beanName);
                if(exitBean!=null) {
                	return;
                }
                Object[] param = processParam(m, paramTypes, null);
                Object tmp = m.invoke(obj, param);
                if(tmp!=null) {
                    ConfigurationProperties confPro = m.getAnnotation(ConfigurationProperties.class);
                    if(confPro!=null) {
                        LazyConfigurationPropertiesBindingPostProcessor.processConfigurationProperties(tmp,confPro);
                    }
                    TestUtil.getInstance().getApplicationContext().registBean(aw.value().length>0?aw.value()[0]:m.getName(), tmp, tmp.getClass());
                }
            }
		}
	}
	

    private Object[] processParam(Method m, Type[] paramTypes, String bName) {
        Object[] param = new Object[paramTypes.length];
        for(int i=0;i<paramTypes.length;i++) {
            Class<?> c = getParamType(m, paramTypes[i]);
            if(paramTypes[i] == List.class) {
                param[i] = LazyBean.findListBean(c);
            }else {
                if(LazyBean.existBean(c) && TestUtil.getInstance().getExistBean(c, m.getName())!=null) {
                    param[i] = TestUtil.getInstance().getExistBean(c, m.getName());
                }else {
                    param[i] = LazyBean.getInstance().buildProxy(c,bName);
                }
            }
        }
        return param;
    }
    
    private static Class<?> getParamType(Method m, Type paramType) {
		if(paramType instanceof ParameterizedType) {
			ParameterizedType  pType = (ParameterizedType) paramType;
			Type[] item = pType.getActualTypeArguments();
			if(item.length == 1) {
				//处理一个集合注入
				try {
					log.info("注入集合=>{}",m.getName());
					return Class.forName(item[0].getTypeName());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}else {
				log.info("其他特殊情况");
			}
		}else {
			return (Class<?>) paramType;
		}
		return null;
	}
}
