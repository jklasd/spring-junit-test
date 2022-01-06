package com.github.jklasd.test.core.facade.loader;

import java.util.Set;

import com.github.jklasd.test.lazyplugn.spring.LazyConfigurationPropertiesBindingPostProcessor;
import com.google.common.collect.Sets;

public class PropResourceLoader {
	private PropResourceLoader() {}
	
	private Set<Class<?>> sets = Sets.newConcurrentHashSet();
	
	public void loadResource(Class<?>... value) {
		if(value == null) {
			return;
		}
		for(Class<?> c:value) {
			sets.add(c);
		}
	}

	public boolean contains(Class<?> tag) {
		return sets.contains(tag);
	}

	private static PropResourceLoader loader = new PropResourceLoader();
	public static PropResourceLoader getInstance() {
		return loader;
	}

	public Object findCreateByProp(Class<?> tagertC) throws InstantiationException, IllegalAccessException {
		if(contains(tagertC)) {
			return tagertC.newInstance();
		}
		return null;
	}

}
