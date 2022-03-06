package com.github.jklasd.test.core.facade;

import java.io.InputStream;

public interface JunitResourceLoader {
	void loadResource(InputStream jarFileIs);
	void loadResource(String... sourcePath);
	void initResource();
}
