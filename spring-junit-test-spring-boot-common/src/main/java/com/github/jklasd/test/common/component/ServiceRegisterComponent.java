package com.github.jklasd.test.common.component;

import java.util.Map;

import com.github.jklasd.test.common.interf.handler.RegisterHandler;
import com.github.jklasd.test.common.util.ScanUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ServiceRegisterComponent extends AbstractComponent{

	
	private static Map<String,RegisterHandler> registerHandlerCollections = Maps.newHashMap();
	
	public static void registerConfiguration(Class<?> rClass) {
		ScanUtil.isBasicClass(rClass);
	}
	
	public static void exportDubboService(Class<?> rClass) {
		if(!registerHandlerCollections.containsKey("DubboRegisterHandler")) {
			log.warn("未加载到dubbo register");
			return;
		}
		registerHandlerCollections.get("DubboRegisterHandler").registClass(rClass);
	}

	@Override
	<T> void add(T component) {
		RegisterHandler tmp = (RegisterHandler) component;
		registerHandlerCollections.put(tmp.getRegisterKey(),tmp);
	}
}
