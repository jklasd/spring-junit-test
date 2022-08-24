package com.github.jklasd.test.common.interf.register;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.github.jklasd.test.common.interf.ContainerRegister;

public interface BeanFactoryProcessorI extends ContainerRegister{
	
	public void loadProcessor(Class<?> configClass);
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory);
	public boolean notBFProcessor(Class<?> classItem);
}
