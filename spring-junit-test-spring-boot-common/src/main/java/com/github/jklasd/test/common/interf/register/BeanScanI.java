package com.github.jklasd.test.common.interf.register;

import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.model.AssemblyDTO;
import com.github.jklasd.test.common.model.JunitMethodDefinition;

public interface BeanScanI extends ContainerRegister{

	JunitMethodDefinition findCreateBeanFactoryClass(AssemblyDTO assemblyData);

}
