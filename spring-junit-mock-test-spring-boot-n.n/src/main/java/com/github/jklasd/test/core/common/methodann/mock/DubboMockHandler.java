package com.github.jklasd.test.core.common.methodann.mock;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.MockAnnHandler;

public class DubboMockHandler extends AbstractMockHandler implements MockAnnHandler{
	
	private ThreadLocal<Class<?>[]> exclusions = new ThreadLocal<Class<?>[]>();
	
	@Override
	public String getType() {
		return DubboMock.class.getName();
	}

	@Override
	public void hand(Method testMethod) {
		DubboMock mockAnn = testMethod.getAnnotation(DubboMock.class);
		useMethodMock.set(true);
		exclusions.set(mockAnn.exclusions());
	}

	@Override
	public void releaseMethod(Method testMethod) {
		useMethodMock.remove();
		exclusions.remove();
	}

	@Override
	public void hand(Class<?> testClass) {
		
	}

	@Override
	public void releaseClass(Class<?> testClass) {
		
	}

}
