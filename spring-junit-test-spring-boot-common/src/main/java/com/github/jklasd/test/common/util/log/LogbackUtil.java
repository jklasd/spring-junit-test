package com.github.jklasd.test.common.util.log;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import com.github.jklasd.test.common.util.ScanUtil;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogbackUtil {
	static String level = "INFO";
	static boolean useLocal = true;
	
	private final static String logbackClass = "ch.qos.logback.classic.LoggerContext";
	
	
	public static void setJunitLevel(String level) {
	    LogbackUtil.level = level;
	    useLocal = false;
	}
	
	public static void resetLog() {
		Class<?> logbackC = ScanUtil.loadClass(logbackClass);
		if(logbackC != null) {
			LogbackHander.resetLog();
		}
	}
//    private static volatile boolean test = true;
	public static void setTraceId() {
		if(StringUtils.isBlank(MDC.get("traceId"))) {
			MDC.put("traceId", UUID.randomUUID().toString());
		}
//		if(test) {
//			test = false;
//			log.info("test");
//		}
	}
	
	public static void clearTraceId() {
		MDC.remove("traceId");
	}

}
