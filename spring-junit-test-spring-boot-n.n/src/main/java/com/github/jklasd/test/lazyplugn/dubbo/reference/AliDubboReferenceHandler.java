package com.github.jklasd.test.lazyplugn.dubbo.reference;

import java.lang.annotation.Annotation;

import com.github.jklasd.test.common.model.FieldDef;

public class AliDubboReferenceHandler extends AbstractReferenceHandler{

	@Override
	public String getType() {
		return "com.alibaba.dubbo.config.annotation.Reference";
	}
}
