package com.github.jklasd.test.common.component;

import java.util.List;
import java.util.Map;

import com.github.jklasd.test.common.interf.handler.BeanDefParser;
import com.github.jklasd.test.common.interf.handler.BeanDefinitionParserDelegateHandler;
import com.google.common.collect.Lists;

public class BeanDefinitionParserDelegateComponent extends AbstractComponent{

	static List<BeanDefinitionParserDelegateHandler> cacheHandler = Lists.newArrayList();
	
	@Override
	<T> void add(T component) {
		cacheHandler.add((BeanDefinitionParserDelegateHandler) component);
	}

	public static void loadBeanDefParser(Map<String, BeanDefParser> parser) {
		cacheHandler.forEach(item->item.loadBeanDefParser(parser));
	}
}
