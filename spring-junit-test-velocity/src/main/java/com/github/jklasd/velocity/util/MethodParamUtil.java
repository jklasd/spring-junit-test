package com.github.jklasd.velocity.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
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
				
				if(checkParams(arr[0])) {
					return "";
				}
				StringBuilder createBean = new StringBuilder();
				
				
				Class<?> tagC = JunitClassLoader.loadClass(typeStr);
				if(tagC != null ) {
					String tagClassName = typeStr.substring(typeStr.lastIndexOf(".")+1);
					if(tagClassName.contains("$")) {
						tagClassName = tagClassName.replace("$", ".");//匿名内部类处理
					}
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
			if(tagClass == null) {
				log.debug("");
				return "";
			}
			StringBuilder jsonBuilder = new StringBuilder("{\"+\n");
			jsonBuilder.append(spaceStr);
			Field[] fields = tagClass.getDeclaredFields();
			for(Field f : fields) {
				jsonBuilder.append("\"");
				jsonBuilder.append("'").append(f.getName()).append("'").append(":")
					.append(handRefParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
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
					.append(handRefParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
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
						.append(handRefParams(new String[] {f.getType().getName(),f.getName()},f.getGenericType()));
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
	
	public static String handlerImport(Method method,Map map) {
		Parameter[] params = method.getParameters();
		String imports = "";
		for(Parameter p : params) {
			imports = handlerImport(p, imports, map);
		}
		return imports;
	}
	/**
	 * export
	 */
	private static String handlerImport(Parameter p,String params,Map map) {
		String[] arr = covertion(p);
		if(checkParams(arr[0]) || arr[0].contains("Class<")) {
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
						if(checkParams(t.getTypeName())) {
							continue;
						}
						appendMap(map, t.getTypeName(), imports);
					}
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
	public static String beforeHandFixedParamsByMethod(Method method) {
		Parameter[] params = method.getParameters();
		StringBuilder ref = new StringBuilder();
		for(Parameter p : params) {
			ref.append(BuildMethodObjParam.beforeHandFixedParams(covertion(p)));
		}
		return ref.toString();
	}
//	public static String beforeHandFixedParams(String paramInfo) {
//		String[] arr= split(paramInfo);
//		return BuildMethodObjParam.beforeHandFixedParams(arr);
//	}
	static String spaceStr = "JSONObject.parseObject".replaceAll(".", " ");
	public static String handRefParams(String[] arr, Type type) {
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
			return "["+handRefParams(new String[] {
					typeImpl.getActualTypeArguments()[0].getTypeName(),
					paramName
			}, type)+"]";
		default:
			Class<?> tagC = JunitClassLoader.loadClass(typeStr);
//			if(tagC == null) {
//				log.debug("断点");
//			}
			if(tagC == null || tagC.isEnum()) {
				return "''";
			}
			return "{}";
		}
	}
	public static String handParams(Method method) {
		Parameter[] params = method.getParameters();
		String paramStr = "";
		for(Parameter p : params) {
			paramStr = handParams(p, paramStr);
		}
		return paramStr;
	}
	public static String handParams(Parameter paramInfo,String params) {
		
		String[] arr= covertion(paramInfo);
		
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

	private static String[] covertion(Parameter paramInfo) {
		return new String[] {paramInfo.getParameterizedType().getTypeName(),paramInfo.getName()};
	}

//	private static String[] split(String paramInfo) {
//		return paramInfo.replace(", ", ",").split(" ");
//	}

	public static String convertionMethodName(String methodName,Object tmpObj) {
		Map tmp = (Map) tmpObj;
		if(tmp.containsKey(methodName)) {
			String tmpName = methodName;
			for(int i=1;i<100;i++) {
				tmpName = methodName+"_t_"+i;
				if(tmp.containsKey(tmpName)) {
					continue;
				}
				tmp.put(tmpName, tmpName);
				break;
			}
			return tmpName;
		}else {
			tmp.put(methodName, methodName);
			return methodName;
		}
	}
	
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
	
	public static String handResult(Method method) {
		if(method.getReturnType() == Void.class ||method.getReturnType() == void.class ) {
			return "";
		}
		String type = "";
		return type+" methodResult = ";
	}
}
