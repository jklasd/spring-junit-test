package com.github.jklasd.test.core.common.methodann.mock;

import com.github.jklasd.test.common.interf.handler.MockAnnHandler;

public abstract class AbstractMockHandler implements MockAnnHandler{
	
	protected ThreadLocal<Boolean> useClassMock = new ThreadLocal<Boolean>();
	protected ThreadLocal<Boolean> useMethodMock = new ThreadLocal<Boolean>();
	
	public Boolean isMock() {
		if(useMethodMock.get()!=null) {//如果设定方法，在优先判断方法
			return useMethodMock.get();
		}else {
			return useClassMock.get()!=null && useClassMock.get();
		}
	}
}
