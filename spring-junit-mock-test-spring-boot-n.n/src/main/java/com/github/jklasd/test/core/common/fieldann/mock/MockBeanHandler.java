package com.github.jklasd.test.core.common.fieldann.mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ResolvableType;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.FieldAnnComponent;
import com.github.jklasd.test.common.ScanUtil;
import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;
import com.github.jklasd.test.common.model.FieldDef;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockBeanHandler extends MockHandler implements FieldHandler{

	String packagePath = "org.springframework.boot.test.mock.mockito";
	private Class<?> MockDefinition = ScanUtil.loadClass(packagePath+".MockDefinition");
	Method createMock;
	Constructor<?> mockDefStructor;
	private JunitApplicationContext junitApplicationContext;
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
		junitApplicationContext = ContainerManager.getComponent(JunitApplicationContext.class.getSimpleName());
	}
	
	@Override
	public String getType() {
		return MockBean.class.getName();
	}

	@Override
	public void handler(FieldDef def,Annotation ann) {
		Field attr = def.getField();
		Object obj = def.getTagObj();
		MockBean mockAnn = (MockBean) ann;
//		Qualifier qualifier = attr.getAnnotation(Qualifier.class);
		try {
			Object qualDef = qualDefStructor.newInstance(attr,Sets.newHashSet(mockAnn));
			
			Object spyDefObj = mockDefStructor.newInstance(mockAnn.name(),
					ResolvableType.forClass(attr.getType()),
					mockAnn.extraInterfaces(),mockAnn.answer(),
					mockAnn.serializable(), mockAnn.reset(),
					qualDef);
			Object value = createMock.invoke(spyDefObj);
			
			String beanName = mockAnn.name();
			if(StringUtils.isBlank(beanName)) {
				beanName = attr.getName();
			}
			//mockBean需要注册
			junitApplicationContext.registBean(beanName, value, attr.getType());
			
			FieldAnnComponent.setObj(attr, obj, value);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("MockBeanHandler#handler",e);
		}
	}
}
