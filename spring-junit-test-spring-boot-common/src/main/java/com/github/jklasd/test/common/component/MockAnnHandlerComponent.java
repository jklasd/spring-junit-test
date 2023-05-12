package com.github.jklasd.test.common.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.handler.MockClassHandler;
import com.github.jklasd.test.common.interf.handler.MockFieldHandlerI;
import com.google.common.collect.Maps;

public class MockAnnHandlerComponent extends AbstractComponent{
	private static Map<String,MockClassHandler> handlerMap = Maps.newHashMap();
	public static void handlerMethod(Method method) {
//		try {
//			PryMethodInfo methodInfo = MethodSnoopUtil.findNotPublicMethodForClass(method);
//			if(!methodInfo.getFindToStatic().isEmpty()) {
//				//处理静态方法
//				methodInfo.getFindToStatic().forEach(tagClass->lazyBean.processStatic(tagClass));
//			}
//		} catch (Exception e) {
//		}
		Annotation[] anns = method.getAnnotations();
		for(Annotation ann : anns) {
			MockClassHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.hand(method);
			}
		}
	}
	
	public static void releaseMethod(Method method) {
		Annotation[] anns = method.getAnnotations();
		for(Annotation ann : anns) {
			MockClassHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.releaseMethod(method);
			}
		}
	}
	
	public static MockClassHandler getHandler(String handlerKey) {
		return handlerMap.get(handlerKey);
	}
	
	public static Boolean isMock(String handlerKey) {
		return handlerMap.containsKey(handlerKey) && handlerMap.get(handlerKey).isMock();
	}

	public static void handlerClass(Class<?> testClass) {
		Annotation[] anns = testClass.getAnnotations();
		for(Annotation ann : anns) {
			MockClassHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.hand(testClass);
			}
		}
		if(testClass.getSuperclass()!=null && testClass.getSuperclass()!=Object.class) {
			handlerClass(testClass.getSuperclass());
		}
		MockFieldHandlerI injectMocksHandler = ContainerManager.getComponent(MockFieldHandlerI.class.getName());
		if(injectMocksHandler!=null) {
			injectMocksHandler.hand(testClass);
		}
	}

	public static void releaseClass(Class<?> testClass) {
		Annotation[] anns = testClass.getAnnotations();
		for(Annotation ann : anns) {
			MockClassHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.releaseClass(testClass);
			}
		}
		if(testClass.getSuperclass()!=null && testClass.getSuperclass()!=Object.class) {
			releaseClass(testClass.getSuperclass());
		}
		MockFieldHandlerI injectMocksHandler = ContainerManager.getComponent(MockFieldHandlerI.class.getName());
		if(injectMocksHandler!=null) {
			injectMocksHandler.releaseClass(testClass);
		}
	}

	public static void beforeAll(Class<?> testClass) {
		MockFieldHandlerI injectMocksHandler = ContainerManager.getComponent(MockFieldHandlerI.class.getName());
		if(injectMocksHandler!=null) {
			injectMocksHandler.registId();
		}
	}

	@Override
	<T> void add(T component) {
		MockClassHandler handler = (MockClassHandler) component;
		if(StringUtils.isNotBlank(handler.getType())) {
			handlerMap.put(handler.getType(), handler);
		}
	}
}
