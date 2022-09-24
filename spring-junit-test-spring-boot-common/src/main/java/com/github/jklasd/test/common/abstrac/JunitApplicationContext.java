package com.github.jklasd.test.common.abstrac;

import org.springframework.context.support.GenericApplicationContext;

import com.github.jklasd.test.common.interf.ContainerRegister;

public abstract class JunitApplicationContext extends GenericApplicationContext implements ContainerRegister{

	public JunitApplicationContext(JunitListableBeanFactory instance) {
		super(instance);
	}
	/**
	 * 
	 * @param beanName
	 * @param value
	 * @param type
	 */
	public abstract void registProxyBean(String beanName, Object proxy, Class<?> type);

	/**
	 * 获取已换成的代理对象
	 * @param beanName
	 * @param tagClass
	 * @return
	 */
	public abstract Object getProxyBeanByClassAndBeanName(String beanName, Class<?> tagClass);
	/**
	 * 获取已缓存的代理对象
	 * @param tagClass
	 * @return
	 */
	public abstract Object getProxyBeanByClass(Class<?> tagClass);

}
