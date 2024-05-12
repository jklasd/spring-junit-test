package com.github.jklasd.test.common;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.runner.RunnerI;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ContainerManager {
	public static int init = 1;
	public static int inited = 2;
	public static volatile int stats;
	
	public static final class NameConstants{
		public final static String SqlInterceptor = "com.github.jklasd.test.core.common.methodann.mock.h2.SqlInterceptor";
	}
	
	private static Map<String,Object> componentContainer = Maps.newHashMap();
	
	public static Object createAndregistComponent(Class<?> componentClass) throws InstantiationException, IllegalAccessException {
		Object obj = componentClass.newInstance();
		if(obj instanceof ContainerRegister) {
			registComponent((ContainerRegister) obj);
		}
		return obj;
	}
	
	public static void registComponent(ContainerRegister component) {
		log.debug("组件注册:{}",component.getBeanKey());
		componentContainer.put(component.getBeanKey(), component);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getComponent(String beanKey) {
		return (T) componentContainer.get(beanKey);
	}
	
	public static <T> T getComponent(Class<T> beanClass) {
		return (T) componentContainer.get(beanClass.getName());
	}
	
	public static class HandlerLoader{
		public static void load(String... handlerClasses){
			try {
				for(String hclass :handlerClasses) {
					Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
					Constructor<?> constructor = handlerClass.getDeclaredConstructors()[0];
					if(!constructor.isAccessible()) {
						constructor.setAccessible(true);
					}
					ContainerRegister handler = (ContainerRegister) constructor.newInstance();
					if(StringUtils.isNotBlank(handler.getBeanKey())) {
						ContainerManager.registComponent( handler);
					}
				}
			} catch (Exception e) {
				log.error("handlerClasses->{}",JSONObject.toJSONString(handlerClasses));
				throw new JunitException(e);
			}
		}
	}

	public static void execu(Class<?> containerClass,RunnerI runner) {
		Object anni = getComponent(containerClass);
		if(anni!=null) {
			runner.execute(anni);
		}
	}

	public static void bootLoadComp() throws InstantiationException, IllegalAccessException {
		List<String> bootConfigurationClass = Lists.newArrayList("com.github.jklasd.test.springboot.configuration.ann.ConfigurationAnnImple",
				"com.github.jklasd.test.springboot.configuration.BootAnnCheckImpl",
				"com.github.jklasd.test.springboot.configuration.ann.ConfigurationAnnImple");
		for (String classpath : bootConfigurationClass) {
			ContainerManager.createAndregistComponent(JunitClassLoader.getInstance().junitloadClass(classpath));
		}
	}
}
