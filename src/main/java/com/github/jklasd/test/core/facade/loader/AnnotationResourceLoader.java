package com.github.jklasd.test.core.facade.loader;

import java.util.Map;
import java.util.jar.JarFile;

import com.github.jklasd.test.core.facade.Facade;
import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.google.common.collect.Maps;

@Facade("ResourceLoader")
class AnnotationResourceLoader implements JunitResourceLoader{
	
	private Map<String,Class<?>> thridAutoConfigMap = Maps.newConcurrentMap();

	@Override
	public void loadResource(JarFile jarFile) {
		//spring.factories;
		//org.springframework.boot.autoconfigure.EnableAutoConfiguration
	}

	@Override
	public void initResource() {
		// TODO Auto-generated method stub
		
	}

}
