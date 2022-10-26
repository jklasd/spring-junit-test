package com.github.jklasd.test.common.model;

import java.lang.reflect.Method;

import lombok.Data;

@Data
public class JunitMethodDefinition {
	
	private String beanName;
	
	private Class<?> returnType;
	
	private Class<?> configurationClass;
	
	private Method method;
}
