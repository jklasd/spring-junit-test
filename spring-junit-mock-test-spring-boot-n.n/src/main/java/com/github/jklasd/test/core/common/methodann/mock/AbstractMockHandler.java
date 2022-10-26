package com.github.jklasd.test.core.common.methodann.mock;

import com.github.jklasd.test.common.interf.handler.MockClassHandler;
import com.github.jklasd.test.core.common.methodann.mock.h2.JunitMysqlToH2;

public abstract class AbstractMockHandler implements MockClassHandler{
	
	protected InheritableThreadLocal<Boolean> useClassMock = new InheritableThreadLocal<Boolean>();
	protected InheritableThreadLocal<Boolean> useMethodMock = new InheritableThreadLocal<Boolean>();
	protected InheritableThreadLocal<JunitMysqlToH2> mysqlToH2Mock = new InheritableThreadLocal<>();
	
	public Boolean isMock() {
		if(useMethodMock.get()!=null) {//如果设定方法，在优先判断方法
			return useMethodMock.get();
		}else {
			return useClassMock.get()!=null && useClassMock.get();
		}
	}
}
