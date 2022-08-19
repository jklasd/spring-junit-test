package com.github.jklasd.test.lazyplugn.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.springframework.util.StringUtils;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.abstrac.JunitListableBeanFactory;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.AbstractLazyProxy;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
	
	/**
	 * TODO 待优化
	 */
	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		Map<String, T> result = super.getBeansOfType(type);
		
		//获取名字
		String[] beanNameArr = StringUtils.toStringArray(beanNameSet);
		for(String beanName:beanNameArr) {
			Object bean = getBean(beanName);
			if(type.isAssignableFrom(AbstractLazyProxy.getProxyTagClass(bean))) {
				result.put(beanName, (T) bean);
			}
		}
		
		if(result.isEmpty()) {
			//获取子类
			List<Class<?>> subClass = ScanUtil.findClassExtendAbstract(type);
			subClass.forEach(subC ->{
				Object bean = LazyBean.getInstance().buildProxy(subC);
				result.put(BeanNameUtil.getBeanName(subC), (T) bean);
			});
		}
		
		return result;
	}
	
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
		}else {
			List<T> list = Lists.newArrayList();
			cacheClassMap.entrySet().forEach(entry->{
				if(entry.getKey().isAssignableFrom(requiredType)) {
					list.add((T) entry.getValue());
				}
			});
			if(list.size()>1) {
				log.warn("{}=>找到两个对象",requiredType);
				return list.get(0);
			}
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
	
	Set<String> beanNameSet = Sets.newHashSet();
	
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		beanNameSet.add(beanName);
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

	protected void releaseBean(Class<?> tagC, Object tmp) {
		cacheProxyBean.entrySet().removeIf(entry->entry.getValue()==tmp);
		
		cacheClassMap.remove(tagC);
	}
}
