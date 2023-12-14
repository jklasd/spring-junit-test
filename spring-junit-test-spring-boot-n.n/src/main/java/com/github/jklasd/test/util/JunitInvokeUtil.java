package com.github.jklasd.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.util.ScanUtil;

public class JunitInvokeUtil extends ReflectionUtils{
	public static Object invokeMethodSignParam(Object obj,String methodStr,Class<?> argClasses ,Object arg) {
        Class<?> tagClass = obj.getClass();
        Method method = findMethod(tagClass, methodStr, argClasses);
        if(!method.isAccessible()) {
            method.setAccessible(true);
        }
        return invokeMethod(method, obj, arg);
    }
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
		if(method == null) {
			method = getMethodByMName(tagClass,methodStr);
		}
		if(!method.isAccessible()) {
			method.setAccessible(true);
		}
		return invokeMethod(method, obj, arg);
	}
	private static Method getMethodByMName(Class<?> tagClass, String methodStr) {
		Class<?> searchType = tagClass;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods() :
					getDeclaredMethods(searchType));
			for (Method method : methods) {
				if (methodStr.equals(method.getName())) {
					return method;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
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
	private static Method getMethodImpl(Class<?> tagClass,String methodStr, Object... arg) {
		Method method = null;
		if(arg.length>0) {
			Class<?>[] paramTypes = new Class<?>[arg.length];
			for(int i=0;i< arg.length;i++) {
				paramTypes[i] = arg[i].getClass();
			}
			method = findMethodExt(tagClass, methodStr,paramTypes);
		}else {
			method = findMethod(tagClass, methodStr);
		}
		return method;
	}
	public static Object invokeMethod(Object obj,String methodStr,String className,Object... arg) {
		Method method = getMethod(ScanUtil.loadClass(className), methodStr);
		if(!method.isAccessible()) {
			method.setAccessible(true);
		}
		return invokeMethod(method, obj, arg);
	}
	public static Object invokeMethod(Object obj,Class<?> tagClass,String methodStr,Object... arg) {
		Method method = getMethodImpl(tagClass, methodStr,arg);
		if(!method.isAccessible()) {
			method.setAccessible(true);
		}
		return invokeMethod(method, obj, arg);
	}
	
	public static Method findMethodExt(Class<?> clazz, String name, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(name, "Method name must not be null");
		Class<?> searchType = clazz;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods() :
				searchType.getDeclaredMethods());
			for (Method method : methods) {
				if (name.equals(method.getName()) && (paramTypes == null || hasParams(method, paramTypes))) {
					return method;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}
	
	private static boolean hasParams(Method method, Class<?>[] paramTypes) {
		if(paramTypes.length == method.getParameterCount()) {
			Class<?>[] mts = method.getParameterTypes();
			for(int i=0;i<mts.length;i++) {
				boolean pass = mts[i].isAssignableFrom(paramTypes[i]);
				if(!pass) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	public static Object invokeStaticMethod(Class<?> tagClass,String methodStr,Class<?>[] paramType,Object... arg) {
	    try {
	    	Method staticMethod = tagClass.getDeclaredMethod(methodStr, paramType);
	    	if(!staticMethod.isAccessible()) {
	    		staticMethod.setAccessible(true);
	    	}
	    	return staticMethod.invoke(null, arg);
        } catch (Throwable e) {
             throw new JunitException(e);
        }
    }
	public static Object invokeReadField(String fieldName,Object source) {
		Class<?> tagClass = source.getClass();
		return invokeReadField(fieldName, source, tagClass);
	}
	public static Object invokeReadField(String fieldName,Object source,Class<?> tagClass) {
		try {
			Field tmpField = tagClass.getDeclaredField(fieldName);
			if(!tmpField.isAccessible()) {
				tmpField.setAccessible(true);
			}
			return tmpField.get(source);
		} catch (Exception e) {
			throw new JunitException(e); 
		}
	}
	public static void invokeWriteField(String fieldName,Object source,Object value) {
		Class<?> tagClass = source.getClass();
		try {
			Field tmpField = tagClass.getDeclaredField(fieldName);
			if(!tmpField.isAccessible()) {
				tmpField.setAccessible(true);
			}
			tmpField.set(source,value);
		} catch (Exception e) {
			throw new JunitException(e); 
		}
	}
}
