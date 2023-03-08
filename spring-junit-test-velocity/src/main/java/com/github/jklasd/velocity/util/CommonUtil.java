package com.github.jklasd.velocity.util;

import java.lang.reflect.Parameter;

public class CommonUtil {
	public static String[] covertion(Parameter paramInfo) {
		return new String[] {paramInfo.getParameterizedType().getTypeName(),paramInfo.getName()};
	}
}
