package com.junit.test.db;

import java.util.Map;

import com.google.common.collect.Maps;
import com.junit.test.ScanUtil;
import com.junit.test.TestUtil;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class LazyMongoBean {
	private final static String MONGO_PATH = "springframework.data.mongodb";
	public static boolean isMongo(Class c) {
		return c.getPackage().getName().contains(MONGO_PATH);
	}

	private static Map<Class,Object> cacheBean = Maps.newHashMap();
	
	public static Object buildBean(Class classBean, String beanName){
		if(cacheBean.containsKey(classBean)) {
			return cacheBean.get(classBean);
		}
		Object obj = ScanUtil.findCreateBeanFromFactory(classBean, beanName, ScanUtil.findClassMap(ScanUtil.BOOT_AUTO_CONFIG));
		
		return obj;
	}
}
