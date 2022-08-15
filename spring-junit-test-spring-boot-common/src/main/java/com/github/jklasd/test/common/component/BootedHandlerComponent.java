package com.github.jklasd.test.common.component;

import java.util.List;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.handler.BootHandler;
import com.google.common.collect.Lists;

public class BootedHandlerComponent {
	
	private static List<BootHandler> bootList = Lists.newArrayList();
	
	public static class HandlerLoader{
		public static void load(String... handlerClasses) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
			for(String hclass :handlerClasses) {
				Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
				BootHandler handler = (BootHandler) handlerClass.newInstance();
				bootList.add(handler);
			}
		}
	}

	public static void init() {
		bootList.stream().forEach(item->item.launcher());
	}
}
