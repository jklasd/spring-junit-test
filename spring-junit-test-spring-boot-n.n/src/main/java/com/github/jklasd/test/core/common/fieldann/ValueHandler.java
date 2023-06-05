package com.github.jklasd.test.core.common.fieldann;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.beans.factory.annotation.Value;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;
import com.github.jklasd.test.common.model.FieldDef;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValueHandler  implements FieldHandler{
	public void handler(FieldDef def,Annotation ann){
		TestUtil util = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
		Field attr = def.getField();
		Object obj = def.getTagObj();
		Value v = (Value) ann;
		if (v != null) {
			FieldAnnComponent.setObj(attr, obj, util.valueFromEnvForAnnotation(v.value(), attr.getGenericType()));
		}
	}
	public String getType() {
		return Value.class.getName();
	}
}
