package com.github.jklasd.test.common;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.github.jklasd.test.common.interf.register.BeanFactoryProcessorI;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.common.util.CheckUtil;
import com.github.jklasd.test.common.util.ClassUtil;
import com.github.jklasd.test.common.util.SignalNotificationUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JunitClassLoader extends ClassLoader{
	private JunitClassLoader() {}
	static JunitClassLoader demo = new JunitClassLoader();
	String version = System.getProperty("java.version");
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
	public Class<?> junitloadClass(String name) {
		try {
			return loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	private Set<Class<?>> staticComponentClass = Sets.newConcurrentHashSet();
	private BeanFactoryProcessorI beanFactoryProcessor;
	private LazyBeanI lazyBean;
	private boolean staticClassHandled;
	public void processStatic() {
		setBean();
		if(!staticClassHandled) {
			staticClassHandled = true;
			staticComponentClass.forEach(staticClass->{
				if(beanFactoryProcessor.notBFProcessor(staticClass) && CheckUtil.checkProp(staticClass)) {
					lazyBean.processStatic(staticClass);
				}
			});
		}
	}
	
	Map<String,Class<?>> cache = Maps.newConcurrentMap();
	
	@SuppressWarnings("unchecked")
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		setBean();
		Class<?> loadClass = findLoadedClass(name);
		if(loadClass!=null) {
			return loadClass;
		}
		if(cache.containsKey(name)) {
			return cache.get(name);//重复处理，会导致java.lang.ClassCircularityError
		}
		if(name.equals("module-info")
				&& version.startsWith("1.8")) {
			log.warn("module-info 版本不支持");
			return null;
		}
		loadClass = loadClass(name, false);//cache 防止并发情况下，重复处理
		
		if(Contants.prepareStatic) {
			handHasstaticMethodClass(loadClass);
		}
		cache.put(name, loadClass);
		return loadClass;
    }

	/**
	 * 部分其他非springbean中调用了静态方法，需要提前处理
	 * @param loadClass
	 */
	private void handHasstaticMethodClass(Class<?> loadClass) {
		if(!Modifier.isAbstract(loadClass.getModifiers())
				&& !loadClass.getName().startsWith("org.springframework")) {
			if(AnnHandlerUtil.isAnnotationPresent(loadClass, Component.class)) {
				log.debug("查看{}是否是静态类",loadClass);
				if(scan.hasStaticMethod(loadClass)) {
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
	}
}
