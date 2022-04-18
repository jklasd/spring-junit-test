package com.github.jklasd.test.common.util;

import org.slf4j.MDC;

public class SignalNotificationUtil {
	public static void put(String key,String value) {
		MDC.put(key, value);
	}
	
	public static void remove(String key) {
		MDC.remove(key);
	}

	public static String get(String key) {
		return MDC.get(key);
	}
}
