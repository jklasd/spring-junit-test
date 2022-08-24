package com.github.jklasd.test.common.interf.handler;

import java.lang.annotation.Annotation;

import com.github.jklasd.test.common.model.FieldDef;

public interface FieldHandler{
	public String getType();
	public void handler(FieldDef def, Annotation ann);
	default public int order() {return 0;};
	default public void injeckMock(FieldDef fieldDef, Annotation ann) {};
}
