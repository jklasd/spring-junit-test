package com.github.jklasd.test.ann;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.MockClassHandler;

public class SpecifyBeanClassHandler implements MockClassHandler{

	@Override
	public String getType() {
		return JunitSpecifyBean.class.getName();
	}

	@Override
	public void hand(Class<?> testClass) {
		
	}

	@Override
	public void releaseClass(Class<?> testClass) {
		
	}

	@Override
	public void hand(Method testMethod) {
		
	}

	@Override
	public void releaseMethod(Method testMethod) {
		
	}

	@Override
	public Boolean isMock() {
		return false;
	}

}
