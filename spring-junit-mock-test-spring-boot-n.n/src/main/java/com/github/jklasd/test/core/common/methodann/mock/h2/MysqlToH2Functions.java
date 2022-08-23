package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.sql.Date;
import java.util.Calendar;

public class MysqlToH2Functions {

	public static Boolean ISNULL(Object a) {
		return a == null;
	}
	
	public static Object IF_(Boolean pass,Object a,Object b) {
		return pass?a:b;
	}
	
}
