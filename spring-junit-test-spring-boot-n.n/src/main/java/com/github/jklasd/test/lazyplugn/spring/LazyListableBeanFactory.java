package com.github.jklasd.test.lazyplugn.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.OrderComparator.OrderSourceProvider;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyListableBeanFactory extends DefaultListableBeanFactory {
	protected LazyListableBeanFactory() {}
	private static LazyListableBeanFactory beanFactory = new LazyListableBeanFactory();
	public static LazyListableBeanFactory getInstance() {
		return beanFactory;
	}

//    public LazyListableBeanFactory(BeanFactory arg0) {
//        super(arg0);
//    }
	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
        super.registerBeanDefinition(beanName, beanDefinition);
	}
	Map<Class<?>,Object> cacheClassMap = Maps.newConcurrentMap();
	public void registerResolvableDependency(Class<?> dependencyType, Object autowiredValue) {
		Assert.notNull(dependencyType, "Dependency type must not be null");
		if(autowiredValue!=null) {
			cacheClassMap.putIfAbsent(dependencyType, autowiredValue);
		}
		super.registerResolvableDependency(dependencyType, autowiredValue);
	}
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		if(cacheClassMap.containsKey(requiredType)) {
			return (T) cacheClassMap.get(requiredType);
		}else if(requiredType.getName().startsWith("org.springframework")) {
			return null;
		}
		return super.getBean(requiredType);
	}
	
//	private void registerAnnBean(String beanName, BeanDefinition beanDefinition) {
//		log.debug("registerAnnBean registerBeanDefinition===={}", beanName);
//		super.registerBeanDefinition(beanName, beanDefinition);
//	}
	
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
		Object obj = super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
		LazyBean.getInstance().processAttr(obj, obj.getClass());
		return obj;
	}
	
//	protected void resetBeanDefinition(String beanName) {
//		super.resetBeanDefinition(beanName);
//	}

	public String[] getBeanNamesForTypedStream(Type requiredType) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, ResolvableType.forRawClass((Class<?>) requiredType));
	}
	
	public OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProviderExt(Map<String, ?> beans) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method createFactoryAwareOrderSourceProvider = DefaultListableBeanFactory.class.getDeclaredMethod("createFactoryAwareOrderSourceProvider", Map.class);
		if(createFactoryAwareOrderSourceProvider.isAccessible()) {
			createFactoryAwareOrderSourceProvider.setAccessible(true);
		}
		return (OrderSourceProvider) createFactoryAwareOrderSourceProvider.invoke(this,beans);
	}
}
