package com.github.jklasd.test.core.facade.scan;

import java.util.Set;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.register.PropResourceManagerI;
import com.google.common.collect.Sets;

public class PropResourceManager implements PropResourceManagerI{
	private PropResourceManager() {}
	
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

	private static PropResourceManager loader = new PropResourceManager();
	public static PropResourceManager getInstance() {
		return loader;
	}

	public Object findCreateByProp(Class<?> tagertC) throws InstantiationException, IllegalAccessException {
		if(contains(tagertC)) {
			return tagertC.newInstance();
		}
		return null;
	}

	@Override
	public String getBeanKey() {
		return PropResourceManagerI.class.getSimpleName();
	}
}
