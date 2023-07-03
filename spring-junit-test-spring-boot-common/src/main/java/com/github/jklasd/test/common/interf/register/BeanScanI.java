package com.github.jklasd.test.common.interf.register;

import java.util.List;

import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.JunitMethodDefinition;

public interface BeanScanI extends ContainerRegister{

	JunitMethodDefinition findCreateBeanFactoryClass(BeanModel assemblyData);

	List<JunitMethodDefinition> findCreateBeanFactoryClasses(BeanModel assemblyData);

}
