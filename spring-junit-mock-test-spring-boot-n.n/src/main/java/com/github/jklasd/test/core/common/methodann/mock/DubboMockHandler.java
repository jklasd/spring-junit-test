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
		useMock.set(true);
		exclusions.set(mockAnn.exclusions());
	}

	@Override
	public void releaseMethod(Method testMethod) {
		useMock.remove();
		exclusions.remove();
	}

}
