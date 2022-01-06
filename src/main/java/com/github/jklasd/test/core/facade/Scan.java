package com.github.jklasd.test.core.facade;

import com.github.jklasd.test.lazybean.model.AssemblyDTO;

public interface Scan {
	void scan();
	public Object[] findCreateBeanFactoryClass(final AssemblyDTO assemblyData);
}
