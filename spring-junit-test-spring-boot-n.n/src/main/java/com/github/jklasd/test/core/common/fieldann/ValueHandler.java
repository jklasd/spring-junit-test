package com.github.jklasd.test.core.common.fieldann;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.beans.factory.annotation.Value;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.common.FieldAnnComponent;
import com.github.jklasd.test.core.common.FieldAnnComponent.FieldHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValueHandler  implements FieldHandler{
	TestUtil util = TestUtil.getInstance();
	public void handler(FieldDef def,Annotation ann){
		Field attr = def.getField();
		Object obj = def.getTagObj();
		Value v = (Value) ann;
		if (v != null) {
			FieldAnnComponent.setObj(attr, obj, util.value(v.value().replace("${", "").replace("}", ""), attr.getType()));
		}
	}
	public String getType() {
		return Value.class.getName();
	}
}
