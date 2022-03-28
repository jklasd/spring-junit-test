package com.github.jklasd.test.core.facade.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.core.common.FieldAnnComponent;
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
			String factoryClassNames = prop.getProperty("com.github.jklasd.test.core.common.FieldAnnUtil.FieldHandler");
			if(StringUtils.isNotBlank(factoryClassNames)) {
				FieldAnnComponent.HandlerLoader.load(factoryClassNames.split(","));
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
		// TODO Auto-generated method stub
		
	}

}
