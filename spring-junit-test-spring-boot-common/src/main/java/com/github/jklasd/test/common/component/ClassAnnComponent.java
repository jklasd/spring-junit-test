package com.github.jklasd.test.common.component;

import java.util.List;
import java.util.Set;

import com.github.jklasd.test.common.interf.handler.ClassAnnHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ClassAnnComponent extends AbstractComponent{

	private static List<ClassAnnHandler> handler = Lists.newArrayList();
	
	@Override
	<T> void add(T component) {
		ClassAnnHandler cah = (ClassAnnHandler) component;
		handler.add(cah);
	}
	
	static Set<Class<?>> configClasses = Sets.newHashSet();

	public static void scanConfig(Class<?> configClass) {
		configClasses.add(configClass);
	}
	
	public static void afterScan() {
		handler.forEach(item->{
			configClasses.forEach(configClass->{
				item.scanConfig(configClass);
			});
		});
	}

}
