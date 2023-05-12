package com.github.jklasd.test.core.facade;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.github.jklasd.test.core.facade.loader.AnnotationResourceLoader;
import com.github.jklasd.test.core.facade.loader.JunitComponentResourceLoader;
import com.github.jklasd.test.core.facade.loader.JunitResourceLoaderEnum;
import com.github.jklasd.test.core.facade.loader.XMLResourceLoader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JunitResourceLoaderManager {

	private final static JunitResourceLoaderManager resourceLoader = new JunitResourceLoaderManager();
	
	private JunitResourceLoaderManager() {
		Lists.newArrayList(
				XMLResourceLoader.getInstance(),//xml顺序第一位
				AnnotationResourceLoader.getInstance(),
				JunitComponentResourceLoader.getInstance()
				).forEach(loaderClass->regist(loaderClass));
	}
	
	public static JunitResourceLoaderManager getInstance() {
		return resourceLoader;
	}
	
	private Map<String,JunitResourceLoaderI> loaderMap = Maps.newHashMap();
	private List<JunitResourceLoaderI> loaderList = Lists.newArrayList();

	private void regist(JunitResourceLoaderI loader) {
		loaderMap.put(loader.key().getFileName(), loader);
		loaderList.add(loader);
		loaderList.sort(new Comparator<JunitResourceLoaderI>() {
			@Override
			public int compare(JunitResourceLoaderI o1, JunitResourceLoaderI o2) {
				return o1.key().getOrder() - o2.key().getOrder();
			}
		});
	}
	
	public void initLoader() {
		loaderList.forEach(loader->loader.initResource());
	}

	public void loadResource(String name, InputStream inputStream) {
		for(JunitResourceLoaderEnum item : JunitResourceLoaderEnum.values()) {
			if(name.contains(item.getFileName())) {
//				log.info("处理:{}",name);
				loaderMap.get(item.getFileName()).loadResource(inputStream);
			}
		}
	}

}
