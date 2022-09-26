package com.github.jklasd.test.lazyplugn.db;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.StringUtils;

import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyMybatisMapperBean;
import com.github.jklasd.test.version_control.AnnotationMetadataUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyMybatisScannerRegistrar implements ScannerRegistrarI {
	
	private static Class<? extends Annotation> mapperScanClass;
	
	private final static String MapperScanName = "org.mybatis.spring.annotation.MapperScan";
	
	LazyMybatisMapperBean lazyMybatis = (LazyMybatisMapperBean) LazyMybatisMapperBean.getInstance();
	
	@SuppressWarnings("unchecked")
	public final Class<? extends Annotation> getAnnotionClass() {
        if (mapperScanClass == null) {
        	mapperScanClass  = ScanUtil.loadClass(MapperScanName);
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
		configurableList.stream()
		.filter(configura ->configura!=null && configura.getAnnotation(mybatisScanAnno)!=null)
		.forEach(configura ->{
			AnnotationAttributes mapperScanAttrs = AnnotationAttributes
			        .fromMap(AnnotationMetadataUtil.from(configura).getAnnotationAttributes(MapperScanName));
			
//			Class<? extends Annotation> annotationClass = mapperScanAttrs.getClass("annotationClass");
//		    if (!Annotation.class.equals(annotationClass)) {
//		      builder.addPropertyValue("annotationClass", annotationClass);
//		    }
//
//		    Class<?> markerInterface = annoAttrs.getClass("markerInterface");
//		    if (!Class.class.equals(markerInterface)) {
//		      builder.addPropertyValue("markerInterface", markerInterface);
//		    }
//
//		    Class<? extends BeanNameGenerator> generatorClass = annoAttrs.getClass("nameGenerator");
//		    if (!BeanNameGenerator.class.equals(generatorClass)) {
//		      builder.addPropertyValue("nameGenerator", BeanUtils.instantiateClass(generatorClass));
//		    }
//
//		    Class<? extends MapperFactoryBean> mapperFactoryBeanClass = annoAttrs.getClass("factoryBean");
//		    if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
//		      builder.addPropertyValue("mapperFactoryBeanClass", mapperFactoryBeanClass);
//		    }
//
//		    String sqlSessionTemplateRef = annoAttrs.getString("sqlSessionTemplateRef");
//		    if (StringUtils.hasText(sqlSessionTemplateRef)) {
//		      builder.addPropertyValue("sqlSessionTemplateBeanName", annoAttrs.getString("sqlSessionTemplateRef"));
//		    }
//
//		    String sqlSessionFactoryRef = annoAttrs.getString("sqlSessionFactoryRef");
//		    if (StringUtils.hasText(sqlSessionFactoryRef)) {
//		      builder.addPropertyValue("sqlSessionFactoryBeanName", annoAttrs.getString("sqlSessionFactoryRef"));
//		    }

		    List<String> basePackages = new ArrayList<>();
		    basePackages.addAll(
		        Arrays.stream(mapperScanAttrs.getStringArray("value")).filter(StringUtils::hasText).collect(Collectors.toList()));

		    basePackages.addAll(Arrays.stream(mapperScanAttrs.getStringArray("basePackages")).filter(StringUtils::hasText)
		        .collect(Collectors.toList()));

		    basePackages.addAll(Arrays.stream(mapperScanAttrs.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName)
		        .collect(Collectors.toList()));
			
			
		    lazyMybatis.processConfig(configura,basePackages);
		});
		lazyMybatis.processScannerConfig();
	}
}
