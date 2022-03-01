package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Field;

import javax.annotation.Resource;

import com.github.jklasd.test.core.common.FieldAnnUtil;
import com.github.jklasd.test.core.common.FieldAnnUtil.FieldHandler;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceHandler  implements FieldHandler{
	public void handler(FieldDef def){
		Field attr = def.getField();
		Object obj = def.getTagObj();
		Resource c = attr.getAnnotation(Resource.class);
		if (c != null) {
			FieldAnnUtil.setObj(attr, obj, LazyBean.getInstance().buildProxy(attr.getType(),c.name()));
		}
	}
	public String getType() {
		return Resource.class.getName();
	}
}
