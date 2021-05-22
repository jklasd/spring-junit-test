package com.github.jklasd.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.extern.slf4j.Slf4j;

public class LogbackUtil {
	private static Level level = Level.INFO;
	public static void setJunitLevel(Level level) {
	    LogbackUtil.level = level;
	}
	public static void resetLog() {
	    System.out.println("===========LogbackUtil=resetLog===========");
	    try {
            Resource logback = ScanUtil.getRecourceAnyOne("logback.xml");
            Resource logback_spring = ScanUtil.getRecourceAnyOne("logback-spring.xml");
            if(logback!=null) {
                loadLogXml(logback,false);
            }else if(logback_spring!=null){
                loadLogXml(logback_spring,true);
            }else {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                context.reset();
                Logger root = context.getLogger("ROOT");
                ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
                appender.setName("CONSOLE");
                PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                encoder.setPattern("[%-5level][%contextName]%d{yyyy-MM-dd HH:mm:ss.SSS}[%thread][%X{traceId}] %logger - %msg%n");
                encoder.setCharset(Charset.forName("UTF-8"));
                encoder.setContext(context);
                
                appender.setEncoder(encoder);
                appender.setContext(context);
                root.setLevel(level);
                root.addAppender(appender);
                appender.start();
                encoder.start();
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
	}
    private static void loadLogXml(Resource logback,boolean isSpringLogback) throws JoranException, IOException, SAXException, ParserConfigurationException {
        JoranConfigurator jc = new JoranConfigurator();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        jc.setContext(context);
        context.reset(); // 重置默认配置
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(logback.getInputStream());
        if(isSpringLogback) {
            NodeList springPropertys = doc.getElementsByTagName("springProperty");
            for(int i=0 ;i<springPropertys.getLength();i++) {
                Element node = (Element)springPropertys.item(i);
                String key = node.getAttribute("name");
                String val = node.getAttribute("source");
                val = TestUtil.getInstance().getPropertiesValue(val);
                context.putProperty(key, val);
            }
        }
        
        NodeList propertys = doc.getElementsByTagName("Property");
        for(int i=0 ;i<propertys.getLength();i++) {
            Element node = (Element)propertys.item(i);
            String key = node.getAttribute("name");
            String val = node.getAttribute("value");
            val = TestUtil.getInstance().getApplicationContext().getEnvironment().resolvePlaceholders(val);
            context.putProperty(key, val.replace("-", ""));
        }
        jc.doConfigure(logback.getInputStream());
    }

}
