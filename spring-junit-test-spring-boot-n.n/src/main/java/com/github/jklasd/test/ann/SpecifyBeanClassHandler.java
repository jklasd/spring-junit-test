package com.github.jklasd.test.ann;

import java.lang.reflect.Method;
import java.util.Set;

import com.github.jklasd.test.common.component.ServiceRegisterComponent;
import com.github.jklasd.test.common.interf.handler.MockClassHandler;

public class SpecifyBeanClassHandler implements MockClassHandler{

	private Set<Class<?>> existsClass;
	
	@Override
	public String getType() {
		return JunitSpecifyBean.class.getName();
	}

	@Override
	public void hand(Class<?> testClass) {
		JunitSpecifyBean beanClass = testClass.getAnnotation(JunitSpecifyBean.class);
		if(beanClass!=null) {
			handBeanClass(beanClass);
		}
	}

	private void handBeanClass(JunitSpecifyBean beanClass) {
		for(Class<?> tmpC : beanClass.value()) {
			if(existsClass.contains(tmpC)) {
				continue;
			}
			existsClass.add(tmpC);
			
			ServiceRegisterComponent.registerConfiguration(tmpC);
		}
	}

	@Override
	public void releaseClass(Class<?> testClass) {
		
	}

	@Override
	public void hand(Method testMethod) {
		JunitSpecifyBean beanClass = testMethod.getAnnotation(JunitSpecifyBean.class);
		if(beanClass!=null) {
			handBeanClass(beanClass);
		}
	}

	@Override
	public void releaseMethod(Method testMethod) {
		
	}

	@Override
	public Boolean isMock() {
		return false;
	}

}
