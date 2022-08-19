package com.github.jklasd.test.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.interf.ContainerRegister;
import com.google.common.collect.Maps;

public final class ContainerManager {
	public static int init = 1;
	public static int inited = 2;
	public static volatile int stats;
	
	public static final class NameConstants{
		public final static String MockFieldHandler = "com.github.jklasd.test.core.common.fieldann.MockFieldHandler";
	}
	
	private static Map<String,Object> componentContainer = Maps.newHashMap();
	public static void registComponent(ContainerRegister component) {
		componentContainer.put(component.getBeanKey(), component);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getComponent(String beanKey) {
		return (T) componentContainer.get(beanKey);
	}
	
	public static class HandlerLoader{
		public static void load(String... handlerClasses) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
			for(String hclass :handlerClasses) {
				Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
				Constructor<?> constructor = handlerClass.getDeclaredConstructors()[0];
				if(!constructor.isAccessible()) {
					constructor.setAccessible(true);
				}
				ContainerRegister handler = (ContainerRegister) constructor.newInstance();
				if(StringUtils.isNotBlank(handler.getBeanKey())) {
					handler.register();
				}
			}
		}
	}
}
