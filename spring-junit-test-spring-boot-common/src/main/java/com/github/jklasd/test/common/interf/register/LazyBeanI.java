package com.github.jklasd.test.common.interf.register;

import com.github.jklasd.test.common.interf.ContainerRegister;

public interface LazyBeanI extends ContainerRegister{

	Object processStatic(Class<?> staticClass);

	Object buildProxy(Class<?> type, String beanName);

}
