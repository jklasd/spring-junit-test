package com.github.jklasd.test;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

public class InvokeUtil extends ReflectionUtils{
    public static Object invokeMethodByParamClass(Object obj,String methodStr,Class<?>[] argClasses ,Object[] arg) {
        Class<?> tagClass = obj.getClass();
        Method method = getMethodByParamClass(tagClass,methodStr, argClasses);
        if(!method.isAccessible()) {
            method.setAccessible(true);
        }
        return invokeMethod(method, obj, arg);
    }
	public static Object invokeMethod(Object obj,String methodStr,Object... arg) {
		Class<?> tagClass = obj.getClass();
		Method method = getMethod(tagClass,methodStr, arg);
		return invokeMethod(method, obj, arg);
	}
	private static Method getMethodByParamClass(Class<?> tagClass,String methodStr, Class<?>[] argClasses) {
        Method method = null;
        if(argClasses.length>0) {
            method = findMethod(tagClass, methodStr,argClasses);
        }else {
            method = findMethod(tagClass, methodStr);
        }
        return method;
    }
	private static Method getMethod(Class<?> tagClass,String methodStr, Object... arg) {
		Method method = null;
		if(arg.length>0) {
			Class<?>[] paramTypes = new Class<?>[arg.length];
			for(int i=0;i< arg.length;i++) {
				paramTypes[i] = arg[i].getClass();
			}
			method = findMethod(tagClass, methodStr,paramTypes);
		}else {
			method = findMethod(tagClass, methodStr);
		}
		return method;
	}
	public static Object invokeMethod(Object obj,String methodStr,String className,Object... arg) {
		Method method = getMethod(ScanUtil.loadClass(className), methodStr);
		return invokeMethod(method, obj, arg);
	}
	public static Object invokeMethod(Object obj,Class<?> tagClass,String methodStr,Object... arg) {
		Method method = getMethod(tagClass, methodStr);
		return invokeMethod(method, obj, arg);
	}
}
