package com.github.jklasd.velocity.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import com.github.jklasd.test.common.exception.JunitException;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodParamUtil {
	
	/**
	 * 构建执行方法参数
	 * @param arr
	 * @return
	 */
	static class BuildMethodObjParam {
		public static String beforeHandFixedParams(String[] arr) {
			try {
				//arr[0]
				String typeStr = arr[0];
				//arr[1]
				String paramName = arr[1];
				
				if(checkParams(arr)) {
					return "";
				}
				StringBuilder createBean = new StringBuilder();
				
				
				Class<?> tagC = JunitClassLoader.loadClass(typeStr);
				if(tagC != null ) {
					String tagClassName = typeStr.substring(typeStr.lastIndexOf(".")+1);
					createBean.append(tagClassName);
					createBean.append(" ");
					createBean.append(paramName);
					createBean.append(" = ");
					if(tagC.isEnum()) {
						createBean.append(" null;");
					}else {
						String json = BuildJSONObjectByClass.parseJsonFromClass(typeStr);
						
						createBean.append("JSONObject.parseObject(\""+json+"\", \n"+spaceStr+tagClassName+".class);");
					}
				}else {
					if(typeStr.startsWith("java.util")) {
						String[] types = parseGenericType(typeStr);
						if(types.length==2) {
							String tagClassName = cutClassName(typeStr, types);
							createBean.append(tagClassName);
							createBean.append(" ");
							createBean.append(paramName);
							createBean.append(" = ");
							String json = BuildJSONObjectByClass.parseJsonFromClass(types[1]);
							createBean.append("JSONObject.parseArray(\"["+json+"]\", \n"+spaceStr+types[1].substring(types[1].lastIndexOf(".")+1)+".class);");
						}else if(types.length==3){
							String tagClassName = cutClassName(typeStr, types);
							createBean.append(tagClassName);
							createBean.append(" ");
							createBean.append(paramName);
							createBean.append(" = ");
							if(typeStr.contains("Map")) {
								createBean.append("new HashMap<>();");
							}
						}
					}else {
						String[] types = parseGenericType(typeStr);
						if(types.length==2) {
							String tagClassName = cutClassName(types[0], types);
							createBean.append(tagClassName);
							String json_inner = BuildJSONObjectByClass.parseJsonFromClass(types[1]);
//							String tagClassName1 = cutClassName(types[1], types);
							createBean.append(" ");
							createBean.append(paramName);
							createBean.append(" = ");
							
							String json = BuildJSONObjectByClass.parseJsonFromClass(types[0],types[1],paramName);
							json = json.replace("#{"+paramName+"}", json_inner);
							createBean.append("JSONObject.parseObject(\""+json_inner+"\", \n"+spaceStr+tagClassName+".class);");
						}else {
							throw new JunitException("异常数据，需要人工优化");
						}
					}
				}
				
				
				return createBean.toString();
			} catch (Exception e) {
				log.error("beforeHandFixedParams",e);
			}
			return "";
		}

		private static String cutClassName(String typeStr, String[] types) {
			String tagClassName = typeStr;
			for(String t:types) {
				tagClassName = tagClassName.replace(t.substring(0, t.lastIndexOf(".")+1), "");
			}
			return tagClassName;
		}
	}
	
	
	/**
	 * 通过类构建json
	 * @param tagClassName
	 * @return
	 */
	static class BuildJSONObjectByClass{
		public static String parseJsonFromClass(String tagClassName) {
			Class<?> tagClass = JunitClassLoader.loadClass(tagClassName);
			StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
			jsonBuilder.append(spaceStr);
			Field[] fields = tagClass.getDeclaredFields();
			for(Field f : fields) {
				jsonBuilder.append("\"");
				jsonBuilder.append("'").append(f.getName()).append("'").append(":")
					.append(handParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
				jsonBuilder.append(",\"+\n");
				jsonBuilder.append(spaceStr);
			}
			
			jsonBuilder.append("\"}");
			
			return jsonBuilder.toString();
		}

		public static String parseJsonFromClassForInner(String tagClassName) {
			Class<?> tagClass = JunitClassLoader.loadClass(tagClassName);
			StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
			jsonBuilder.append(spaceStr);
			Field[] fields = tagClass.getDeclaredFields();
			for(Field f : fields) {
				jsonBuilder.append("\"");
				jsonBuilder.append("'").append(f.getName()).append("'").append(":")
					.append(handParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
				jsonBuilder.append(",\"+\n");
				jsonBuilder.append(spaceStr);
			}
			
			jsonBuilder.append("\"}");
			
			return jsonBuilder.toString();
		}
		public static String parseJsonFromClass(String tagClassName, String refType, String refName) {
			Class<?> tagClass = JunitClassLoader.loadClass(tagClassName);
			Type[] tagCt = tagClass.getTypeParameters();
			StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
			jsonBuilder.append(spaceStr);
			Field[] fields = tagClass.getDeclaredFields();
			for(Field f : fields) {
				jsonBuilder.append("\"");
				Type ftg = f.getGenericType();
				if(Objects.equal(tagCt[0], ftg)) {
					jsonBuilder.append("'").append(f.getName()).append("'").append(":")
						.append("#{"+refName+"}");
				}else {
					jsonBuilder.append("'").append(f.getName()).append("'").append(":")
						.append(handParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
				}
				jsonBuilder.append(",\"+\n");
				jsonBuilder.append(spaceStr);
			}
			
			jsonBuilder.append("\"}");
			
			return jsonBuilder.toString();
		}
	}
	
	/**
	 * 判断类型，是否是引用对象
	 * @param arr
	 * @return
	 */
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
			break;
		}
		return false;
	}
	
	/**
	 * export
	 */
	public static String handlerImport(String paramInfo,String params) {
		String[] arr = split(paramInfo);
		//arr[0]
		String typeStr = arr[0];
		//arr[1]
//		String paramName = arr[1];
		
		if(checkParams(arr)) {
			return params;
		}
		StringBuilder imports = new StringBuilder(params);
		Class<?> tagC = JunitClassLoader.loadClass(typeStr);
		if(tagC != null ) {
			imports.append("import ").append(typeStr).append(";");
			imports.append("\n");
		}else {
			if(typeStr.startsWith("java.util")) {
				String[] types = parseGenericType(typeStr);
				Set<String> exits = Sets.newHashSet(types);
				for(String t : exits) {
					imports.append("import ").append(t).append(";");
					imports.append("\n");
				}
				if(typeStr.contains("Map")) {
					imports.append("import java.util.HashMap;");
					imports.append("\n");
				}
			}else {
				String[] types = parseGenericType(typeStr);
				Set<String> exits = Sets.newHashSet(types);
				for(String t : exits) {
					imports.append("import ").append(t).append(";");
					imports.append("\n");
				}
			}
		}
		
		
		return imports.toString();
	}
	private static String[] parseGenericType(String typeStr) {
		if(typeStr.contains("Map")) {
			return typeStr.replace(">", "").replace("<", ",").split(",");
		}else {
			return typeStr.replace(">", "").split("<");
		}
	}
	/**
	 * 构建执行方法参数
	 * export
	 */
	public static String beforeHandFixedParams(String paramInfo) {
		String[] arr= split(paramInfo);
		return BuildMethodObjParam.beforeHandFixedParams(arr);
	}
	static String spaceStr = "JSONObject.parseObject".replaceAll(".", " ");
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
			return "["+handParams(new String[] {
					typeImpl.getActualTypeArguments()[0].getTypeName(),
					paramName
			}, type)+"]";
		default:
			Class<?> tagC = JunitClassLoader.loadClass(typeStr);
			if(tagC.isEnum()) {
				return "''";
			}
			return "{}";
		}
	}
	public static String handParams(String paramInfo,String params) {
		
		String[] arr= split(paramInfo);
		
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

	private static String[] split(String paramInfo) {
		return paramInfo.replace(", ", ",").split(" ");
	}
	
}
