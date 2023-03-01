package com.github.jklasd.test.common.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassUtil {
	private static ClassUtil util = new ClassUtil();
	private ClassUtil() {}
	
	public static ClassUtil getInstance() {
		return util;
	}
	
	/**
	 * @param tagClass 需要检查的类
	 * @return 存在静态方法
	 */
	public boolean hasStaticMethod(Class<?> tagClass) {
		try {
			Method[] methods = tagClass.getDeclaredMethods();
			return Lists.newArrayList(methods).stream().anyMatch(m->{
				if(Modifier.isPublic(m.getModifiers())
						&& Modifier.isStatic(m.getModifiers())
						&& !m.getName().equals("main")//非main方法
						&& !m.getName().contains("lambda$")//非匿名方法
						&& !m.getName().contains("access$")//非匿名方法
						&& !m.getName().startsWith("$")) {//非代理类的方法
					Class<?> returnType = m.getReturnType();
					if(!returnType.getName().contains("void")) {
						log.debug("class=>{},method=>{}",tagClass,m);
						return true;
					}
				}
				return false;
			});
		}catch(Exception e) {
			log.error("hasStaticMethod",e);
		}
		return false;
	}
	
	
	private static final Map<String, Class<?>> namePrimitiveMap = new HashMap<>();
    static {
         namePrimitiveMap.put("boolean", Boolean.TYPE);
         namePrimitiveMap.put("byte", Byte.TYPE);
         namePrimitiveMap.put("char", Character.TYPE);
         namePrimitiveMap.put("short", Short.TYPE);
         namePrimitiveMap.put("int", Integer.TYPE);
         namePrimitiveMap.put("long", Long.TYPE);
         namePrimitiveMap.put("double", Double.TYPE);
         namePrimitiveMap.put("float", Float.TYPE);
         namePrimitiveMap.put("void", Void.TYPE);
    }
    
    public boolean isBasicType(Class<?> tagClass) {
    	if(tagClass == null) {
    		return false;
    	}
    	return namePrimitiveMap.containsKey(tagClass.getName());
    }

    /**
     * Maps primitive {@code Class}es to their corresponding wrapper {@code Class}.
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();
    static {
         primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
         primitiveWrapperMap.put(Byte.TYPE, Byte.class);
         primitiveWrapperMap.put(Character.TYPE, Character.class);
         primitiveWrapperMap.put(Short.TYPE, Short.class);
         primitiveWrapperMap.put(Integer.TYPE, Integer.class);
         primitiveWrapperMap.put(Long.TYPE, Long.class);
         primitiveWrapperMap.put(Double.TYPE, Double.class);
         primitiveWrapperMap.put(Float.TYPE, Float.class);
         primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
    }
}
