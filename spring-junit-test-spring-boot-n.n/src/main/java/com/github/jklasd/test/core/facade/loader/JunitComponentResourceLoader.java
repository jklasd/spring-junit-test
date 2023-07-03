package com.github.jklasd.test.core.facade.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.VersionController;
import com.github.jklasd.test.common.component.AbstractComponent;
import com.github.jklasd.test.common.component.BeanDefinitionParserDelegateComponent;
import com.github.jklasd.test.common.component.BootedHandlerComponent;
import com.github.jklasd.test.common.component.ClassAnnComponent;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.component.LazyPlugnBeanFactoryComponent;
import com.github.jklasd.test.common.component.MockAnnHandlerComponent;
import com.github.jklasd.test.common.component.ScannerRegistrarComponent;
import com.github.jklasd.test.common.component.VersionControlComponent;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.interf.handler.BeanDefinitionParserDelegateHandler;
import com.github.jklasd.test.common.interf.handler.BootHandler;
import com.github.jklasd.test.common.interf.handler.ClassAnnHandler;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.interf.handler.MockClassHandler;
import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;
import com.github.jklasd.test.core.facade.JunitResourceLoaderI;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JunitComponentResourceLoader implements JunitResourceLoaderI{
	private static JunitComponentResourceLoader loader = new JunitComponentResourceLoader();
	private JunitComponentResourceLoader() {}
	public static JunitResourceLoaderI getInstance() {
		return loader;
	}
	
	Map<String,Class<?>> componentCache = Maps.newHashMap();
	{
		componentCache.put(BootHandler.class.getName(), BootedHandlerComponent.class);
		componentCache.put(FieldHandler.class.getName(), FieldAnnComponent.class);
		componentCache.put(MockClassHandler.class.getName(), MockAnnHandlerComponent.class);
		componentCache.put(VersionController.class.getName(), VersionControlComponent.class);
		componentCache.put(ScannerRegistrarI.class.getName(), ScannerRegistrarComponent.class);
		componentCache.put(BeanDefinitionParserDelegateHandler.class.getName(), BeanDefinitionParserDelegateComponent.class);
		componentCache.put(LazyPlugnBeanFactory.class.getName(), LazyPlugnBeanFactoryComponent.class);
		componentCache.put(ClassAnnHandler.class.getName(), ClassAnnComponent.class);
	}
	

	@Override
	public void loadResource(InputStream jarFileIs) {
		try {
			Properties prop = new Properties();
			prop.load(new InputStreamReader(jarFileIs));
			
			
			String containerName = prop.getProperty(ContainerRegister.class.getName());
			if(StringUtils.isNotBlank(containerName)) {
				ContainerManager.HandlerLoader.load(containerName.split(","));
			}
			
			Iterator<Entry<String, Class<?>>> iterator = componentCache.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<String, Class<?>> item = iterator.next();
				String beanDefinitionParserDelegateHandlerName = prop.getProperty(item.getKey());
				if(StringUtils.isNotBlank(beanDefinitionParserDelegateHandlerName)) {
					AbstractComponent.loadComponent((AbstractComponent) ContainerManager.createAndregistComponent(item.getValue()),beanDefinitionParserDelegateHandlerName.split(","));
				}
			}
			
			
		} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("=====================loadResource=====================",e);
		}
	}

	@Override
	public void loadResource(String... sourcePath) {
		
	}

	@Override
	public void initResource() {
		BootedHandlerComponent.init();
	}
	@Override
	public JunitResourceLoaderEnum key() {
		return JunitResourceLoaderEnum.JUNIT;
	}

}
