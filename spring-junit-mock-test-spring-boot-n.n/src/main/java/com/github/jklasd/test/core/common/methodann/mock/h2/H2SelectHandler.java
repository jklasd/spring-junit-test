package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.MockClassHandler;
import com.github.jklasd.test.core.common.methodann.mock.AbstractMockHandler;

/**
 * 数据源切换
 * 
 * 使用时，切换到H2数据库
 * @author jubin.zhang
 *
 */
public class H2SelectHandler extends AbstractMockHandler implements MockClassHandler{

	@Override
	public String getType() {
		return JunitH2Selected.class.getName();
	}

	@Override
	public void hand(Method testMethod) {
		JunitH2Selected selected = testMethod.getAnnotation(JunitH2Selected.class);
		useMethodMock.set(selected.value());
	}

	@Override
	public void releaseMethod(Method testMethod) {
		useMethodMock.remove();
	}

	@Override
	public void hand(Class<?> testClass) {
		JunitH2Selected selected = testClass.getAnnotation(JunitH2Selected.class);
		useClassMock.set(selected.value());
	}

	@Override
	public void releaseClass(Class<?> testClass) {
		useClassMock.remove();
	}
}
