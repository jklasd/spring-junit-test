package com.github.jklasd.test.core.facade;

import java.util.jar.JarFile;

public interface JunitResourceLoader {
	void loadResource(JarFile jarFile);
	void initResource();
}
