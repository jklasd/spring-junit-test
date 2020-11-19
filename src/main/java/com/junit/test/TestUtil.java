package com.junit.test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class TestUtil implements ApplicationContextAware{
	
	public TestUtil() {
		System.out.println("实例化TestUtil");
	}
	
	private static ApplicationContext staticApplicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		staticApplicationContext = applicationContext;		
		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
		Object bean = bf.getSingleton("org.springframework.context.annotation.internalAutowiredAnnotationProcessor");
		if(bean != null) {
			((AutowiredAnnotationBeanPostProcessor)bean).setRequiredParameterValue(false);
		}else {
			System.out.println("初始化失败TestUtil");
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Object getExistBean(Class classD,String beanName) {
		try {
			Object obj = staticApplicationContext.getBean(classD);
			return obj;
		} catch (NoUniqueBeanDefinitionException e) {
			if(beanName != null) {
				Object obj = staticApplicationContext.getBean(beanName);
				return obj;
			}
			return null;
		}
	}
	public static String getValue(String key) {
		String[] keys = key.split(":");
		String value = staticApplicationContext.getEnvironment().getProperty(keys[0]);
		if(StringUtils.isNotBlank(value)) {
			return value;
		}else {
			return keys.length>1?keys[1]:null;
		}
	}
	public static Object value(String key,Class type) {
		String value = getValue(key);
		try {
			if(type == null || type == String.class) {
				return	value;
			}else if(type == Integer.class) {
				return Integer.valueOf(value);
			}else if(type == Long.class) {
				return Long.valueOf(value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static void configBeanFactory(Class... classArg) {
		ApplicationContext context = new BeanFactory();
		for(Class c : classArg) {
			try {
				Object obj = c.newInstance();
				Method m = c.getDeclaredMethod("setApplicationContext", ApplicationContext.class);
				m.invoke(obj,context);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	static class BeanFactory implements ApplicationContext{
		@Override
		public Environment getEnvironment() {
			return staticApplicationContext.getEnvironment();
		}

		@Override
		public boolean containsBeanDefinition(String beanName) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getBeanDefinitionCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String[] getBeanDefinitionNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getBeanNamesForType(ResolvableType type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getBeanNamesForType(Class<?> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
				throws BeansException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
				throws BeansException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
				throws NoSuchBeanDefinitionException {
			return null;
		}

		@Override
		public Object getBean(String name) throws BeansException {
			if(staticApplicationContext.containsBean(name)) {
				return staticApplicationContext.getBean(name);
			}else {
				return LazyBean.findBean(name);
			}
		}

		@Override
		public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
			return null;
		}

		@Override
		public <T> T getBean(Class<T> requiredType) throws BeansException {
			return staticApplicationContext.getBean(requiredType);
		}

		@Override
		public Object getBean(String name, Object... args) throws BeansException {
			return staticApplicationContext.getBean(name, args);
		}

		@Override
		public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean containsBean(String name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getAliases(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public org.springframework.beans.factory.BeanFactory getParentBeanFactory() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean containsLocalBean(String name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void publishEvent(ApplicationEvent event) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void publishEvent(Object event) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Resource[] getResources(String locationPattern) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Resource getResource(String location) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getApplicationName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getStartupDate() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ApplicationContext getParent() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
