package com.github.jklasd.test.common.interf.register;

import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.model.AssemblyDTO;

public interface BeanScanI extends ContainerRegister{

	Object[] findCreateBeanFactoryClass(AssemblyDTO assemblyData);

}
