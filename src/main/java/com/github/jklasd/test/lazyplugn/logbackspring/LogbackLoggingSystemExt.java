package com.github.jklasd.test.lazyplugn.logbackspring;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.core.env.Environment;

public class LogbackLoggingSystemExt extends LogbackLoggingSystem{

	public LogbackLoggingSystemExt(ClassLoader classLoader) {
		super(classLoader);
	}
	
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
//		Environment env = initializationContext.getEnvironment();
//		if(env.getProperty("spring.application.name") == null) {
//			return;
//		}
		super.loadConfiguration(initializationContext, location, logFile);
	}

}
