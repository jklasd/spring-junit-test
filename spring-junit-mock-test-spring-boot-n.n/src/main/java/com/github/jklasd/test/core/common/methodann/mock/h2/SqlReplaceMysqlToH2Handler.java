package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.MockClassHandler;
import com.github.jklasd.test.core.common.methodann.mock.AbstractMockHandler;

public class SqlReplaceMysqlToH2Handler extends AbstractMockHandler implements MockClassHandler{
	
	@Override
	public String getType() {
		return MysqlToH2.class.getName();
	}

	@Override
	public void hand(Method testMethod) {
		MysqlToH2 selected = testMethod.getAnnotation(MysqlToH2.class);
		mysqlToH2Mock.set(selected);
	}

	@Override
	public void releaseMethod(Method testMethod) {
		mysqlToH2Mock.remove();
	}

	@Override
	public void hand(Class<?> testClass) {
//		MysqlToH2 selected = testClass.getAnnotation(MysqlToH2.class);
//		mysqlToH2Mock.set(selected);
	}

	@Override
	public void releaseClass(Class<?> testClass) {
		mysqlToH2Mock.remove();
	}
	
	public MysqlToH2 getData() {
		return mysqlToH2Mock.get();
	}

}
