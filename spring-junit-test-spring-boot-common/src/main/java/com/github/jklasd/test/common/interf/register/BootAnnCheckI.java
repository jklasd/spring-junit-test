package com.github.jklasd.test.common.interf.register;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.ContainerRegister;

public interface BootAnnCheckI extends ContainerRegister{

	boolean checkClassExists(Class<?> configClass);

	boolean checkClassExistsForMethod(Method m);

	boolean checkProp(Class<?> staticClass);

}
