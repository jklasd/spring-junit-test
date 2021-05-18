package com.github.jklasd.util;

import java.nio.charset.Charset;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogbackUtil {
	private static Level level = Level.INFO;
	public static void setJunitLevel(Level level) {
	    LogbackUtil.level = level;
	}
	public static void resetLog() {
//		JoranConfigurator jc = new JoranConfigurator();
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
		Logger root = context.getLogger("ROOT");
		root.detachAppender("CONSOLE");
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
		appender.setName("CONSOLE");
//		String logPattern = this.patterns.getProperty("console", CONSOLE_LOG_PATTERN);
//		encoder.setPattern(OptionHelper.substVars(logPattern, config.getContext()));
//		encoder.setCharset(UTF8);
//		config.start(encoder);
//		appender.setEncoder(encoder);
//		config.appender("CONSOLE", appender);
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setPattern("[%-5level][%contextName]%d{yyyy-MM-dd HH:mm:ss.SSS}[%thread][%X{req.requestURI}] [%X{traceId}] %logger - %msg%n");
		encoder.setCharset(Charset.forName("UTF-8"));
		encoder.setContext(context);
		
		appender.setEncoder(encoder);
		appender.setContext(context);
		root.setLevel(level);
		root.addAppender(appender);
		
//		root.setLevel(Level.INFO);
//		root.addAppender(appender);
		
		appender.start();
		encoder.start();
	}

}
