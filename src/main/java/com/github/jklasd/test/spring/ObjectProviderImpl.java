package com.github.jklasd.test.spring;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectProviderImpl implements ObjectProvider<Object>, Serializable{
	/**
     *
     */
    private static final long serialVersionUID = 3004906006576222643L;
    private Type type;
	public ObjectProviderImpl(Type type) {
		this.type = type;
	}

	@Override
	public Object getObject() throws BeansException {
		return null;
	}

	@Override
	public Object getObject(Object... args) throws BeansException {
		return null;
	}

	@Override
	public Object getIfAvailable() throws BeansException {
		if(type != null) {
			Class tagC = (Class) type;
			try {
				Class builderC = Class.forName(tagC.getName()+"$Builder");
				Method[] ms = builderC.getDeclaredMethods();
				for(Method m : ms) {
					if(m.getReturnType() == tagC) {
						return m.invoke(builderC.newInstance());
					}
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("ObjectProvider#getIfAvailable",e);
			}
		}
		return null;
	}

	@Override
	public Object getIfUnique() throws BeansException {
		return null;
	}

}
