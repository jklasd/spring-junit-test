package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据源切换
 * 
 * 使用时，切换到H2数据库
 * @author jubin.zhang
 *
 */
@Target({ElementType.METHOD,ElementType.TYPE})//指定位置
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JunitH2Selected {
	boolean value() default true; 
}
