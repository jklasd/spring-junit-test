package com.github.jklasd.test.lazyplugn.dubbo.reference;

import java.lang.annotation.Annotation;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.interf.handler.MockFieldHandlerI;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboAnnotationRefHandler;

public abstract class AbstractReferenceHandler implements FieldHandler{
	
	private MockFieldHandlerI handler = ContainerManager.getComponent(ContainerManager.NameConstants.MockFieldHandler);
	
	@Override
	public void handler(FieldDef def, Annotation ann) {
		/**
		 * 延迟处理
		 */
		BeanModel beanModel = new BeanModel();
		beanModel.setTagClass(def.getField().getType());
		beanModel.setFieldName(def.getField().getName());
		Object ref = LazyBean.getInstance().buildProxy(beanModel);
		if(ref!=null) {
			FieldAnnComponent.setObj(def.getField(), def.getTagObj(),ref);
		}
		LazyDubboAnnotationRefHandler.getInstance().registerBeanDef(def.getField(),ann);
	}
	

	public void injeckMock(FieldDef fieldDef, Annotation ann) {
		handler.injeckMock(fieldDef);
	}
}
