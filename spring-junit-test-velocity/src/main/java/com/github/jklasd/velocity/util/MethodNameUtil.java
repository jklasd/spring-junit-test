package com.github.jklasd.velocity.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.github.jklasd.test.common.ann.DefaultTestMethod;

public class MethodNameUtil {
	public static boolean validateMethod(Method method, Map<String, String> methodNames) {
		Class<?> tagClass = JunitClassLoader.loadClass(method.getDeclaringClass().getName()+"_Test");
		if(tagClass != null) {
			try {
				Method m = tagClass.getDeclaredMethod(methodNames.get(convertionMethodName(method))+"_test");
				DefaultTestMethod defaultAnn = m.getAnnotation(DefaultTestMethod.class);
				if(defaultAnn == null) {
					return true;//保留原有代码
				}
			} catch (NoSuchMethodException | SecurityException e) {
			}
		}
		return false;//通过，生成默认测试用例
	}
	
	public static String convertionMethodName(Method method) {
		Class<?>[] pTs = method.getParameterTypes();
		String name = method.getName();
		for(int i=0;i<pTs.length;i++) {
			name+= "_"+pTs[i].getName().length();
		}
		return name;
	}
	
	/*
	 * export
	 */
	public static String convertionMethodName(Method method,Object tmpObj) {
		Set tmp = (Set) tmpObj;
		String methodName = method.getName();
		if(tmp.contains(methodName)) {
			String tmpName = methodName;
			for(int i=1;i<100;i++) {
				tmpName = methodName+"_t_"+i;
				if(tmp.contains(tmpName)) {
					continue;
				}
				tmp.add(tmpName);
				break;
			}
			return tmpName;
		}else {
			tmp.add(methodName);
			return methodName;
		}
	}
}
