package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.lazyplugn.spring.configprop.LazyConfPropBind;
import com.github.jklasd.test.util.ScanUtil;

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
		if(sup != null && sup!=Object.class) {
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
			    Object[] param = processParam(m, paramTypes);
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
                String beanName = aw.name();
                Object[] param = processParam(m, paramTypes);
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
//                if(beanName.equals("buildConsumerConfig")) {
//                	log.debug("短点");
//                }
                boolean exitBean = TestUtil.getInstance().getApplicationContext().containsBean(beanName);
                if(exitBean) {
                	return;
                }
                if (!m.isAccessible()) {
					m.setAccessible(true);
				}
                Object[] param = processParam(m, paramTypes);
                Object tmp = m.invoke(obj, param);
                if(tmp!=null) {
                    ConfigurationProperties confPro = m.getAnnotation(ConfigurationProperties.class);
                    if(confPro!=null) {
                    	LazyConfPropBind.processConfigurationProperties(tmp,confPro);
                    }
                    TestUtil.getInstance().getApplicationContext().registBean(aw.value().length>0?aw.value()[0]:m.getName(), tmp, tmp.getClass());
                }
            }
		}
	}
	

    private Object[] processParam(Method m, Type[] paramTypes) {
    	Object[] param = new Object[paramTypes.length];
    	try {
    		for(int i=0;i<paramTypes.length;i++) {
    			BeanModel bm = getParamType(m, paramTypes[i]);
//    			if(bm.getTagClass() == List.class) {
//    				param[i] = LazyBean.findListBean(bm.getClassGeneric()[0].getClass());
//    			}else{
    				param[i] = LazyBean.getInstance().buildProxy(bm);
//    			}
    		}
    		return param;
    	}catch(Exception e) {
    		throw new JunitException(e,true);
    	}
    }
    private static BeanModel getParamType(Method m, Type paramType) {
    	BeanModel model = new BeanModel();
    	if(paramType instanceof ParameterizedType) {
    		ParameterizedType  pType = (ParameterizedType) paramType;
			Type[] item = pType.getActualTypeArguments();
			if(item.length == 1) {
				//处理一个集合注入
				model.setTagClass((Class<?>) pType.getRawType());
				model.setClassGeneric(item);
			}else {
				log.info("其他特殊情况");
			}
    	}else {
    		model.setTagClass((Class<?>) paramType);
		}
    	return model;
    }
}
