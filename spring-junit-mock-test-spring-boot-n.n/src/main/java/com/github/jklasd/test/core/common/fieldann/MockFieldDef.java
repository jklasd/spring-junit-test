package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Field;

import com.github.jklasd.test.common.model.FieldDef;

import lombok.Getter;

public class MockFieldDef extends FieldDef{

	@Getter
	private Class<?> tetClass;
	
	public MockFieldDef(Field field, Object tagObj,Class<?> testClass) {
		super(field, tagObj);
		this.tetClass = testClass;
	}

}
