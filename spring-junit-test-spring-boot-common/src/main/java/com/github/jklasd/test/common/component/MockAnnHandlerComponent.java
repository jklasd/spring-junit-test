package com.github.jklasd.test.common.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.handler.MockAnnHandler;
import com.google.common.collect.Maps;

public class MockAnnHandlerComponent {
	private static Map<String,MockAnnHandler> handlerMap = Maps.newHashMap();
	public static class HandlerLoader{
		public static void load(String... handlerClasses) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
			for(String hclass :handlerClasses) {
				Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
				MockAnnHandler handler = (MockAnnHandler) handlerClass.newInstance();
				if(StringUtils.isNotBlank(handler.getType())) {
					handlerMap.put(handler.getType(), handler);
				}
			}
		}
	}
	public static void handlerMethod(Method method) {
		Annotation[] anns = method.getAnnotations();
		for(Annotation ann : anns) {
			MockAnnHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.hand(method);
			}
		}
	}
	
	public static void releaseMethod(Method method) {
		Annotation[] anns = method.getAnnotations();
		for(Annotation ann : anns) {
			MockAnnHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.releaseMethod(method);
			}
		}
	}
	
	public static Boolean isMock(String handlerKey) {
		return handlerMap.containsKey(handlerKey) && handlerMap.get(handlerKey).isMock();
	}

	public static void handlerClass(Class<?> testClass) {
		Annotation[] anns = testClass.getAnnotations();
		for(Annotation ann : anns) {
			MockAnnHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.hand(testClass);
			}
		}
		if(testClass.getSuperclass()!=null && testClass.getSuperclass()!=Object.class) {
			handlerClass(testClass.getSuperclass());
		}
	}

	public static void releaseClass(Class<?> testClass) {
		Annotation[] anns = testClass.getAnnotations();
		for(Annotation ann : anns) {
			MockAnnHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.releaseClass(testClass);
			}
		}
		if(testClass.getSuperclass()!=null && testClass.getSuperclass()!=Object.class) {
			releaseClass(testClass.getSuperclass());
		}
	}
}
