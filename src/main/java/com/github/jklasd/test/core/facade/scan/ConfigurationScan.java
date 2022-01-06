package com.github.jklasd.test.core.facade.scan;

import java.io.IOException;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.github.jklasd.test.util.CheckUtil;

public class ConfigurationScan {
	JunitResourceLoader xmlResourceLoader;
	
	private void scanConfigClasses(Class<?>... configClasses) throws IOException {
		for(Class<?> c:configClasses) {
			scanConfigClass(c);
		}
	}

	public void scanConfigClass(Class<?> configClass) throws IOException {
		//@Configuration
		if(configClass.isAnnotationPresent(Configuration.class) && CheckUtil.check(configClass)) {
			//@AutoConfigureBefore
			//@AutoConfigureAfter
			if(configClass.isAnnotationPresent(AutoConfigureAfter.class)) {
				AutoConfigureAfter resource = configClass.getAnnotation(AutoConfigureAfter.class);
				scanConfigClasses(resource.value());
			}
			//@Import
			if(configClass.isAnnotationPresent(Import.class)) {
				Import resource = configClass.getAnnotation(Import.class);
				scanConfigClasses(resource.value());
			}
			
			
			//@ImportResource;
			if(configClass.isAnnotationPresent(ImportResource.class)) {
				/**
				 * 查看导入资源
				 */
				ImportResource resource = configClass.getAnnotation(ImportResource.class);
				if(resource != null) {
					xmlResourceLoader.loadResource(resource.value());
				}
			}
			//@EnableConfigurationProperties
			if(configClass.isAnnotationPresent(EnableConfigurationProperties.class)) {
				
			}
		}
	}
}
