package com.github.jklasd.test.core.facade.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.VersionController;
import com.github.jklasd.test.common.component.BootedHandlerComponent;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.component.MockAnnHandlerComponent;
import com.github.jklasd.test.common.component.ScannerRegistrarComponent;
import com.github.jklasd.test.common.component.VersionControlComponent;
import com.github.jklasd.test.common.interf.handler.BootHandler;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.interf.handler.MockAnnHandler;
import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;
import com.github.jklasd.test.core.facade.JunitResourceLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JunitComponentResourceLoader implements JunitResourceLoader{
	private static JunitComponentResourceLoader loader = new JunitComponentResourceLoader();
	private JunitComponentResourceLoader() {}
	public static JunitResourceLoader getInstance() {
		return loader;
	}

	@Override
	public void loadResource(InputStream jarFileIs) {
		try {
			Properties prop = new Properties();
			prop.load(new InputStreamReader(jarFileIs));
			
			String bootHandlerClassName = prop.getProperty(BootHandler.class.getName());
			if(StringUtils.isNotBlank(bootHandlerClassName)) {
				BootedHandlerComponent.HandlerLoader.load(bootHandlerClassName.split(","));
			}
			
			String factoryClassNames = prop.getProperty(FieldHandler.class.getName());
			if(StringUtils.isNotBlank(factoryClassNames)) {
				FieldAnnComponent.HandlerLoader.load(factoryClassNames.split(","));
			}
			
			String junitMockAnnClassName = prop.getProperty(MockAnnHandler.class.getName());
			if(StringUtils.isNotBlank(junitMockAnnClassName)) {
				MockAnnHandlerComponent.HandlerLoader.load(junitMockAnnClassName.split(","));
			}
			
			String versionControllerClass = prop.getProperty(VersionController.class.getName());
			if(StringUtils.isNotBlank(versionControllerClass)) {
 				VersionControlComponent.load(versionControllerClass.split(","));
			}
			
			String scannerRegistrarClass = prop.getProperty(ScannerRegistrarI.class.getName());
			if(StringUtils.isNotBlank(scannerRegistrarClass)) {
				ScannerRegistrarComponent.load(scannerRegistrarClass.split(","));
			}
		} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			log.error("=====================loadResource=====================",e);
		}
	}

	@Override
	public void loadResource(String... sourcePath) {
		
	}

	@Override
	public void initResource() {
		BootedHandlerComponent.init();
	}
	@Override
	public JunitResourceLoaderEnum key() {
		return JunitResourceLoaderEnum.JUNIT;
	}

}
