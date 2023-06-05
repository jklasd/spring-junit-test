package com.github.jklasd.test.common.interf.register;

import java.lang.reflect.Type;

import org.springframework.beans.factory.ObjectProvider;

import com.github.jklasd.test.common.interf.ContainerRegister;

public interface ObjectProviderBuilder extends ContainerRegister{

	<T> ObjectProvider<T> buildObjectProviderImpl(Type type);
}
