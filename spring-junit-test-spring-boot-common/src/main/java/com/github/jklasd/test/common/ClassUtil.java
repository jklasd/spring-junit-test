package com.github.jklasd.test.common;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassUtil {
	private static ClassUtil util = new ClassUtil();
	private ClassUtil() {}
	
	public static ClassUtil getInstance() {
		return util;
	}
	
	/**
	 * 个别自定义类
	 * @param configClass
	 */
	public boolean hasStaticMethod(Class<?> configClass) {
		try {
			Method[] methods = configClass.getDeclaredMethods();
			return Lists.newArrayList(methods).stream().anyMatch(m->{
				if(Modifier.isStatic(m.getModifiers())
						&& !m.getName().contains("lambda$")//非匿名方法
						&& !m.getName().contains("access$")) {//非匿名方法
					Class<?> returnType = m.getReturnType();
					if(!returnType.getName().contains("void")) {
						log.debug("method=>{}",m);
						return true;
					}
				}
				return false;
			});
		}catch(Exception e) {
			log.error("hasStaticMethod",e);
		}
		return false;
	}
}
