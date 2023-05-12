package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.util.Map;

import com.github.jklasd.test.common.interf.handler.BeanDefParser;
import com.github.jklasd.test.common.interf.handler.BeanDefinitionParserDelegateHandler;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;

public class BeanDefParserLoader implements BeanDefinitionParserDelegateHandler{

	@Override
	public void loadBeanDefParser(Map<String, BeanDefParser> parser) {
		XmlBeanUtil.getInstance().getNamespaceURIList().stream().filter(url->url.contains("dubbo")).forEach(url->{
            parser.put(url, (BeanDefParser)LazyDubboBean.getInstance());
        });
	}

}
