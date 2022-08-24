package com.github.jklasd.test.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 由于AOP的使用场景比较少，在单元测试中对Aop的支持需要指定。减少bean构建的扫描
 * @author jubin.zhang
 *
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface JunitSpecifyAop {
	/**
	 * 指定class
	 * @return
	 */
	Class<?>[] value();
}
