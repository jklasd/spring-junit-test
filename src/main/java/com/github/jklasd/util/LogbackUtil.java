package com.github.jklasd.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.spring.XmlBeanUtil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogbackUtil {
	public static String level = "info";
	public static void init(Resource logback, StandardServletEnvironment evn) {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger root = lc.getLogger("ROOT");
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			//创建DocumentBuilder对象
			DocumentBuilder db = dbf.newDocumentBuilder();
			//通过DocumentBuilder对象的parser方法加载books.xml文件到当前项目下
			Document document = db.parse(logback.getFile());
			
			NodeList nodeList = document.getElementsByTagName("appender");
			
			for(int i=0;i<nodeList.getLength();i++) {
				Element attr = (Element) nodeList.item(i);
				if(attr.getAttribute("class").contains("Console")) {
					root.detachAppender(attr.getAttribute("name"));
					
					List<Node> patterList = XmlBeanUtil.findNodeByTag(nodeList.item(i), "pattern");
					String logPattern = patterList.get(0).getTextContent();
					
					if(logPattern.startsWith("${")) {
						logPattern = logPattern.replace("${", "").replace("}", "");
						boolean find = false;
						NodeList propList = document.getElementsByTagName("Property");
						for(int j=0; j<propList.getLength(); j++) {
							Element prop = (Element) propList.item(j);
							if(prop.getAttribute("name").equals(logPattern)) {
								logPattern = prop.getAttribute("value");
								find = true;
							}
						}
						if(!find) {
							NodeList spropList = document.getElementsByTagName("springProperty");
							for(int j=0; j<spropList.getLength(); j++) {
								Element prop = (Element) spropList.item(j);
								if(prop.getAttribute("name").equals(logPattern)) {
									logPattern = evn.getProperty(prop.getAttribute("source"));
									find = true;
								}
							}
						}
					}
					ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
					appender.setName(attr.getAttribute("name"));
//					String logPattern = this.patterns.getProperty("console", CONSOLE_LOG_PATTERN);
//					encoder.setPattern(OptionHelper.substVars(logPattern, config.getContext()));
//					encoder.setCharset(UTF8);
//					config.start(encoder);
//					appender.setEncoder(encoder);
//					config.appender("CONSOLE", appender);
					PatternLayoutEncoder encoder = new PatternLayoutEncoder();
					encoder.setPattern(logPattern);
					encoder.setCharset(Charset.forName("UTF-8"));
					encoder.setContext(lc);
					
					appender.setEncoder(encoder);
					appender.setContext(lc);
					root.setLevel(Level.INFO);
					root.addAppender(appender);
					
					appender.start();
					encoder.start();
					break;
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			log.error("解析异常",e);
		}
		
		
//		LogbackConfigurator lcg = new LogbackConfigurator(lc);
//		new DefaultLogbackConfiguration(new LoggingInitializationContext(evn), null).apply(lcg);
	}
	public static void resetLog() {
//		JoranConfigurator jc = new JoranConfigurator();
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
//		context.setName("com.github.jklasd");
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
		root.setLevel(Level.INFO);
		root.addAppender(appender);
		
		root.setLevel(Level.INFO);
		root.addAppender(appender);
		
		appender.start();
		encoder.start();
	}

}
