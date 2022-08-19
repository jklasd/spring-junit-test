package com.github.jklasd.test.common.interf.handler;

import com.github.jklasd.test.common.model.FieldDef;

public interface MockFieldHandlerI {
	public void hand(Class<?> testClass);
	public void releaseClass(Class<?> testClass);
	public void injeckMock(FieldDef fieldDef);
}
