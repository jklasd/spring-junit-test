package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Constructor;

import com.github.jklasd.test.common.util.ScanUtil;

public abstract class AbstractMockHandler{
	protected String packagePath = "org.springframework.boot.test.mock.mockito";
	protected Class<?> QualifierDefinition = ScanUtil.loadClass(packagePath+".QualifierDefinition");
	protected Constructor<?> qualDefStructor;
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
