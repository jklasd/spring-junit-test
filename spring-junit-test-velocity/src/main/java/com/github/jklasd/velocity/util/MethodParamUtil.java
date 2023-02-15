package com.github.jklasd.velocity.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.github.jklasd.test.common.JunitClassLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodParamUtil {
	public static boolean checkParams(String[] arr) {
		//arr[0]
		String typeStr = arr[0];
		
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
			log.info("特殊类型:{}",typeStr);
			break;
		}
		
		return false;
	}
	public static String handlerImport(String[] arr,String params) {
		//arr[0]
		String typeStr = arr[0];
		//arr[1]
//		String paramName = arr[1];
		
		if(checkParams(arr)) {
			return params;
		}
		StringBuilder imports = new StringBuilder(params);
		imports.append("import ");
		imports.append(typeStr);
		imports.append(";");
		imports.append("\n");
		
		
		return imports.toString();
	}
	public static String beforeHandFixedParams(String[] arr) {
		//arr[0]
		String typeStr = arr[0];
		//arr[1]
		String paramName = arr[1];
		
		if(checkParams(arr)) {
			return "";
		}
		StringBuilder createBean = new StringBuilder();
		String tagClassName = typeStr.substring(typeStr.lastIndexOf(".")+1);
		createBean.append(tagClassName);
		createBean.append(" ");
		createBean.append(paramName);
		createBean.append(" = ");
		
		String json = parse(typeStr);
		
		createBean.append("JSONObject.parseObject("+json+", \n"+spaceStr+tagClassName+".class);");
		
		return createBean.toString();
	}
	static String spaceStr = "JSONObject.parseObject".replaceAll(".", " ");
	private static String parse(String tagClassName) {
		try {
			Class<?> tagClass = JunitClassLoader.getInstance().loadClass(tagClassName);
			StringBuilder jsonBuilder = new StringBuilder("\"{\"+\n");
			jsonBuilder.append(spaceStr);
			Field[] fields = tagClass.getDeclaredFields();
			for(Field f : fields) {
				jsonBuilder.append("\"");
				jsonBuilder.append("'").append(f.getName()).append("'").append(":")
					.append(handParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
				jsonBuilder.append(",\"+\n");
				jsonBuilder.append(spaceStr);
			}
			
			jsonBuilder.append("\"}\"");
			
			return jsonBuilder.toString();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return "";
		}
	}
	public static String handParams(String[] arr, Type type) {
		//arr[0]
		String typeStr = arr[0];
		//arr[1]
		String paramName = arr[1];
		
		String tagClassName = typeStr.substring(typeStr.lastIndexOf(".")+1);
		
//		String name = type.getClass().getName();
		
		switch (typeStr) {
		case "int":
		case "java.lang.Integer":
		case "byte":
		case "java.lang.Byte":
		case "short":
		case "java.lang.Short":
		case "long":
		case "java.lang.Long":
			return "0";
		case "java.lang.String":
			return "'"+paramName+"'";
		case "char":
			return "'j'";
		case "boolean":
		case "java.lang.Boolean":
			return "false";
		case "float":
		case "java.lang.Float":
		case "double":
		case "java.lang.Double":
			return "0.0";
		case "java.util.List":
			ParameterizedType typeImpl = (ParameterizedType) type;
			return "["+handParams(new String[] {
					typeImpl.getActualTypeArguments()[0].getTypeName(),
					paramName
			}, type)+"]";
		default:
			return "JSONObject.parseObject('', "+tagClassName+".class)";
		}
	}
	public static String handParams(String[] arr,String params) {
		//arr[0]
		String typeStr = arr[0];
		//arr[1]
		String paramName = arr[1];
		params = params.length()>0?params+", ":params;
		switch (typeStr) {
		case "int":
		case "java.lang.Integer":
		case "byte":
		case "java.lang.Byte":
		case "short":
		case "java.lang.Short":
			return params+"0";
		case "java.lang.String":
			return params+paramName;
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
	
}
