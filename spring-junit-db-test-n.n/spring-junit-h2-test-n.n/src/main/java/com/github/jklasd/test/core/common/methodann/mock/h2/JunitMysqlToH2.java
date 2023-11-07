package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,ElementType.TYPE})//指定位置
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JunitMysqlToH2 {

	String[] from();
	
	String[] to();
}
