package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;

import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.model.BeanModel;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{
	
	protected ThreadLocal<BeanDefinition> localCache = new ThreadLocal<>();
	
	static Map<Class<?>,LazyAbstractPlugnBeanFactory> instance = Maps.newHashMap();
	synchronized static LazyAbstractPlugnBeanFactory getInstanceByClass(Class<?> beanPlugn) {
		if(instance.containsKey(beanPlugn)) {
			return instance.get(beanPlugn);
		}
		try {
			LazyAbstractPlugnBeanFactory tmp = (LazyAbstractPlugnBeanFactory) beanPlugn.newInstance();
			instance.put(beanPlugn, tmp);
			return tmp;
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("getInstance#"+beanPlugn,e);
		}
		return null;
	}
	
	@Override
	public void afterPropertiesSet(Object obj,BeanModel model) {
		// TODO Auto-generated method stub
		
	}
}
