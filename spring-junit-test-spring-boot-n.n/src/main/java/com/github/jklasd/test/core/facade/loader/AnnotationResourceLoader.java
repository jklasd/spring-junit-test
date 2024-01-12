package com.github.jklasd.test.core.facade.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.JunitResourceLoaderI;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.core.facade.scan.ConfigurationScan;
import com.github.jklasd.test.spring.suppert.AopContextSuppert;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnnotationResourceLoader implements JunitResourceLoaderI{
	private static AnnotationResourceLoader loader = new AnnotationResourceLoader();
	private AnnotationResourceLoader() {}
	public static JunitResourceLoaderI getInstance() {
		return loader;
	}
	
	private Set<Class<?>> thridAutoConfigClass = Sets.newConcurrentHashSet();
//	private Map<String,Class<?>> thridAutoConfigMap = Maps.newConcurrentMap();
//	private Map<String,Class<?>> thridAutoPropMap = Maps.newConcurrentMap();
	
	private ConfigurationScan configurationScan = ConfigurationScan.getInstance();
	
	@Override
	public void loadResource(InputStream jarFileIs) {
		//spring.factories;
		//org.springframework.boot.autoconfigure.EnableAutoConfiguration
		try {
			Properties prop = new Properties();
			prop.load(new InputStreamReader(jarFileIs));
			String factoryClassNames = prop.getProperty("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
			if(StringUtils.isNotBlank(factoryClassNames)) {
				loadAutoConfigClass(factoryClassNames.split(","));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadAutoConfigClass(String... classNames) {
		for(String className : classNames) {
			log.debug("loadAutoConfigClass=>{}",className);
			Class<?> configC = ScanUtil.loadClass(className);
			if(configC!=null) {
				thridAutoConfigClass.add(ScanUtil.loadClass(className));
			}
		}
	}

	@Override
	public void initResource() {
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(thridAutoConfigClass)).forEach(configClass->{
			try {
				configurationScan.scanConfigClass(configClass,true);
				ClassScan.getInstance().loadComponentClassForAnnotation(configClass);
			} catch (IOException e) {
				log.error("AnnotationResourceLoader#scanConfigClass",e);
			}
		});
		thridAutoConfigClass.clear();
		thridAutoConfigClass = null;
		AopContextSuppert.registerObj();
	}
	@Override
	public void loadResource(String... sourcePath) {
		
	}
	@Override
	public JunitResourceLoaderEnum key() {
		return JunitResourceLoaderEnum.ANN;
	}

}
