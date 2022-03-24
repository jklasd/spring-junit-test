package com.github.jklasd.test.core.facade.processor;

import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanFactoryProcessor {
	private static BeanFactoryProcessor processor;
	public static BeanFactoryProcessor getInstance() {
		if(processor==null) {
			buildBean();
		}
		return processor;
	}
	private synchronized static void buildBean() {
		if(processor==null) {
			processor = new BeanFactoryProcessor();
		}
	}
//	List<BeanFactoryPostProcessor> processorList = Lists.newArrayList();
	Set<Class<?>> filter = Sets.newConcurrentHashSet();
	LazyApplicationContext lazyApplicationContext = TestUtil.getInstance().getApplicationContext();
	public void loadProcessor(Class<?> configClass) {
		synchronized(lazyApplicationContext.getBeanFactoryPostProcessors()) {
			try {
				if(filter.contains(configClass)) {
					return;
				}
				BeanFactoryPostProcessor newProcessor = (BeanFactoryPostProcessor) configClass.newInstance();
				lazyApplicationContext.addBeanFactoryPostProcessor(newProcessor);
				filter.add(configClass);
			} catch (InstantiationException | IllegalAccessException e) {
				log.error("loadProcessor",e);
			}
		}
	}


	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException{
		lazyApplicationContext.getBeanFactoryPostProcessors()
		.forEach(processor->processor.postProcessBeanFactory(beanFactory));
	}
	public boolean notBFProcessor(Class<?> classItem) {
		return !filter.contains(classItem);
	}
}