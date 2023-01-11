package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.AbstractDbHandler;
import com.github.jklasd.test.common.interf.handler.MockClassHandler;

public class DockerDbHandler extends AbstractDbHandler implements MockClassHandler{

	@Override
	public String getType() {
		return JunitMysqlContainerSelected.class.getName();
	}

	@Override
	public void hand(Method testMethod) {
		JunitMysqlContainerSelected selected = testMethod.getAnnotation(JunitMysqlContainerSelected.class);
		useMethodMock.set(selected.value());
	}

	@Override
	public void releaseMethod(Method testMethod) {
		useMethodMock.remove();
	}

	@Override
	public void hand(Class<?> testClass) {
		JunitMysqlContainerSelected selected = testClass.getAnnotation(JunitMysqlContainerSelected.class);
		useClassMock.set(selected.value());
		
		RoutingDataSourceExt.getInstance().handInsertResource(selected.insertResource());
	}

	@Override
	public void releaseClass(Class<?> testClass) {
		useClassMock.remove();
	}

}
