package com.junit.test;

import java.lang.reflect.Type;
import java.util.Map;

import lombok.Data;

@Data
public class AssemblyUtil {
	/**
	 * 目标类
	 */
	private Class<?> tagClass;
	
	private String beanName;
	
	/**
	 * 存在泛型对象
	 */
	private Type[] classGeneric;
	
	@SuppressWarnings("rawtypes")
	private Map<String,Class> nameMapTmp;
}
