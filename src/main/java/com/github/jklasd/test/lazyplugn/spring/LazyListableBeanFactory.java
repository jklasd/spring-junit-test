package com.github.jklasd.test.lazyplugn.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.OrderComparator.OrderSourceProvider;
import org.springframework.core.ResolvableType;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

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
