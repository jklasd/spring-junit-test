package com.github.jklasd.test.common.interf.handler;

public abstract class AbstractDbHandler implements MockClassHandler{
	
	protected InheritableThreadLocal<Boolean> useClassMock = new InheritableThreadLocal<Boolean>();
	protected InheritableThreadLocal<Boolean> useMethodMock = new InheritableThreadLocal<Boolean>();
	
	public Boolean isMock() {
		if(useMethodMock.get()!=null) {//如果设定方法，在优先判断方法
			return useMethodMock.get();
		}else {
			return useClassMock.get()!=null && useClassMock.get();
		}
	}
}
