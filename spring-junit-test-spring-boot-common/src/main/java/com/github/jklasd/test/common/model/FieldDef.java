package com.github.jklasd.test.common.model;

import java.lang.reflect.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FieldDef {
	private Field field;
	private Object tagObj;
}
