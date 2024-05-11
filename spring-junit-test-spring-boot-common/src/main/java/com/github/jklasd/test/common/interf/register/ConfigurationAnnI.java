package com.github.jklasd.test.common.interf.register;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;

import com.github.jklasd.test.common.interf.ContainerRegister;

public interface ConfigurationAnnI extends ContainerRegister{

	Map<String,Object> getAnnoData(Class<?> configClass) throws IOException;
	
	void environmentPrepared(ConfigurableEnvironment env);

	List<Class<?>> findClassWithAnnotation(Map<String, Class<?>> applicationAllClassMap);

	void initializeLog(ConfigurableEnvironment environment);

}
