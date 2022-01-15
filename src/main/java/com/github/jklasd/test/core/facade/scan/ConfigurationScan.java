package com.github.jklasd.test.core.facade.scan;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.github.jklasd.test.core.facade.loader.PropResourceLoader;
import com.github.jklasd.test.core.facade.loader.XMLResourceLoader;
import com.github.jklasd.test.util.AnnHandlerUtil;
import com.github.jklasd.test.util.CheckUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigurationScan {
	private ConfigurationScan() {}
	private static ConfigurationScan scaner = new ConfigurationScan();
	public static ConfigurationScan getInstance() {return scaner;};
	
	JunitResourceLoader xmlResourceLoader = XMLResourceLoader.getInstance();
	PropResourceLoader propLoader = PropResourceLoader.getInstance();
	BeanCreaterScan beanCreaterScan = BeanCreaterScan.getInstance();
	
	private void scanConfigClasses(String... classNames) throws IOException {
		for(String cName:classNames) {
			scanConfigClass(ScanUtil.loadClass(cName));
		}
	}
	public void scanConfigClass(Class<?> configClass) throws IOException {
		if(configClass==null || beanCreaterScan.contains(configClass)) {
			return;
		}
		//@Configuration
		if(!AnnHandlerUtil.isAnnotationPresent(configClass, Configuration.class)) {
			return;
		}
		if(!CheckUtil.checkClassExists(configClass)) {
			return;
		}
		
		//@AutoConfigureBefore
		//@AutoConfigureAfter
		//@Import
		Lists.newArrayList(AutoConfigureAfter.class,Import.class).forEach(ann->{
			try {
				Map<String,Object> attr = AnnHandlerUtil.getInstance().getAnnotationValue(configClass, ann);
				if(attr!=null) {
					scanConfigClasses((String[]) attr.get("value"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		if(!CheckUtil.checkProp(configClass)) {
			return;
		}
		beanCreaterScan.load(configClass);
		try {
			Class<?>[] subCs = configClass.getDeclaredClasses();
			for(Class<?> c : subCs) {
				scanConfigClass(c);
			}
		} catch (NoClassDefFoundError e2) {
			log.error("scanConfigClass=>{}",configClass,e2);
		} catch (Exception e2) {
			log.error("scanConfigClass=>{}",configClass,e2);
		}
		//@ImportResource;
		if(AnnHandlerUtil.isAnnotationPresent(configClass,ImportResource.class)) {
			Map<String,Object> attr = AnnHandlerUtil.getInstance().getAnnotationValue(configClass, ImportResource.class);
			xmlResourceLoader.loadResource((String[]) attr.get("value"));
		}
		//@EnableConfigurationProperties
		if(AnnHandlerUtil.isAnnotationPresent(configClass,EnableConfigurationProperties.class)) {
			Map<String,Object> attr = AnnHandlerUtil.getInstance().getAnnotationValue(configClass, EnableConfigurationProperties.class);
			for(String cName:(String[]) attr.get("value")) {
				propLoader.loadResource(ScanUtil.loadClass(cName));
			}
		}
		//@PropertySource
		if(AnnHandlerUtil.isAnnotationPresent(configClass,PropertySource.class)) {
			Map<String,Object> attr = AnnHandlerUtil.getInstance().getAnnotationValue(configClass, PropertySource.class);
			for(String sourcePath : (String[]) attr.get("value")) {
				Resource resource = ScanUtil.getRecourceAnyOne(sourcePath);
				TestUtil.getInstance().loadEnv(sourcePath,resource.getFilename());
			}
		}
	}
}
