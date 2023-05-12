package com.github.jklasd.test.common.component;

import com.github.jklasd.test.common.JunitClassLoader;

public abstract class AbstractComponent{
	public static void loadComponent(AbstractComponent component,String... handlerClasses) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		for(String hclass :handlerClasses) {
			Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
			component.add(handlerClass.newInstance());
		}
	}
	
	abstract <T> void add(T component);
	
}
