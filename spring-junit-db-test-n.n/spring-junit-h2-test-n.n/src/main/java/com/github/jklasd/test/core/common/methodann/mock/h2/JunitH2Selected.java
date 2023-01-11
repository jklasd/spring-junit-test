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
	/**
	 * 是否使用H2数据库
	 * @return
	 */
	boolean value() default true;
	
	/**
	 * 当定义在class上时使用，控制当前表示当前junit class用到的数据源写入。<br/>
	 * 
	 * insertResource,寻址源头是从src/test/resources去找，路径可以自定义
	 * 
	 * 表的创建建议统一写到db-h2/schema <br/>
	 * db-h2/data下也可以写一些插入公共数据
	 * @return
	 */
	String[] insertResource() default "";
}
