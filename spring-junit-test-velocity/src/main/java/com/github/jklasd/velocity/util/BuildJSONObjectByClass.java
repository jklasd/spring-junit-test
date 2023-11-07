package com.github.jklasd.velocity.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.common.base.Objects;

import lombok.extern.slf4j.Slf4j;
/**
 * 通过类构建json
 * @param tagClassName
 * @return
 */
@Slf4j
public class BuildJSONObjectByClass {
	
	public static String parseJsonFromClass(Class<?> tagClass) {
		if(tagClass == null) {
			return "";
		}
		StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
		jsonBuilder.append(CommonUtil.spaceStr);
		Field[] fields = tagClass.getDeclaredFields();
		for(Field f : fields) {
			jsonBuilder.append("\"");
			jsonBuilder.append("'").append(f.getName()).append("'").append(":")
				.append(handRefParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
			jsonBuilder.append(",\"+\n");
			jsonBuilder.append(CommonUtil.spaceStr);
		}
		
		jsonBuilder.append("\"}");
		
		return jsonBuilder.toString();
	}
	
	public static String parseJsonFromClass(String tagClassName) {
		Class<?> tagClass = JunitClassLoader.loadClass(tagClassName);
		if(tagClass == null) {
			return "";
		}
		StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
		jsonBuilder.append(CommonUtil.spaceStr);
		Field[] fields = tagClass.getDeclaredFields();
		for(Field f : fields) {
			jsonBuilder.append("\"");
			jsonBuilder.append("'").append(f.getName()).append("'").append(":")
				.append(handRefParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
			jsonBuilder.append(",\"+\n");
			jsonBuilder.append(CommonUtil.spaceStr);
		}
		
		jsonBuilder.append("\"}");
		
		return jsonBuilder.toString();
	}

	public static String parseJsonFromClassForInner(String tagClassName) {
		Class<?> tagClass = JunitClassLoader.loadClass(tagClassName);
		StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
		jsonBuilder.append(CommonUtil.spaceStr);
		Field[] fields = tagClass.getDeclaredFields();
		for(Field f : fields) {
			jsonBuilder.append("\"");
			jsonBuilder.append("'").append(f.getName()).append("'").append(":")
				.append(handRefParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
			jsonBuilder.append(",\"+\n");
			jsonBuilder.append(CommonUtil.spaceStr);
		}
		
		jsonBuilder.append("\"}");
		
		return jsonBuilder.toString();
	}
	public static String parseJsonFromClass(String tagClassName, String refType, String refName) {
		Class<?> tagClass = JunitClassLoader.loadClass(tagClassName);
		Type[] tagCt = tagClass.getTypeParameters();
		StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
		jsonBuilder.append(CommonUtil.spaceStr);
		Field[] fields = tagClass.getDeclaredFields();
		for(Field f : fields) {
			jsonBuilder.append("\"");
			Type ftg = f.getGenericType();
			if(Objects.equal(tagCt[0], ftg)) {
				jsonBuilder.append("'").append(f.getName()).append("'").append(":")
					.append("#{"+refName+"}");
			}else {
				jsonBuilder.append("'").append(f.getName()).append("'").append(":")
					.append(handRefParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
			}
			jsonBuilder.append(",\"+\n");
			jsonBuilder.append(CommonUtil.spaceStr);
		}
		
		jsonBuilder.append("\"}");
		
		return jsonBuilder.toString();
	}
	
	public static String handRefParams(String[] arr, Type type) {
		//arr[0]
		String typeStr = arr[0];
		//arr[1]
		String paramName = arr[1];
		
//		String tagClassName = typeStr.substring(typeStr.lastIndexOf(".")+1);
		
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
		case "java.math.BigDecimal":
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
		case "java.util.Date":
			return "'"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"'";
		case "java.util.List":
			ParameterizedType typeImpl = (ParameterizedType) type;
			return "["+handRefParams(new String[] {
					typeImpl.getActualTypeArguments()[0].getTypeName(),
					paramName
			}, type)+"]";
		default:
			Class<?> tagC = JunitClassLoader.loadClass(typeStr);
			if(tagC == null || tagC.isEnum()) {
				return "''";
			}
			return "{}";
		}
	}
}
