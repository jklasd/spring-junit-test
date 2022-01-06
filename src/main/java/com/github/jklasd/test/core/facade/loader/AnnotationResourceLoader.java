package com.github.jklasd.test.core.facade.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.github.jklasd.test.core.facade.scan.ConfigurationScan;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnnotationResourceLoader implements JunitResourceLoader{
	
	private Set<Class<?>> thridAutoConfigClass = Sets.newConcurrentHashSet();
	private Map<String,Class<?>> thridAutoConfigMap = Maps.newConcurrentMap();
	private Map<String,Class<?>> thridAutoPropMap = Maps.newConcurrentMap();
	
	private ConfigurationScan configurationScan = new ConfigurationScan();
	
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
				configurationScan.scanConfigClass(configClass);
			} catch (IOException e) {
				log.error("AnnotationResourceLoader#scanConfigClass",e);
			}
		});
	}
	private CachingMetadataReaderFactory cachingmetadatareaderfactory = new CachingMetadataReaderFactory(this.getClass().getClassLoader());
	private Map<String, Object> getAnnotationValue(Class<?> configClass,Class<?> annotation) throws IOException {
		return cachingmetadatareaderfactory.getMetadataReader(configClass.getName()).getAnnotationMetadata()
				.getAnnotationAttributes(annotation.getName(), true);
	}

	@Override
	public Object[] findCreateBeanFactoryClass(AssemblyDTO assemblyData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadResource(String... sourcePath) {
		// TODO Auto-generated method stub
		
	}

}
