package com.github.jklasd.test.core.common.methodann.mock;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.MockClassHandler;

public class DubboMockHandler extends AbstractMockHandler implements MockClassHandler{
	
	private ThreadLocal<Class<?>[]> exclusions = new ThreadLocal<Class<?>[]>();
	
	@Override
	public String getType() {
		return JunitDubboMock.class.getName();
	}

	@Override
	public void hand(Method testMethod) {
		JunitDubboMock mockAnn = testMethod.getAnnotation(JunitDubboMock.class);
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
