package com.github.jklasd.test.springboot.configuration;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.register.BootAnnCheckI;

public class BootAnnCheckImpl implements BootAnnCheckI{

	@Override
	public String getBeanKey() {
		return BootAnnCheckI.class.getName();
	}

	@Override
	public boolean checkClassExists(Class<?> configClass) {
		return CheckUtil.checkClassExists(configClass);
	}

	@Override
	public boolean checkClassExistsForMethod(Method m) {
		return CheckUtil.checkClassExistsForMethod(m);
	}

	@Override
	public boolean checkProp(Class<?> staticClass) {
		return CheckUtil.checkProp(staticClass);
	}

}
