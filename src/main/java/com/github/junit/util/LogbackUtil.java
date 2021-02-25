package com.github.junit.util;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.junit.test.spring.XmlBeanUtil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.util.OptionHelper;
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
				Map<String,String> attr = XmlBeanUtil.loadXmlNodeAttr(nodeList.item(i).getAttributes());
				if(attr.get("class").contains("Console")) {
					root.detachAppender(attr.get("name"));
					
					List<Node> patterList = XmlBeanUtil.findNodeByTag(nodeList.item(i), "pattern");
					String logPattern = patterList.get(0).getTextContent();
					
					if(logPattern.startsWith("${")) {
						logPattern = logPattern.replace("${", "").replace("}", "");
						boolean find = false;
						NodeList propList = document.getElementsByTagName("Property");
						for(int j=0; j<propList.getLength(); j++) {
							Map<String,String> prop = XmlBeanUtil.loadXmlNodeAttr(propList.item(j).getAttributes());
							if(prop.get("name").equals(logPattern)) {
								logPattern = prop.get("value");
								find = true;
							}
						}
						if(!find) {
							NodeList spropList = document.getElementsByTagName("springProperty");
							for(int j=0; j<spropList.getLength(); j++) {
								Map<String,String> prop = XmlBeanUtil.loadXmlNodeAttr(spropList.item(j).getAttributes());
								if(prop.get("name").equals(logPattern)) {
									logPattern = evn.getProperty(prop.get("source"));
									find = true;
								}
							}
						}
					}
					ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
					appender.setName(attr.get("name"));
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

}
