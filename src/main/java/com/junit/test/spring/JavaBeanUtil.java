package com.junit.test.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.Maps;
import com.junit.test.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaBeanUtil {
	private static Map<Class,Object> factory = Maps.newHashMap();
	private static Map<String,Object> cacheBean = Maps.newHashMap();
	public static Object buildBean(Class<?> c, Method m, Class classBean, String beanName) {
		String key = classBean+"=>beanName:"+beanName;
		if(cacheBean.containsKey(key)) {
			return cacheBean.get(key);
		}
		try {
			if(!factory.containsKey(c)) {
				factory.put(c, c.newInstance());
				LazyBean.processAttr(factory.get(c), c);
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		//configration
		Object obj = factory.get(c);
		if(obj!=null) {
			try {
				//如果存在参数
				if(m.getParameterCount()>0) {
					Class[] paramTypes = m.getParameterTypes();
					log.warn("存在参数，需要处理");
				}
				Object tagObj = m.invoke(obj);
				cacheBean.put(key, tagObj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return cacheBean.get(key);
	}

}
