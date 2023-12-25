package com.github.jklasd.test.common.component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.jklasd.test.common.interf.handler.RegisterHandler;
import com.github.jklasd.test.common.util.ScanUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ServiceRegisterComponent extends AbstractComponent{

	
	private static Map<String,RegisterHandler> registerHandlerCollections = Maps.newHashMap();
	
	public static void registerConfiguration(Class<?> rClass) {
		if(ScanUtil.isBasicClass(rClass)) {
			return;
		}
		
		if(!registerHandlerCollections.containsKey("ConfigurationRegisterHandler")) {
			return;
		}
		registerHandlerCollections.get("ConfigurationRegisterHandler").registClass(rClass);
	}
	
	public static void exportDubboService(TimeUnit time,long timeValue,Class<?>... rClasses) {
		if(!registerHandlerCollections.containsKey("DubboRegisterHandler")) {
			log.warn("未加载到dubbo register");
			return;
		}
		for(Class<?> rClass : rClasses) {
			registerHandlerCollections.get("DubboRegisterHandler").registClass(rClass);
		}
		try {
			time.sleep(timeValue);
		} catch (InterruptedException e) {
			log.error("exportDubboService",e);
		}
	}
	public static void exportDubboService(Class<?>... rClass) {
		exportDubboService(TimeUnit.MINUTES, 1,rClass);
	}

	@Override
	<T> void add(T component) {
		RegisterHandler tmp = (RegisterHandler) component;
		registerHandlerCollections.put(tmp.getRegisterKey(),tmp);
	}
}
