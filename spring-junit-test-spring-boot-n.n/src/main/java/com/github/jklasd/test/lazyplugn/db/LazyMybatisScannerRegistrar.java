package com.github.jklasd.test.lazyplugn.db;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringUtils;

import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyMybatisMapperBean;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.version_control.AnnotationMetadataUtil;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyMybatisScannerRegistrar implements ScannerRegistrarI {
	
	private static Class<? extends Annotation> mapperScanClass;
	
	private final static String MapperScanName = "org.mybatis.spring.annotation.MapperScan";
	
	private final static String MapperFactoryBeanName = "org.mybatis.spring.mapper.MapperFactoryBean";
	
	private final static String MapperScannerRegistrarName = "org.mybatis.spring.mapper.MapperScannerConfigurer";

	private Class<? extends FactoryBean> MapperFactoryBean;
	
	private LazyApplicationContext registry = LazyApplicationContext.getInstance();
	
	
	LazyMybatisMapperBean lazyMybatis = (LazyMybatisMapperBean) LazyMybatisMapperBean.getInstance();
	
	@SuppressWarnings("unchecked")
	public final Class<? extends Annotation> getAnnotionClass() {
        if (mapperScanClass == null) {
        	mapperScanClass  = ScanUtil.loadClass(MapperScanName);
        }
        if(MapperFactoryBean == null) {
        	MapperFactoryBean = ScanUtil.loadClass(MapperFactoryBeanName);
        }
        return mapperScanClass;
    }
	
	@Override
	public boolean using() {
		return LazyMybatisMapperBean.useMybatis();
	}

	@Override
	public void scannerAndRegister() {
		List<Class<?>> configurableList = ScanUtil.findClassWithAnnotation(Configuration.class,ClassScan.getApplicationAllClassMap());
		Class<? extends Annotation> mybatisScanAnno = getAnnotionClass();
		log.info("configurableList=>{}",configurableList);
		List<?> mapperScan = Lists.newArrayList();
		configurableList.stream()
		.filter(configura ->configura!=null && configura.getAnnotation(mybatisScanAnno)!=null)
		.forEach(configura ->{
			AnnotationAttributes mapperScanAttrs = AnnotationAttributes
			        .fromMap(AnnotationMetadataUtil.from(configura).getAnnotationAttributes(MapperScanName));
			
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerRegistrarName);
			
			Class<? extends Annotation> annotationClass = mapperScanAttrs.getClass("annotationClass");
		    if (!Annotation.class.equals(annotationClass)) {
		      builder.addPropertyValue("annotationClass", annotationClass);
		    }

		    Class<?> markerInterface = mapperScanAttrs.getClass("markerInterface");
		    if (!Class.class.equals(markerInterface)) {
		      builder.addPropertyValue("markerInterface", markerInterface);
		    }

		    Class<? extends BeanNameGenerator> generatorClass = mapperScanAttrs.getClass("nameGenerator");
		    if (!BeanNameGenerator.class.equals(generatorClass)) {
		      builder.addPropertyValue("nameGenerator", BeanUtils.instantiateClass(generatorClass));
		    }

		    Class mapperFactoryBeanClass = mapperScanAttrs.getClass("factoryBean");
		    if (!MapperFactoryBean.equals(mapperFactoryBeanClass)) {
		      builder.addPropertyValue("mapperFactoryBeanClass", MapperFactoryBean);
		    }

		    String sqlSessionTemplateRef = mapperScanAttrs.getString("sqlSessionTemplateRef");
		    if (StringUtils.hasText(sqlSessionTemplateRef)) {
		      builder.addPropertyValue("sqlSessionTemplateBeanName", mapperScanAttrs.getString("sqlSessionTemplateRef"));
		    }

		    String sqlSessionFactoryRef = mapperScanAttrs.getString("sqlSessionFactoryRef");
		    if (StringUtils.hasText(sqlSessionFactoryRef)) {
		      builder.addPropertyValue("sqlSessionFactoryBeanName", mapperScanAttrs.getString("sqlSessionFactoryRef"));
		    }

		    List<String> basePackages = new ArrayList<>();
		    basePackages.addAll(
		        Arrays.stream(mapperScanAttrs.getStringArray("value")).filter(StringUtils::hasText).collect(Collectors.toList()));

		    basePackages.addAll(Arrays.stream(mapperScanAttrs.getStringArray("basePackages")).filter(StringUtils::hasText)
		        .collect(Collectors.toList()));

		    basePackages.addAll(Arrays.stream(mapperScanAttrs.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName)
		        .collect(Collectors.toList()));
			
		    if(!basePackages.isEmpty()) {
		    	builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));
		    }
		    
		    registry.registerBeanDefinition(mapperScanClass.getName()+"#scanBy"+configura.getSimpleName(), builder.getBeanDefinition());
			
		    lazyMybatis.processConfig(configura,basePackages);
		});
		
		lazyMybatis.processScannerConfig();
	}
}
