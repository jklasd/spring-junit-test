package com.github.jklasd.test.core.facade;

import java.util.Map;

import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Maps;

public class FacadeLoader {
	private static Map<Class<?>,Object> cacheFacadeBeanForClass = Maps.newConcurrentMap();
	@SuppressWarnings("unchecked")
	public static <T> T getFacadeUtil(Class<? extends T> facadeClass) throws InstantiationException, IllegalAccessException {
		if(!cacheFacadeBeanForClass.containsKey(facadeClass)) {
			initFacade(facadeClass);
		}
		return (T) cacheFacadeBeanForClass.get(facadeClass);
	}
	private synchronized static <T> void initFacade(Class<? extends T> facadeClass) throws InstantiationException, IllegalAccessException {
		if(!cacheFacadeBeanForClass.containsKey(facadeClass)) {
			cacheFacadeBeanForClass.put(facadeClass, facadeClass.newInstance());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getFacadeUtil(String className){
		Class<?> facadeClass = ScanUtil.loadClass(className);
		if(!cacheFacadeBeanForClass.containsKey(facadeClass)) {
			try {
				initFacade(facadeClass);
			} catch (Exception e) {
			}
		}
		return (T) cacheFacadeBeanForClass.get(facadeClass);
	}
	
}
