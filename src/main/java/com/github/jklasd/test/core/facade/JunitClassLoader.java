package com.github.jklasd.test.core.facade;

import java.io.IOException;
import java.lang.reflect.Modifier;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.core.facade.processor.BeanFactoryProcessor;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.AnnHandlerUtil;

public class JunitClassLoader extends ClassLoader{
	private JunitClassLoader() {}
	static JunitClassLoader demo = new JunitClassLoader();
	public static JunitClassLoader getInstance() {
		return demo;
	}
	AnnHandlerUtil annHandler = AnnHandlerUtil.getInstance();
	private ClassScan scan = ClassScan.getInstance();
	
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> loadClass = loadClass(name, false);
		try {
			if(!Modifier.isAbstract(loadClass.getModifiers())) {
				AnnotationMetadata meta = annHandler.getAnnotationMetadata(loadClass);
				if(meta.hasAnnotation(Component.class.getName())
						|| meta.hasAnnotation(Service.class.getName())
						|| meta.hasAnnotation(Configuration.class.getName())
						|| meta.hasAnnotation(Repository.class.getName())
						) {
					if(scan.hasStaticMethod(loadClass) 
							&& !loadClass.getName().startsWith("org.springframework")) {
						//预热静态方法的实例对象
						if(BeanFactoryProcessor.getInstance().notBFProcessor(loadClass)) {
							LazyBean.getInstance().processStatic(loadClass);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return loadClass;
    }
}
