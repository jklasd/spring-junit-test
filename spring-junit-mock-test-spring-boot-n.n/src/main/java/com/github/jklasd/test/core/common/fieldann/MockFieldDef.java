package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Field;

import com.github.jklasd.test.common.model.FieldDef;

import lombok.Getter;

public class MockFieldDef extends FieldDef{

	@Getter
	private Class<?> testClass;
	@Getter
	private String beanName;
	
	public MockFieldDef(Field field, Object tagObj,Class<?> testClass,String beanName) {
		super(field, tagObj);
		this.testClass = testClass;
		this.beanName = beanName;
	}

}
