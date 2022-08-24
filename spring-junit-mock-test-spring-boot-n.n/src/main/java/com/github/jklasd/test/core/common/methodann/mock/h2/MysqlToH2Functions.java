package com.github.jklasd.test.core.common.methodann.mock.h2;

public class MysqlToH2Functions {

	public static Boolean ISNULL_(Object a) {
		return a == null;
	}
	
	public static Object IF_(Boolean pass,Object a,Object b) {
		return pass?a:b;
	}
	
}
