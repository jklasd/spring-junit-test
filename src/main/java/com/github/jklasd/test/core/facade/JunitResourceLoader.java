package com.github.jklasd.test.core.facade;

import java.io.InputStream;

import com.github.jklasd.test.lazybean.model.AssemblyDTO;

public interface JunitResourceLoader {
	void loadResource(InputStream jarFileIs);
	void loadResource(String... sourcePath);
	void initResource();
	public Object[] findCreateBeanFactoryClass(final AssemblyDTO assemblyData);
}
