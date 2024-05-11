package com.github.jklasd.test.springboot.configuration.ann;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.EventPublishingRunListener;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.register.ConfigurationAnnI;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.springboot.configuration.LogbackLoggingSystemExt;

public class ConfigurationAnnImple implements ConfigurationAnnI{

	@Override
	public String getBeanKey() {
		return ConfigurationAnnI.class.getName();
	}


	@Override
	public void environmentPrepared(ConfigurableEnvironment env) {
		new EventPublishingRunListener(new SpringApplication(),new String[] {})
		.environmentPrepared(new DefaultBootstrapContext(), env);
	}

	@Override
	public Map<String, Object> getAnnoData(Class<?> configClass) throws IOException {
		return AnnHandlerUtil.getInstance().getAnnotationValue(configClass,AutoConfigureAfter.class);
	}


	@Override
	public List<Class<?>> findClassWithAnnotation(Map<String, Class<?>> applicationAllClassMap) {
		return ScanUtil.findClassWithAnnotation(SpringBootApplication.class,applicationAllClassMap);
	}


	@Override
	public void initializeLog(ConfigurableEnvironment environment) {
		LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
        LogbackLoggingSystemExt logbackSystem = new LogbackLoggingSystemExt(JunitClassLoader.getInstance());
        logbackSystem.initialize(initializationContext, null, LogFile.get(environment));
	}

}
