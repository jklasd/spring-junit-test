package com.github.jklasd.test.core.common.fieldann.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ResolvableType;

import com.github.jklasd.test.core.common.FieldAnnComponent;
import com.github.jklasd.test.core.common.FieldAnnComponent.FieldHandler;
import com.github.jklasd.test.core.common.fieldann.FieldDef;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Sets;

public class MockBeanHandler extends MockHandler implements FieldHandler{

	String packagePath = "org.springframework.boot.test.mock.mockito";
	private Class<?> MockDefinition = ScanUtil.loadClass(packagePath+".MockDefinition");
	Method createMock;
	Constructor<?> mockDefStructor;
	{
		try {
			Constructor<?>[] structors = MockDefinition.getDeclaredConstructors();
			mockDefStructor = structors[0];
			mockDefStructor.setAccessible(true);
			
			createMock = MockDefinition.getDeclaredMethod("createMock");
			createMock.setAccessible(true);
			
		} catch (SecurityException | IllegalArgumentException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String getType() {
		return MockBean.class.getName();
	}

	@Override
	public void handler(FieldDef def) {
		Field attr = def.getField();
		Object obj = def.getTagObj();
		MockBean mockAnn = attr.getAnnotation(MockBean.class);
//		Qualifier qualifier = attr.getAnnotation(Qualifier.class);
		try {
			Object qualDef = qualDefStructor.newInstance(attr,Sets.newHashSet(mockAnn));
			
			Object spyDefObj = mockDefStructor.newInstance(mockAnn.name(),
					ResolvableType.forClass(attr.getType()),
					mockAnn.extraInterfaces(),mockAnn.answer(),
					mockAnn.serializable(), mockAnn.reset(),
					qualDef);
			Object value = createMock.invoke(spyDefObj);
			FieldAnnComponent.setObj(attr, obj, value);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

}
