package com.github.jklasd.test.common.abstrac;

import org.springframework.context.support.GenericApplicationContext;

import com.github.jklasd.test.common.interf.ContainerRegister;

public abstract class JunitApplicationContext extends GenericApplicationContext implements ContainerRegister{

	public JunitApplicationContext(JunitListableBeanFactory instance) {
		super(instance);
	}
	public abstract void registBean(String beanName, Object value, Class<?> type);

	public abstract Object getBeanByClassAndBeanName(String beanName, Class<?> tagClass);
	public abstract Object getBeanByClass(Class<?> tagClass);

}
