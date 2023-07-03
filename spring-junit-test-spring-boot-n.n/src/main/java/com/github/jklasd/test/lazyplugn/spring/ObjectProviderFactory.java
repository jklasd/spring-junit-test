package com.github.jklasd.test.lazyplugn.spring;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.springframework.beans.factory.ObjectProvider;

import com.github.jklasd.test.common.interf.register.ObjectProviderBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectProviderFactory implements ObjectProviderBuilder, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4892046356245533830L;

	@Override
	public String getBeanKey() {
		return ObjectProviderBuilder.class.getName();
	}

	@Override
	public <T> ObjectProvider<T> buildObjectProviderImpl(Type type) {
		return new ObjectProviderImpl<T>(type);
	}

}
