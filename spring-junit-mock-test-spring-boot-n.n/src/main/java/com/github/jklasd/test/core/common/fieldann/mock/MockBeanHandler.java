package com.github.jklasd.test.core.common.fieldann.mock;

import static org.mockito.Mockito.withSettings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.common.fieldann.AbstractMockHandler;
import com.github.jklasd.test.core.common.fieldann.MockFieldHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockBeanHandler extends AbstractMockHandler implements FieldHandler{

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
	public void handler(FieldDef def,Annotation ann) {
		Field attr = def.getField();
		Object obj = def.getTagObj();
		MockBean mockAnn = (MockBean) ann;
		try {
			String beanName = mockAnn.name();
			if(StringUtils.isBlank(beanName)) {
				beanName = attr.getName();
			}
//			//mockBean需要注册
			
			Object value = Mockito.mock(attr.getType(), withSettings().name(beanName));
			
			FieldAnnComponent.setObj(attr, obj, value);
			MockFieldHandler.getInstance().registBean(beanName, value, attr.getType(),obj.getClass());
		} catch (IllegalArgumentException e) {
			log.error("MockBeanHandler#handler",e);
		}
	}
}
