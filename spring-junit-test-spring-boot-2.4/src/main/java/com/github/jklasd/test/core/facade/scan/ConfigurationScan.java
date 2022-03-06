package com.github.jklasd.test.core.facade.scan;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.classreading.MetadataReader;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.common.ClassUtil;
import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.github.jklasd.test.core.facade.loader.PropResourceLoader;
import com.github.jklasd.test.core.facade.loader.XMLResourceLoader;
import com.github.jklasd.test.core.facade.processor.BeanFactoryProcessor;
import com.github.jklasd.test.util.AnnHandlerUtil;
import com.github.jklasd.test.util.CheckUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	
	Set<Class<?>> cacheScanConfig = Sets.newConcurrentHashSet();
	
	public void scanConfigClass(Class<?> configClass) throws IOException {
		if(configClass==null || cacheScanConfig.contains(configClass)) {
			return;
		}
		
//		if(configClass.getName().contains("MongoReactiveDataAutoConfiguration")) {
//			log.debug("=============加载{}=============",configClass);			
//		}
		
		cacheScanConfig.add(configClass);
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
		MetadataReader sourceToProcess = null;
		try {
			Class<?>[] subCs = configClass.getDeclaredClasses();
			for(Class<?> c : subCs) {
				scanConfigClass(c);
			}
		} catch (NoClassDefFoundError e2) {
			sourceToProcess = AnnHandlerUtil.getInstance().getMetadataReader(configClass.getName());
			String[] memberClassNames = sourceToProcess.getClassMetadata().getMemberClassNames();
			scanConfigClasses(memberClassNames);
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
		
		if(!configClass.getName().startsWith("org.springframework") && ClassUtil.getInstance().hasStaticMethod(configClass)) {
			ClassScan.getInstance().loadComponentClass(configClass);
		}
		
		if(ScanUtil.isImple(configClass, BeanFactoryPostProcessor.class)) {
			BeanFactoryProcessor.getInstance().loadProcessor(configClass);
		}
	}
	
}
