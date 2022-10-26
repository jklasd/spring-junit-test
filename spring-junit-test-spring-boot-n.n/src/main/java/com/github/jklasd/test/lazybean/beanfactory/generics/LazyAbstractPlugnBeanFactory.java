package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.util.Map;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{
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
	
	public void load(Map<String, BeanDefParser> parser) {}
	
	@Override
	public void afterPropertiesSet(Object obj,BeanModel model) {
		// TODO Auto-generated method stub
		
	}
}
