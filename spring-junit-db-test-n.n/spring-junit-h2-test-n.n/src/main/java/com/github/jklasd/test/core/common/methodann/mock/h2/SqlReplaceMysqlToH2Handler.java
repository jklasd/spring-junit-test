package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.AbstractDbHandler;
import com.github.jklasd.test.common.interf.handler.MockClassHandler;

public class SqlReplaceMysqlToH2Handler extends AbstractDbHandler implements MockClassHandler{
	
	protected InheritableThreadLocal<JunitMysqlToH2> mysqlToH2Mock = new InheritableThreadLocal<>();
	
	@Override
	public String getType() {
		return JunitMysqlToH2.class.getName();
	}

	@Override
	public void hand(Method testMethod) {
		JunitMysqlToH2 selected = testMethod.getAnnotation(JunitMysqlToH2.class);
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
	
	public JunitMysqlToH2 getData() {
		return mysqlToH2Mock.get();
	}

}
