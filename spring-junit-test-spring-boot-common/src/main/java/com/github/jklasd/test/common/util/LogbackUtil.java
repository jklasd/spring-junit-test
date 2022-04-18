package com.github.jklasd.test.common.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.LogbackLoggingSystemExt;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;

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
	private static Level level = Level.INFO;
	private static boolean useLocal = true;
	private static JunitCoreComponentI junitCoreComponent;
	static {
		junitCoreComponent = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
	}
	public static void setJunitLevel(Level level) {
	    LogbackUtil.level = level;
	    useLocal = false;
	}
	public static void resetLog() {
	    try {
            if(useLocal) {
//            	System.out.println("===========LogbackUtil=resetLog===========");
                Resource logback = ScanUtil.getRecourceAnyOne("logback.xml");
                Resource logback_spring = ScanUtil.getRecourceAnyOne("logback-spring.xml");
                if(logback!=null) {
                    loadLogXml(logback,false);
                    return;
                }else if(logback_spring!=null){
                    loadLogXml(logback_spring,true);
                    return;
                }  
            }else {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                context.reset();
                Logger root = context.getLogger("ROOT");
                ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
                appender.setName("CONSOLE");
                PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                encoder.setPattern("[%-5level][%contextName] %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread][%X{traceId}] %logger%line - %msg%n");
                encoder.setCharset(Charset.forName("UTF-8"));
                encoder.setContext(context);
                
                appender.setEncoder(encoder);
                appender.setContext(context);
                root.setLevel(level);
                root.addAppender(appender);
                appender.start();
                encoder.start();
            }
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
//            NodeList springPropertys = doc.getElementsByTagName("springProperty");
//            for(int i=0 ;i<springPropertys.getLength();i++) {
//                Element node = (Element)springPropertys.item(i);
//                String key = node.getAttribute("name");
//                String val = node.getAttribute("source");
//                val = TestUtil.getInstance().getPropertiesValue(val, node.getAttribute("defaultValue"));
//                context.putProperty(key, val);
//            }
            
            LoggingInitializationContext initializationContext = new LoggingInitializationContext(
            		junitCoreComponent.getEnvironment());
            LogbackLoggingSystemExt logbackSystem = new LogbackLoggingSystemExt(JunitClassLoader.getInstance());
            logbackSystem.initialize(initializationContext, null, LogFile.get(junitCoreComponent.getEnvironment()));
            return ;
        }
        
        NodeList propertys = doc.getElementsByTagName("Property");
        for(int i=0 ;i<propertys.getLength();i++) {
            Element node = (Element)propertys.item(i);
            String key = node.getAttribute("name");
            String val = node.getAttribute("value");
            val = junitCoreComponent.getApplicationContext().getEnvironment().resolvePlaceholders(val);
            context.putProperty(key, val.replace("-", ""));
        }
        jc.doConfigure(logback.getInputStream());
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
