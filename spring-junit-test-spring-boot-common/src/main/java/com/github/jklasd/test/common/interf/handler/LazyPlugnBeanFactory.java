 package com.github.jklasd.test.common.interf.handler;

import com.github.jklasd.test.common.model.BeanModel;

public interface LazyPlugnBeanFactory {
	
	String getName();
	/**
	 * 配置拓展实例bean构造器顺序，小到大执行
	 * @return
	 */
	default Integer getOrder() {
		return 0;
	}
	
	default boolean isInterfaceBean() {
		return false;
	}
	
	default boolean isClassBean() {
		return false;
	}
	
	
	void afterPropertiesSet(Object obj,BeanModel model);
	
    Object buildBean(BeanModel model);

	boolean finded(BeanModel beanModel);
	
	default boolean canBeInstance(){
		return true;
	}
}
