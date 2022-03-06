package com.github.jklasd.test.core.facade;

import java.io.IOException;
import java.lang.reflect.Modifier;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.core.common.ClassUtil;
import com.github.jklasd.test.core.facade.processor.BeanFactoryProcessor;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.AnnHandlerUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JunitClassLoader extends ClassLoader{
	private JunitClassLoader() {}
	static JunitClassLoader demo = new JunitClassLoader();
	public static JunitClassLoader getInstance() {
		return demo;
	}
	AnnHandlerUtil annHandler = AnnHandlerUtil.getInstance();
	private ClassUtil scan = ClassUtil.getInstance();
	
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> loadClass = findLoadedClass(name);
		if(loadClass!=null) {
			return loadClass;
		}
		loadClass = loadClass(name, false);
		try {
			if(!Modifier.isAbstract(loadClass.getModifiers())
					&& !loadClass.getName().startsWith("org.springframework")) {
				AnnotationMetadata meta = annHandler.getAnnotationMetadata(loadClass);
				if(meta.hasAnnotation(Component.class.getName())
						|| meta.hasAnnotation(Service.class.getName())
						|| meta.hasAnnotation(Configuration.class.getName())
						|| meta.hasAnnotation(Repository.class.getName())
						) {
					log.debug("查看{}是否是静态类",loadClass);
					if(scan.hasStaticMethod(loadClass)) {
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
