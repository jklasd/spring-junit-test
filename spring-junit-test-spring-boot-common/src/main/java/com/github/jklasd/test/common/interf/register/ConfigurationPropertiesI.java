package com.github.jklasd.test.common.interf.register;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.ContainerRegister;

public interface ConfigurationPropertiesI extends ContainerRegister{

	void processConfigurationProperties(Object obj);

	void processConfigurationProperties(Object obj, Class<?> superClass);

	void processConfigurationProperties(Method method, Object tagObj);

}
