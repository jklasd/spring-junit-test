 package com.github.jklasd.test.lazyplugn;

import com.github.jklasd.test.common.model.BeanModel;

public interface LazyPlugnBeanFactory {
	
	void afterPropertiesSet(Object obj,BeanModel model);
	
    Object buildBean(BeanModel model);

	boolean finded(BeanModel beanModel);
}
