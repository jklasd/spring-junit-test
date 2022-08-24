package com.github.jklasd.test.common.interf.handler;

import java.lang.reflect.Method;

public interface MockClassHandler {
	public String getType();
	public void hand(Class<?> testClass);
	public void releaseClass(Class<?> testClass);
	public void hand(Method testMethod);
	public void releaseMethod(Method testMethod);
	public Boolean isMock();

}
