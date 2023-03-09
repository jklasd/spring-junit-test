package com.github.jklasd.velocity.util;

import java.lang.reflect.Parameter;

public class CommonUtil {
	public final static String spaceStr = "JSONObject.parseObject".replaceAll(".", " ");
	
	public static String[] covertion(Parameter paramInfo) {
		return new String[] {paramInfo.getParameterizedType().getTypeName(),paramInfo.getName()};
	}
	/**
	 * 判断类型，是否是引用对象
	 * @param arr
	 * @return
	 */
	public static boolean checkParams(String typeStr) {
		//arr[0]
		
		switch (typeStr) {
		case "int":
		case "java.lang.Integer":
		case "byte":
		case "java.lang.Byte":
		case "short":
		case "java.lang.Short":
		case "java.lang.String":
		case "char":
		case "boolean":
		case "java.lang.Boolean":
		case "long":
		case "java.lang.Long":
		case "double":
		case "java.lang.Double":
		case "float":
		case "java.lang.Float":
			return true;
		default:
			break;
		}
		return false;
	}
}
