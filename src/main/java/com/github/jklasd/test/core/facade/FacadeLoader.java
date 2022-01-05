package com.github.jklasd.test.core.facade;

import java.util.Map;

import com.google.common.collect.Maps;

public class FacadeLoader {
	private static Map<String,Object> cacheFacadeBean = Maps.newConcurrentMap();
	public static <T> T getFacadeUtil(Class<? extends T> facadeClass) {
		if(!cacheFacadeBean.containsKey(facadeClass)) {
			initFacade(facadeClass);
		}
		return null;
	}
	private synchronized static <T> void initFacade(Class<? extends T> facadeClass) {
		if(!cacheFacadeBean.containsKey(facadeClass)) {
		}
	}
	
	private synchronized static void initFacade(String beanName) {
	}
	
	public static <T> T getFacadeUtil(Class<? extends T> facadeClass,String beanName) {
		if(!cacheFacadeBean.containsKey(beanName)) {
			initFacade(beanName);
		}
		return null;
	}
	
}
