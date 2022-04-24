package com.github.jklasd.test.common;

import java.util.Map;

import com.github.jklasd.test.common.interf.ContainerRegister;
import com.google.common.collect.Maps;

public final class ContainerManager {
	public static int init = 1;
	public static int inited = 2;
	public static volatile int stats;
	
	private static Map<String,Object> componentContainer = Maps.newHashMap();
	public static void registComponent(ContainerRegister component) {
		componentContainer.put(component.getBeanKey(), component);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getComponent(String beanKey) {
		return (T) componentContainer.get(beanKey);
	}
}
