package com.github.jklasd.test.common.interf.register;

import org.springframework.core.env.ConfigurableEnvironment;

import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.interf.ContainerRegister;

public interface JunitCoreComponentI extends ContainerRegister{

	ConfigurableEnvironment getEnvironment();

	JunitApplicationContext getApplicationContext();

}
