package com.github.jklasd.test.core.common.methodann.mock;

import com.github.jklasd.test.common.interf.handler.MockAnnHandler;

public abstract class AbstractMockHandler implements MockAnnHandler{
	
	protected ThreadLocal<Boolean> useMock = new ThreadLocal<Boolean>();
	
	public Boolean isMock() {
		return useMock.get()!=null && useMock.get();
	}
}
