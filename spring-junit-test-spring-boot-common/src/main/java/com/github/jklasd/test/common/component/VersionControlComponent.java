package com.github.jklasd.test.common.component;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.VersionController;

public class VersionControlComponent {

	public static void load(String[] className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		for(String hclass :className) {
			Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
			VersionController handler = (VersionController) handlerClass.newInstance();
			handler.register();
		}
	}

}
