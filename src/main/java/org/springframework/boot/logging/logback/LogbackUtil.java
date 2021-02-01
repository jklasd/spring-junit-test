package org.springframework.boot.logging.logback;

import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

import ch.qos.logback.classic.LoggerContext;

public class LogbackUtil {

	public static void init(StandardServletEnvironment evn) {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.getLogger("ROOT").detachAppender("STDOUT");
		LogbackConfigurator lcg = new LogbackConfigurator(lc);
		new DefaultLogbackConfiguration(new LoggingInitializationContext(evn), null).apply(lcg);
	}

}
