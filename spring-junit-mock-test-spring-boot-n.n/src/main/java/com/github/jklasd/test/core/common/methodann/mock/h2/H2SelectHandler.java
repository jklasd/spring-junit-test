package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.MockAnnHandler;
import com.github.jklasd.test.core.common.methodann.mock.AbstractMockHandler;

/**
 * 数据源切换
 * 
 * 使用时，切换到H2数据库
 * @author jubin.zhang
 *
 */
public class H2SelectHandler extends AbstractMockHandler implements MockAnnHandler{

	@Override
	public String getType() {
		return H2Select.class.getName();
	}

	@Override
	public void hand(Method testMethod) {
		useMock.set(true);
	}

	@Override
	public void releaseMethod(Method testMethod) {
		useMock.remove();
	}
}
