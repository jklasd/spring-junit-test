package com.github.jklasd.velocity.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class ImportUtil {
	
	public static String handlerExceptionImport(Method method,String importStr,Map tmpMap) {
		if(method.getGenericExceptionTypes().length>0) {
			StringBuilder sbl = new StringBuilder();
			for(Type et:method.getGenericExceptionTypes()) {
				appendMap(tmpMap, et.getTypeName(), sbl);
			}
			return sbl.toString()+importStr;
		}
		return importStr;
	}
	/**
	 * export
	 */
	public static String handlerImport(Parameter p,String params,Map map) {
		String[] arr = CommonUtil.covertion(p);
		if(CommonUtil.checkParams(arr[0]) || arr[0].contains("Class<")) {
			return params;
		}
		
		StringBuilder imports = new StringBuilder(params);
		Type pType = p.getParameterizedType();
		if(pType!=null) {
			if(pType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) pType;
				if(pt.getActualTypeArguments()!=null
						&& pt.getActualTypeArguments().length>0) {
					for(Type t:pt.getActualTypeArguments()) {
						if(CommonUtil.checkParams(t.getTypeName())) {
							continue;
						}
						appendMap(map, t.getTypeName(), imports);
					}
				}
				if(map.containsKey("java.util.Map")) {
					appendMap(map, "java.util.HashMap", imports);
				}
				appendMap(map, "com.alibaba.fastjson.JSONObject", imports);
			}else {
				if(Modifier.isFinal(p.getType().getModifiers())) {
					if(!p.getType().isEnum()) {
						appendMap(map, "com.github.jklasd.test.lazybean.beanfactory.LazyBean", imports);
					}
				}else {
					appendMap(map, "com.alibaba.fastjson.JSONObject", imports);
				}
			}
		}
		appendMap(map, p.getType().getName(), imports);
		
		return imports.toString();
	}
	
	private static void appendMap(Map map, String typeStr, StringBuilder imports) {
		if(!map.containsKey(typeStr)) {
			imports.append("import ").append(typeStr).append(";");
			imports.append("\n");
			
			map.put(typeStr, typeStr);
		}
	}
}
