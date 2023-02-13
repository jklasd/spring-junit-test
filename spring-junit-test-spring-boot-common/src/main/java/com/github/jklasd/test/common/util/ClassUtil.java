package com.github.jklasd.test.common.util;

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
	 * @param tagClass 需要检查的类
	 * @return 存在静态方法
	 */
	public boolean hasStaticMethod(Class<?> tagClass) {
		try {
			Method[] methods = tagClass.getDeclaredMethods();
			return Lists.newArrayList(methods).stream().anyMatch(m->{
				if(Modifier.isStatic(m.getModifiers())
						&& !m.getName().equals("main")//非main方法
						&& !m.getName().contains("lambda$")//非匿名方法
						&& !m.getName().contains("access$")//非匿名方法
						&& !m.getName().startsWith("$")) {//非代理类的方法
					Class<?> returnType = m.getReturnType();
					if(!returnType.getName().contains("void")) {
						log.debug("class=>{},method=>{}",tagClass,m);
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
