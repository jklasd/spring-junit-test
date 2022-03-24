package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.jklasd.test.core.common.FieldAnnComponent;
import com.github.jklasd.test.core.common.FieldAnnComponent.FieldHandler;
import com.github.jklasd.test.core.facade.JunitClassLoader;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutowiredHandler implements FieldHandler{
	public void handler(FieldDef def){
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
					List<?> list = LazyBean.findListBean(c);
					FieldAnnComponent.setObj(attr, tagObj, list);
					log.info("{}注入集合=>{},{}个对象",tagObj.getClass(),attr.getName(),list.size());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}else {
				log.info("其他特殊情况");
			}
		}else {
			FieldAnnComponent.setObj(attr, tagObj,LazyBean.getInstance().buildProxy(attr.getType(),bName));
		}
	}

	@Override
	public String getType() {
		return Autowired.class.getName();
	}
}
