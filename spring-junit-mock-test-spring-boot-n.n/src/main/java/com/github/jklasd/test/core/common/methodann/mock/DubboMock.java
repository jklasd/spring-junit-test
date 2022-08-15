package com.github.jklasd.test.core.common.methodann.mock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将单元测试方法调用的所有dubbo bean都切换到mock形式
 * @author jubin.zhang
 *
 */
@Target(ElementType.METHOD)//指定位置
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DubboMock {
	/**
	 * 排除某写接口，不走mock，走集成方式
	 * @return
	 */
	Class<?>[] exclusions() default{}; 
}
