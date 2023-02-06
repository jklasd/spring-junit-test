package com.github.jklasd.test.common.interf.register;

import java.util.List;
import java.util.Map;

import com.github.jklasd.test.common.interf.ContainerRegister;

public interface Scan extends ContainerRegister{
	void scan();

	Map<String, Class<?>> findClassMap(String scanPath);

//	@Deprecated
//	void loadContextPathClass();

	Class<?> findClassByName(String beanName);

	Boolean isInScanPath(Class<?> requiredType);

	List<Class<?>> findClassExtendAbstract(Class<?> abstractClass);

	List<Class<?>> findClassImplInterface(Class<?> interfaceClass);
}
