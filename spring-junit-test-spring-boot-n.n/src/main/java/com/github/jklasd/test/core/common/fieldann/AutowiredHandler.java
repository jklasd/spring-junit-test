package com.github.jklasd.test.core.common.fieldann;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.interf.handler.MockFieldHandlerI;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutowiredHandler implements FieldHandler{
	public void handler(FieldDef def,Annotation ann){
		Field attr = def.getField();
		Object tagObj = def.getTagObj();
		
		String bName = attr.getAnnotation(Qualifier.class)!=null?attr.getAnnotation(Qualifier.class).value():null;
		
		if(attr.getType() == List.class) {
			ParameterizedType t = (ParameterizedType) attr.getGenericType();
			Type[] item = t.getActualTypeArguments();
			if(item.length == 1) {
				//处理一个集合注入
				try {
					Class<?> c = JunitClassLoader.getInstance().junitloadClass(item[0].getTypeName());
					List list = LazyBean.findListBean(c);
					if(list.isEmpty()) {
						String[] beanNames = LazyApplicationContext.getInstance().getBeanNamesForType(c);
						for(String beanName : beanNames) {
							list.add(LazyApplicationContext.getInstance().getBean(beanName));
						}
					}
					FieldAnnComponent.setObj(attr, tagObj, list);
					log.info("{}注入集合=>{},{}个对象",tagObj.getClass(),attr.getName(),list.size());
				} catch (ClassNotFoundException e) {
					log.error("ClassNotFoundException",e);
					throw new JunitException("ClassNotFoundException", true);
				}
			}else {
				log.info("其他特殊情况");
			}
		}else {
			if(StringUtils.isBlank(bName)) {
				bName = attr.getName();
			}
			FieldAnnComponent.setObj(attr, tagObj,LazyBean.getInstance().buildProxy(attr.getType(),bName));
		}
	}

	@Override
	public String getType() {
		return Autowired.class.getName();
	}
	
	private MockFieldHandlerI handler = ContainerManager.getComponent(ContainerManager.NameConstants.MockFieldHandler);

	@Override
	public void injeckMock(FieldDef fieldDef, Annotation ann) {
		handler.injeckMock(fieldDef);
	}
}
