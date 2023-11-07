package com.github.jklasd.velocity.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.objenesis.ObjenesisStd;

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
			
			if(CommonUtil.checkParams(arr[0])) {
				return "";
			}
			StringBuilder createBean = new StringBuilder();
			
			Type pType = p.getParameterizedType();
			if(pType!=null) {
				if(pType instanceof ParameterizedType) {
					//存在泛型类
					ParameterizedType pt = (ParameterizedType) pType;
//					if(pt.getActualTypeArguments()!=null
//							&& pt.getActualTypeArguments().length>0) {
//						for(Type t:pt.getActualTypeArguments()) {
//							if(MethodParamUtil.checkParams(t.getTypeName())) {
//								continue;
//							}
//							
//						}
//					}
					if(typeStr.startsWith("java.util")) {
						String[] types = parseGenericType(typeStr);
						if(types.length==2) {
							String tagClassName = cutClassName(typeStr, types);
							createBean.append(tagClassName);
							createBean.append(" ");
							createBean.append(paramName);
							createBean.append(" = ");
							String json = BuildJSONObjectByClass.parseJsonFromClass(types[1]);
							createBean.append("JSONObject.parseArray(\"["+json+"]\", \n"+CommonUtil.spaceStr+types[1].substring(types[1].lastIndexOf(".")+1)+".class);");
						}else if(types.length==3){
							String tagClassName = cutClassName(typeStr, types);
							createBean.append(tagClassName);
							createBean.append(" ");
							createBean.append(paramName);
							createBean.append(" = ");
							if(typeStr.contains("Map")) {
								createBean.append("new HashMap<>();\n");
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
							createBean.append("JSONObject.parseObject(\""+json_inner+"\", \n"+CommonUtil.spaceStr+tagClassName+".class);");
						}else {
							throw new JunitException("异常数据，需要人工优化");
						}
					}
				}else {
					//非泛型类
					Class<?> tagC = (Class<?>) pType;
					String tagClassName = tagC.getSimpleName();
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
						if(Modifier.isFinal(tagC.getModifiers())) {
							createBean.append("LazyBean.invokeBuildObject("+tagClassName+".class);");
						}else {
							String json = BuildJSONObjectByClass.parseJsonFromClass(tagC);
							
							createBean.append("JSONObject.parseObject(\""+json+"\", \n"+CommonUtil.spaceStr+tagClassName+".class);\n");
						}
					}
					return createBean.toString();
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
