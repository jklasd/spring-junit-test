package com.github.jklasd.test.core.common.fieldann.mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.mockito.Mock;
import org.mockito.internal.configuration.MockAnnotationProcessor;

import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.core.common.fieldann.AbstractMockHandler;
import com.github.jklasd.test.core.common.fieldann.MockFieldHandler;

public class MockHandler extends AbstractMockHandler implements FieldHandler{

	@Override
	public String getType() {
		return Mock.class.getName();
	}
	
	private MockAnnotationProcessor mockAnnotationProcessor = new MockAnnotationProcessor();

	@Override
	public void handler(FieldDef def, Annotation ann) {
		Field attr = def.getField();
		Object tagObject = def.getTagObj();
		Mock mockAnn = (Mock) ann;
		Object obj = mockAnnotationProcessor.process(mockAnn, def.getField());
		try {
			FieldAnnComponent.setObj(attr, tagObject, obj);
			
			MockFieldHandler.getInstance().registBean(attr.getName(), obj, attr.getType(),tagObject.getClass());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

}
