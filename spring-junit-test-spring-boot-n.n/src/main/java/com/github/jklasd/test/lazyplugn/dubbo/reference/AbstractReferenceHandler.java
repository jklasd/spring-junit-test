package com.github.jklasd.test.lazyplugn.dubbo.reference;

import java.lang.annotation.Annotation;

import com.github.jklasd.test.core.common.FieldAnnComponent;
import com.github.jklasd.test.core.common.FieldAnnComponent.FieldHandler;
import com.github.jklasd.test.core.common.fieldann.FieldDef;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboAnnotationRefHandler;

public abstract class AbstractReferenceHandler implements FieldHandler{
	@Override
	public void handler(FieldDef def, Annotation ann) {
		Object ref = LazyDubboAnnotationRefHandler.getInstance().buildBeanRef(def.getField().getType(),ann);
		if(ref!=null) {
			FieldAnnComponent.setObj(def.getField(), def.getTagObj(),ref);
		}
	}
}
