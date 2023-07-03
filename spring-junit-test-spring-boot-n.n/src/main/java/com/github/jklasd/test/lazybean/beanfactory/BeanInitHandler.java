package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.google.common.collect.Sets;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class BeanInitHandler {
	private static BeanInitHandler lazyBean = new BeanInitHandler();
	private BeanInitHandler() {}
	public static BeanInitHandler getInstance() {
		return lazyBean;
	}
	private CommonAnnotationBeanPostProcessor commonAnnotationBeanPostProcessor = new CommonAnnotationBeanPostProcessor();
	@Builder
	@Getter
	public static class Param{
		Object obj;
		Method[] ms;
		Class<?> sup;
		boolean hasStatic;
	}
	private Set<Object> initObj = Sets.newConcurrentHashSet();
	protected void processSpringAnnMethod(Param handlerParam) throws IllegalAccessException, InvocationTargetException {
		Object obj = handlerParam.getObj();
		boolean isStatic = handlerParam.isHasStatic();
		if(LazyProxyManager.isProxy(obj)) {
			if(isStatic) {//假如是存在静态的代理对象，则需要进行预热处理
				LazyProxyManager.instantiateProxy(obj);
			}
			return;
		}
		if(!initObj.contains(obj)) {//查看父类会重复执行这个方法
			initObj.add(obj);
			//不能被重复执行
			commonAnnotationBeanPostProcessor.postProcessBeforeInitialization(obj, null);
		}
//		ReflectionUtils.doWithLocalMethods(obj.getClass(), method -> {
//		});
		Method[] ms = handlerParam.getMs();
		for (Method m : ms) {
		    Type[] paramTypes = m.getGenericParameterTypes();
			if(m.getAnnotation(Value.class) != null) {
			    Value aw = m.getAnnotation(Value.class);
			    if(paramTypes.length>0) {
			        Type type = paramTypes[0];
			        Object param = TestUtil.getInstance().valueFromEnvForAnnotation(aw.value(), (Class<?>)type);
			        m.invoke(obj, param);
			    }else {
			        m.invoke(obj);
			    }
			    
            }else if(m.getAnnotation(Resource.class) != null) {
            	if(m.getReturnType() != Void.class
            			&& m.getReturnType() != void.class) {
            		return;
            	}
                Resource aw = m.getAnnotation(Resource.class);
                String beanName = aw.name();
                Object[] param = processParam(m, paramTypes);
                Object tmp = m.invoke(obj, param);
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
	public void processMethod(Param param) throws IllegalAccessException, InvocationTargetException {
		if(ScanUtil.isImple(param.getObj().getClass(), ApplicationContextAware.class)) {
			for (Method m : param.getMs()) {
				if(m.getName().equals("setApplicationContext")//当对象方法存是setApplicationContext
						&& (param.getSup() == null || !param.getSup().getName().contains("AbstractJUnit4SpringContextTests"))) {
					try {
						if(m!=null) {
							try {
								m.invoke(param.getObj(),LazyApplicationContext.getInstance());
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								log.error("不能注入applicationContext",e);
							}
						}
					} catch (SecurityException e) {
					}
				}
			}
		}
		processSpringAnnMethod(param);
	}
	public void processMethod(BeanInitModel model) {
		boolean isStatic = model.isStatic();
		if(ScanUtil.isImple(model.getTagClass(), ApplicationContextAware.class)) {
			ReflectionUtils.doWithLocalMethods(model.getTagClass(), method -> {
				if(method.getName().equals("setApplicationContext")) {
					try {
						method.invoke(model.getObj(),LazyApplicationContext.getInstance());
					} catch (InvocationTargetException e) {
						log.error("不能注入applicationContext",e);
					}
				}
			});
//			try {
//				model.getTagClass().getDeclaredMethods();
//				Method setApplicationContext = model.getTagClass().getDeclaredMethod("setApplicationContext", ApplicationContext.class);
//				if(!setApplicationContext.isAccessible()) {
//					setApplicationContext.setAccessible(true);
//				}
//				setApplicationContext.invoke(model.getObj(),LazyApplicationContext.getInstance());
//			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
//				log.error("不能注入applicationContext",e);
//			}
		}
		
		Object obj = model.getObj();
		
		if(LazyProxyManager.isProxy(obj)) {
			if(isStatic) {//假如是存在静态的代理对象，则需要进行预热处理
				LazyProxyManager.instantiateProxy(obj);
			}
			return;
		}
		if(!initObj.contains(obj)) {//查看父类会重复执行这个方法
			initObj.add(obj);
			//不能被重复执行
			commonAnnotationBeanPostProcessor.postProcessBeforeInitialization(obj, model.getBeanName());
		}
		ReflectionUtils.doWithLocalMethods(model.getTagClass(), method -> {
			try {
				Type[] paramTypes = method.getGenericParameterTypes();
				if(method.getAnnotation(Value.class) != null) {
				    Value aw = method.getAnnotation(Value.class);
				    if(paramTypes.length>0) {
				        Type type = paramTypes[0];
				        Object param = TestUtil.getInstance().valueFromEnvForAnnotation(aw.value(), (Class<?>)type);
				        method.invoke(obj, param);
				    }else {
				        method.invoke(obj);
				    }
				    
	            }else if(method.getAnnotation(Resource.class) != null) {
	            	if(method.getReturnType() != Void.class
	            			&& method.getReturnType() != void.class) {
	            		return;
	            	}
	                Resource aw = method.getAnnotation(Resource.class);
	                String beanName = aw.name();
	                Object[] param = processParam(method, paramTypes);
	                Object tmp = method.invoke(obj, param);
	            }else if(method.getAnnotation(Autowired.class) != null) {
	            	if(method.getReturnType() != Void.class
	            			&& method.getReturnType() != void.class) {
	            		return;
	            	}
	                Object[] param = processParam(method, paramTypes);
	                Object tmp = method.invoke(obj, param);
	            }
			} catch (Exception e) {
				log.warn("执行初始化方法异常",e);
			}
		});
	}
}
