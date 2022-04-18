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

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.abstrac.JunitListableBeanFactory;
import com.github.jklasd.test.lazybean.beanfactory.AbstractLazyProxy;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyListableBeanFactory extends JunitListableBeanFactory {
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
			if(AbstractLazyProxy.isProxy(autowiredValue)) {//代理bean不注册到spring容器中
				return;
			}
		}
		super.registerResolvableDependency(dependencyType, autowiredValue);
	}
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		if(cacheClassMap.containsKey(requiredType)) {
			return (T) cacheClassMap.get(requiredType);
		}
		//存在jdbcTemplate、 mongoTemplate等bean需要获取
//		else if(requiredType.getName().startsWith("org.springframework")) {
//			return null;
//		}
		return super.getBean(requiredType);
	}
	
	Map<String,Object> cacheProxyBean = Maps.newHashMap();
	
	public Object getBean(String beanName) {
		if(super.containsBean(beanName) || !cacheProxyBean.containsKey(beanName)) {
			return super.getBean(beanName);
		}
		return cacheProxyBean.get(beanName);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String beanName, Class<T> requiredType) throws BeansException {
		if(super.containsBean(beanName) || !cacheProxyBean.containsKey(beanName)) {
			return super.getBean(beanName,requiredType);
		}
		return (T) cacheProxyBean.get(beanName);
	}
//	public <T> T getBean(Class<T> requiredType) throws BeansException {
//		if(cacheClassMap.containsKey(requiredType)) {
//			return (T) cacheClassMap.get(requiredType);
//		}
//		Object obj = super.getBean(requiredType);
//		if(obj == null) {
//			obj = LazyBean.getInstance().findBean(requiredType); 
//		}
//		return (T) obj;
//	}
	
	public boolean containsBean(String name) {
		if(cacheProxyBean.containsKey(name)) {
			return true;
		}
		return super.containsBean(name);
	}
	
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		if(AbstractLazyProxy.isProxy(singletonObject)) {
			cacheProxyBean.put(beanName, singletonObject);
			return;
		}
		super.registerSingleton(beanName, singletonObject);
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

	@Override
	public void register() {
		ContainerManager.registComponent( this);
	}
	
	@Override
	public String getBeanKey() {
		return JunitListableBeanFactory.class.getSimpleName();
	}
}
