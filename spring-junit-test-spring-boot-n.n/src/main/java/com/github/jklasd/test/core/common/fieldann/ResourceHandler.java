package com.github.jklasd.test.core.common.fieldann;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.annotation.Resource;

import com.github.jklasd.test.core.common.FieldAnnComponent;
import com.github.jklasd.test.core.common.FieldAnnComponent.FieldHandler;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceHandler  implements FieldHandler{
	public void handler(FieldDef def,Annotation ann){
		Field attr = def.getField();
		Object obj = def.getTagObj();
		Resource c = (Resource) ann;
		if (c != null) {
			FieldAnnComponent.setObj(attr, obj, LazyBean.getInstance().buildProxy(attr.getType(),c.name()));
		}
	}
	public String getType() {
		return Resource.class.getName();
	}
}
