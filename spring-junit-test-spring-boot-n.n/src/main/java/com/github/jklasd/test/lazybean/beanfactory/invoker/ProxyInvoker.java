package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.model.BeanModel;

public interface ProxyInvoker {
	public void setNextInvoker(ProxyInvoker invoker);
	public Object invoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws Exception, Throwable;
}
