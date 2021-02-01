package com.junit.test.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.junit.test.ScanUtil;
import com.junit.test.TestUtil;
import com.junit.test.spring.LazyConfigurationPropertiesBindingPostProcessor;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class LazyMongoBean {
	
	public static boolean isMongo(Class c) {
		return c.getPackage().getName().contains("springframework.data.mongodb");
	}

	private static Map<Class,Object> cacheBean = Maps.newHashMap();
	
	public static Object buildBean(Class classBean, String beanName){
		if(cacheBean.containsKey(classBean)) {
			return cacheBean.get(classBean);
		}
		try {
			Class mp = classBean.getClassLoader().loadClass("org.springframework.boot.autoconfigure.mongo.MongoProperties");
			Object mpObj = TestUtil.buildBean(mp);
			LazyConfigurationPropertiesBindingPostProcessor.processConfigurationProperties(mpObj);
			TestUtil.registerBean(mpObj);
			
			Class mac = classBean.getClassLoader().loadClass("org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration");
			Object macObj = TestUtil.buildBean(mac);
			scope.add(macObj);
			Class mdac = classBean.getClassLoader().loadClass("org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration");
			Object mdacObj = TestUtil.buildBean(mdac);
			scope.add(mdacObj);
			
			buildBeanForScope(classBean);
			return cacheBean.get(classBean);
		} catch (ClassNotFoundException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	private static Set<Object> scope = Sets.newHashSet();
	private static Object buildBeanForScope(Class c) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		if(cacheBean.containsKey(c)) {
			return cacheBean.get(c);
		}
		if(c == BeanFactory.class) {
			return TestUtil.getExistBean(ApplicationContext.class);
		}
		for(Object obj : scope) {
			Method[] ms = obj.getClass().getDeclaredMethods();
			for(Method m : ms) {
				if(m.getReturnType() == c || (c.isInterface() && ScanUtil.isImple(m.getReturnType(), c))) {
					Parameter[] ps = m.getParameters();
					Object[] args = new Object[ps.length];
					for(int i=0;i<ps.length;i++) {
						args[i] = buildBeanForScope(ps[i].getType());
					}
					cacheBean.put(c,m.invoke(obj, args));
					return cacheBean.get(c);
				}
			}
		}
		return null;
	}
}
