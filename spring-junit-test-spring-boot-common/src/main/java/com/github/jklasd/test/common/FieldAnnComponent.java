package com.github.jklasd.test.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.model.FieldDef;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldAnnComponent {
	
	private static Map<String,FieldHandler> handlerMap = Maps.newHashMap();
	public static class HandlerLoader{
		public static void load(String... handlerClasses) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
			for(String hclass :handlerClasses) {
				Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
				FieldHandler handler = (FieldHandler) handlerClass.newInstance();
				if(StringUtils.isNotBlank(handler.getType())) {
					handlerMap.put(handler.getType(), handler);
				}
			}
		}
	}
	

	public static void handlerField(FieldDef attr) {
		Annotation[] anns = attr.getField().getAnnotations();
		for(Annotation ann : anns) {
			FieldHandler handler = handlerMap.get(ann.annotationType().getName());
			if(handler!=null) {
				handler.handler(attr,ann);
				break;
			}
		}
	}
	
	public static void setObj(Field attr,Object obj,Object proxyObj) {
		if(proxyObj == null) {//延迟注入,可能启动时，未加载到bean
			log.warn("=====注入数据为空==={}={}=",obj,attr.getName());
		}
		try {
			if (!attr.isAccessible()) {
				attr.setAccessible(true);
			}
			attr.set(obj, proxyObj);
		} catch (Exception e) {
			log.error("注入对象异常",e);
		}
	}
}
