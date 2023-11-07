package com.github.jklasd.test.common.interf.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.FieldDef;

public interface MockFieldHandlerI extends ContainerRegister{
	void registId();
	public void hand(Class<?> testClass);
	public void releaseClass(Class<?> testClass);
	public void injeckMock(FieldDef fieldDef);
	public Object getMockBean(Class<?> tagClass,String name);
	public boolean finded(BeanModel beanModel);
	Object invoke(Object poxy, Method method, Object[] param, BeanModel beanModel) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
}
