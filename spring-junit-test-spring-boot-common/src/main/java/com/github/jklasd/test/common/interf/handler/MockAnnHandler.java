package com.github.jklasd.test.common.interf.handler;

import java.lang.reflect.Method;

public interface MockAnnHandler {
	public String getType();
	public void hand(Method testMethod);
	public void releaseMethod(Method testMethod);
	public Boolean isMock();

}
