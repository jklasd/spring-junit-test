package com.github.jklasd.test.lazybean.model;

import java.lang.reflect.Type;
import java.util.Map;

import lombok.Data;

@Data
public class AssemblyDTO {
	/**
	 * 目标类
	 */
	private Class<?> tagClass;
	
	private String beanName;
	
	/**
	 * 存在泛型对象
	 */
	private Type[] classGeneric;
	
	private Map<String,Class<?>> nameMapTmp;
	
	public String toString(){
	    return "tagClass=>"+tagClass+";beanName=>"+beanName+";classGeneric=>"+classGeneric;
	}
}
