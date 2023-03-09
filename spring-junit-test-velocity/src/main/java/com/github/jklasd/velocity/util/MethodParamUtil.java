package com.github.jklasd.velocity.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodParamUtil {
	
	//export
	public static String handlerImport(Method method,Map map) {
		Parameter[] params = method.getParameters();
		String imports = "";
		for(Parameter p : params) {
			imports = ImportUtil.handlerImport(p, imports, map);
		}
		return imports;
	}
	

	
	/**
	 * 构建执行方法参数
	 * export
	 */
	public static String beforeHandFixedParamsByMethod(Method method) {
		Parameter[] params = method.getParameters();
		StringBuilder ref = new StringBuilder();
		for(Parameter p : params) {
			ref.append(BuildMethodObjParam.beforeHandFixedParams(p));
		}
		return ref.toString();
	}
	//export
	public static String handParams(Method method) {
		Parameter[] params = method.getParameters();
		String paramStr = "";
		for(Parameter p : params) {
			paramStr = handParams(p, paramStr);
		}
		return paramStr;
	}
	private static String handParams(Parameter paramInfo,String params) {
		
		String[] arr= CommonUtil.covertion(paramInfo);
		
		//arr[0]
		String typeStr = arr[0];
		//arr[1]
		String paramName = arr[1];
		params = params.length()>0?params+", ":params;
		switch (typeStr) {
		case "int":
		case "java.lang.Integer":
		case "short":
		case "java.lang.Short":
			return params+"0";
		case "byte":
		case "java.lang.Byte":
			return params+"(byte)0";
		case "java.lang.String":
			return params+"\""+paramName+"\"";
		case "char":
			return params+"'j'";
		case "boolean":
		case "java.lang.Boolean":
			return params+"false";
		case "long":
		case "java.lang.Long":
			return params+"0l";
		case "double":
		case "java.lang.Double":
			return params+"0.0";
		case "float":
		case "java.lang.Float":
			return params+"0.f";
		default:
			log.info("特殊类型:{}",typeStr);
			return params+paramName;
		}
		
	}
	/*
	 * export
	 */
	public static String convertionMethodName(Method method) {
		return MethodNameUtil.convertionMethodName(method);
	}

	
	/*
	 * export
	 */
	public static String handlerExceptionImport(Method method,String importStr,Map tmpMap) {
		return ImportUtil.handlerExceptionImport(method, importStr, tmpMap);
	}
	/*
	 * export
	 */
	public static String throwException(Method method) {
		if(method.getGenericExceptionTypes().length>0) {
			StringBuilder sbl = new StringBuilder();
			for(Type et:method.getGenericExceptionTypes()) {
				sbl.append(((Class<?>)et).getSimpleName());
			}
			return "throws "+sbl.toString();
		}
		return "";
	}
	/*
	 * export
	 */
	public static String handResult(Method method) {
		if(method.getReturnType() == Void.class ||method.getReturnType() == void.class ) {
			return "";
		}
		String type = "";
		return type+" methodResult = ";
	}
}
