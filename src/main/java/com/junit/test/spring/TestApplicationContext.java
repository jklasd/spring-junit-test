package com.junit.test.spring;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.StandardServletEnvironment;

import com.junit.test.ScanUtil;

public class TestApplicationContext implements ApplicationContext{

	private ApplicationContext parentContext;
	public TestApplicationContext(ApplicationContext context) {
		this.parentContext = context;
	}
	
	private Properties properties;
	
	@Override
	public Environment getEnvironment() {
		if(parentContext == null || parentContext == this) {
			StandardServletEnvironment env = new StandardServletEnvironment();
			try {
				Resource[] resources = ScanUtil.getResources("application.properties");
				properties = new Properties();
				if(resources.length>0) {
					properties.load(resources[0].getInputStream());
					env.getPropertySources().addFirst(new PropertiesPropertySource("local", properties));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return env;
		}
		return parentContext.getEnvironment();
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

	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {
		return ScanUtil.findBeanWithAnnotation(annotationType);
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {
		return null;
	}

	@Override
	public Object getBean(String name) throws BeansException {
		if(parentContext == null || parentContext == this) {
			return ScanUtil.findBean(name);
		}
		if(parentContext.containsBean(name)) {
			return parentContext.getBean(name);
		}else {
			return ScanUtil.findBean(name);
		}
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return null;
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		try {
			if(parentContext == null || parentContext == this) {
				return (T)ScanUtil.findBean(requiredType);
			}
			Object bean = parentContext.getBean(requiredType);
			return (T) bean;
		} catch (NoSuchBeanDefinitionException e) {
			return (T)ScanUtil.findBean(requiredType);
		}
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		if(parentContext == null || parentContext == this) {
			return null;
		}
		return parentContext.getBean(name, args);
	}

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
		return ScanUtil.getResources(locationPattern);
	}

	@Override
	public Resource getResource(String location) {
		try {
			return ScanUtil.getRecource(location);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	public Properties getProperties() {
		return properties;
	}

}
