package com.github.jklasd.test.core.facade.processor;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.register.BeanFactoryProcessorI;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanFactoryProcessor implements BeanFactoryProcessorI{
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
			processor.register();
		}
	}
//	List<BeanFactoryPostProcessor> processorList = Lists.newArrayList();
	Set<Class<?>> filter = Sets.newConcurrentHashSet();
	LazyApplicationContext lazyApplicationContext = (LazyApplicationContext) TestUtil.getInstance().getApplicationContext();
	public void loadProcessor(Class<?> configClass) {
		synchronized(lazyApplicationContext.getBeanFactoryPostProcessors()) {
			try {
				if(filter.contains(configClass)) {
					return;
				}
				BeanFactoryPostProcessor newProcessor = (BeanFactoryPostProcessor) configClass.newInstance();
				
				//注入相关
//				LazyBean.getInstance().processAttr(newProcessor, configClass);
				
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
	@Override
	public void register() {
		ContainerManager.registComponent(this);
	}
	@Override
	public String getBeanKey() {
		return BeanFactoryProcessorI.class.getSimpleName();
	}
}
