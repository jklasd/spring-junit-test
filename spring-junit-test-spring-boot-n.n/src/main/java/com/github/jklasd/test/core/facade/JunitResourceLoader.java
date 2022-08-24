package com.github.jklasd.test.core.facade;

import java.io.InputStream;

import com.github.jklasd.test.core.facade.loader.JunitResourceLoaderEnum;

public interface JunitResourceLoader {
	JunitResourceLoaderEnum key();
	void loadResource(InputStream jarFileIs);
	void loadResource(String... sourcePath);
	void initResource();
}
