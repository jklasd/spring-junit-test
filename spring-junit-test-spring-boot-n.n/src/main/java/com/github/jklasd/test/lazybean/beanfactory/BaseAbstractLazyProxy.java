package com.github.jklasd.test.lazybean.beanfactory;

import java.util.Map;

import com.github.jklasd.test.common.model.BeanModel;

import lombok.Getter;

public abstract class BaseAbstractLazyProxy {
	@Getter
    protected Map<String,Object> attr;
	@Getter
    protected BeanModel beanModel;
}
