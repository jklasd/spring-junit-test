package com.github.jklasd.test.core.common.fieldann.mock;

import java.lang.reflect.Constructor;

import com.github.jklasd.test.common.ScanUtil;

public abstract class MockHandler{
	String packagePath = "org.springframework.boot.test.mock.mockito";
	protected Class<?> QualifierDefinition = ScanUtil.loadClass(packagePath+".QualifierDefinition");
	Constructor<?> qualDefStructor;
	{
		try {
			Constructor<?>[] qstructors = QualifierDefinition.getDeclaredConstructors();
			qualDefStructor = qstructors[0];
			qualDefStructor.setAccessible(true);
		} catch (SecurityException | IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
}
