package com.github.jklasd.test.core.common.fieldann.mock;

import static org.mockito.Mockito.withSettings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.mockito.InjectMocks;
import org.mockito.Mockito;

import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.core.common.fieldann.AbstractMockHandler;
import com.github.jklasd.test.core.common.fieldann.MockFieldHandler;

/**
 * 需要后置处理
 * @author jubin.zhang
 *
 */
public class InjectMocksHandler extends AbstractMockHandler implements FieldHandler{

	@Override
	public String getType() {
		return InjectMocks.class.getName();
	}
	
	@Override
	public int order() {//比Autowrite 大1
		return 1;
	}

	
	
	@Override
	public void handler(FieldDef def, Annotation ann) {
		Field attr = def.getField();
		Object tagObject = def.getTagObj();
//		InjectMocks mockAnn = (InjectMocks) ann;
		
		try {
			//如果存在spring 注解
			Object obj = null;
			//则注入，在类结束后释放掉对象
			if(attr.get(tagObject)!=null) {
				obj = attr.get(tagObject);
			}else {
				//创建mockbean 注册到容器中，需要兼容spring 注入注解
				obj = Mockito.mock(attr.getType(), withSettings()
						.defaultAnswer(Mockito.CALLS_REAL_METHODS)
						.name(attr.getName()));
				//针对mockbean处理
				FieldAnnComponent.setObj(attr, tagObject, obj);
			}
			
			MockFieldHandler.getInstance().load(tagObject.getClass(),obj,attr.getType());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
	}
}
