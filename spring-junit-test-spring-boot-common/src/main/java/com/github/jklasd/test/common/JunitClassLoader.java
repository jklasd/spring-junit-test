package com.github.jklasd.test.common;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.common.interf.register.BeanFactoryProcessorI;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.common.util.ClassUtil;
import com.github.jklasd.test.common.util.SignalNotificationUtil;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JunitClassLoader extends ClassLoader{
	private JunitClassLoader() {}
	static JunitClassLoader demo = new JunitClassLoader();
	public static JunitClassLoader getInstance() {
		return demo;
	}

	protected void setBean() {
		if(beanFactoryProcessor==null) {
			beanFactoryProcessor = ContainerManager.getComponent(BeanFactoryProcessorI.class.getSimpleName()); 
		}
		if(lazyBean==null) {
			lazyBean = ContainerManager.getComponent(LazyBeanI.class.getSimpleName()); 
		}
	}
	AnnHandlerUtil annHandler = AnnHandlerUtil.getInstance();
	private ClassUtil scan = ClassUtil.getInstance();
	public Class<?> junitloadClass(String name) throws ClassNotFoundException {
		return loadClass(name);
	}
	private List<Class<?>> staticComponentClass = Lists.newArrayList();
	private BeanFactoryProcessorI beanFactoryProcessor;
	private LazyBeanI lazyBean;
	private boolean staticClassHandled;
	public void processStatic() {
		setBean();
		if(!staticClassHandled) {
			staticClassHandled = true;
			staticComponentClass.forEach(staticClass->{
				if(beanFactoryProcessor.notBFProcessor(staticClass)) {
					lazyBean.processStatic(staticClass);
				}
			});
		}
	}
	
	@SuppressWarnings("unchecked")
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		setBean();
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
//						scan.hasStaticMethod(loadClass);
						if(loadClass.getName().contains("CostConfiguration")) {
							log.debug("断点");
						}
						int process = ContainerManager.stats;
						if(process < ContainerManager.inited) {
							staticComponentClass.add(loadClass);
						}else {
							//预热静态方法的实例对象
							if(beanFactoryProcessor.notBFProcessor(loadClass)) {
								String value = SignalNotificationUtil.get(loadClass.getName());
								if(value == null) {
									lazyBean.processStatic(loadClass);
								}else {
									SignalNotificationUtil.put(loadClass.getName(), "false");
								}
							}
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
