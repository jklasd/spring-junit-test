package com.github.jklasd.test.common.abstrac;

import org.springframework.context.support.GenericApplicationContext;

public abstract class JunitApplicationContext extends GenericApplicationContext
{

	public JunitApplicationContext(JunitListableBeanFactory instance) {
		super(instance);
	}
	/**
	 * 
	 * @param beanName beanId
	 * @param proxy 代理对象
	 * @param type 代理对象对应的类
	 */
	public abstract void registProxyBean(String beanName, Object proxy, Class<?> type);

	/**
	 * 获取已缓存的代理对象
	 * @param beanName bean
	 * @param tagClass	bean对应的类
	 * @return	返回代理对象
	 */
	public abstract Object getProxyBeanByClassAndBeanName(String beanName, Class<?> tagClass);
	/**
	 * 获取已缓存的代理对象
	 * @param tagClass bean对应的类
	 * @return 返回代理对象
	 */
	public abstract Object getProxyBeanByClass(Class<?> tagClass);

}
