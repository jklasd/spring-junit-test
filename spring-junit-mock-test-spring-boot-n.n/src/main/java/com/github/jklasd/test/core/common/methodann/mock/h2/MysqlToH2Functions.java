package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.util.Date;

public class MysqlToH2Functions {

	public static Date DATEADD(Date date,Object type) {
		return date;
	}
	
	public static Date CURDATE() {
		return new Date();
	}
}
