 package com.github.jklasd.test.common.interf.handler;

import com.github.jklasd.test.common.model.BeanModel;

public interface LazyPlugnBeanFactory {
	
	String getName();
	
	void afterPropertiesSet(Object obj,BeanModel model);
	
    Object buildBean(BeanModel model);

	boolean finded(BeanModel beanModel);
}
