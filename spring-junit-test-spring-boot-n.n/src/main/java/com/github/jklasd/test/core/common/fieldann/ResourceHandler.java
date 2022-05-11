package com.github.jklasd.test.core.common.fieldann;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.Resource;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceHandler  implements FieldHandler{
	public void handler(FieldDef def,Annotation ann){
		Field attr = def.getField();
		Object obj = def.getTagObj();
		Resource c = (Resource) ann;
		if (c != null) {
			if(attr.getType() == List.class) {
				ParameterizedType t = (ParameterizedType) attr.getGenericType();
				Type[] item = t.getActualTypeArguments();
				if(item.length == 1) {
					//处理一个集合注入
					try {
						Class<?> itemC = JunitClassLoader.getInstance().junitloadClass(item[0].getTypeName());
						List<?> list = LazyBean.findListBean(itemC);
						FieldAnnComponent.setObj(attr, obj, list);
						log.info("{}注入resource集合=>{},{}个对象",obj.getClass(),attr.getName(),list.size());
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}else {
					log.info("其他特殊情况");
				}
			}else {
				FieldAnnComponent.setObj(attr, obj, LazyBean.getInstance().buildProxy(attr.getType(),c.name()));
			}
		}
	}
	public String getType() {
		return Resource.class.getName();
	}
}
