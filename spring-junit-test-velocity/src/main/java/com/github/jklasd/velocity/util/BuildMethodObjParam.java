package com.github.jklasd.velocity.util;

import java.lang.reflect.Parameter;

import com.github.jklasd.test.common.exception.JunitException;

import lombok.extern.slf4j.Slf4j;
/**
 * 构建执行方法参数
 * @param arr
 * @return
 */
@Slf4j
public class BuildMethodObjParam {
	public static String beforeHandFixedParams(Parameter p) {
		try {
			String[] arr = CommonUtil.covertion(p);
			//arr[0]
			String typeStr = arr[0];
			//arr[1]
			String paramName = arr[1];
			
			if(MethodParamUtil.checkParams(arr[0])) {
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
					
					createBean.append("JSONObject.parseObject(\""+json+"\", \n"+MethodParamUtil.spaceStr+tagClassName+".class);");
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
						createBean.append("JSONObject.parseArray(\"["+json+"]\", \n"+MethodParamUtil.spaceStr+types[1].substring(types[1].lastIndexOf(".")+1)+".class);");
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
//						String tagClassName1 = cutClassName(types[1], types);
						createBean.append(" ");
						createBean.append(paramName);
						createBean.append(" = ");
						
						String json = BuildJSONObjectByClass.parseJsonFromClass(types[0],types[1],paramName);
						json = json.replace("#{"+paramName+"}", json_inner);
						createBean.append("JSONObject.parseObject(\""+json_inner+"\", \n"+MethodParamUtil.spaceStr+tagClassName+".class);");
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
	
	private static String[] parseGenericType(String typeStr) {
		if(typeStr.contains("Map")) {
			return typeStr.replace(">", "").replace("<", ",").split(",");
		}else {
			return typeStr.replace(">", "").split("<");
		}
	}
}
