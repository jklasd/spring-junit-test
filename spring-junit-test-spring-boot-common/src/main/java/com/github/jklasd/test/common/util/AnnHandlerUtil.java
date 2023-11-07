package com.github.jklasd.test.common.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import com.github.jklasd.test.common.JunitClassLoader;

public class AnnHandlerUtil {
	private static AnnHandlerUtil bean = new AnnHandlerUtil();
	private AnnHandlerUtil() {}
	public static AnnHandlerUtil getInstance() {return bean;}
	private CachingMetadataReaderFactory cachingmetadatareaderfactory = new CachingMetadataReaderFactory(JunitClassLoader.getInstance());
	
	public MetadataReader getMetadataReader(String className) throws IOException {
		return cachingmetadatareaderfactory.getMetadataReader(className);
	}
	
	public AnnotationMetadata getAnnotationMetadata(Class<?> configClass) throws IOException {
		return cachingmetadatareaderfactory.getMetadataReader(configClass.getName()).getAnnotationMetadata();
	}
	
	public Map<String, Object> getAnnotationValue(Class<?> configClass,Class<?> annotation) throws IOException {
		return cachingmetadatareaderfactory.getMetadataReader(configClass.getName()).getAnnotationMetadata()
				.getAnnotationAttributes(annotation.getName(), true);
	}
	
	public static boolean isAnnotationPresent(Class<?> configClass,Class<?> annotation){
		try {
			return bean.getAnnotationValue(configClass, annotation)!=null;
		} catch (IOException e) {
			return false;
		}
	}
//	public static boolean isAnnotationPresent(Method method, Class<?> annotation) {
//		try {
//			return bean.getAnnotationValue(method, annotation)!=null;
//		} catch (IOException e) {
//			return false;
//		}
//	}
	public MethodMetadata getAnnotationValue(Method method, String annName) throws IOException {
		Set<MethodMetadata> mmds = cachingmetadatareaderfactory.getMetadataReader(method.getDeclaringClass().getName()).getAnnotationMetadata().getAnnotatedMethods(annName);
		for(MethodMetadata mmd : mmds) {
			if(mmd.getMethodName().equals(method.getName())
					&& mmd.getReturnTypeName().equals(method.getReturnType().getName())
					&& mmd.getDeclaringClassName().equals(method.getDeclaringClass().getName())) {
				return mmd;
			}
		}
		return null;
	}
//	public AnnotatedTypeMetadata getAnnotationMetadata(Method method) throws IOException {
//		return cachingmetadatareaderfactory.getMetadataReader(method.getName()).getAnnotationMetadata();
//	}
	public Set<String> loadAnnoName(Class<?> configClass) throws IOException {
		Set<String> member = cachingmetadatareaderfactory.getMetadataReader(configClass.getName()).getAnnotationMetadata().getAnnotationTypes();
		if(!member.isEmpty()) {
			return member;
		}
		return null;
	}
	
}
