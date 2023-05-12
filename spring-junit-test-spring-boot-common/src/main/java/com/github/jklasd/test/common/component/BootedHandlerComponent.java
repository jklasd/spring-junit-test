package com.github.jklasd.test.common.component;

import java.util.List;

import com.github.jklasd.test.common.interf.handler.BootHandler;
import com.google.common.collect.Lists;

public class BootedHandlerComponent extends AbstractComponent{
	
	private static List<BootHandler> bootList = Lists.newArrayList();
	
	public static void init() {
		bootList.stream().forEach(item->item.launcher());
	}

	@Override
	public void add(Object component) {
		bootList.add((BootHandler) component);
	}

}
