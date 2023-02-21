package com.github.jklasd.velocity.util;

public class JunitClassLoader {

	public static Class<?> loadClass(String className){
		try {
			return JunitClassLoader.class.getClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
