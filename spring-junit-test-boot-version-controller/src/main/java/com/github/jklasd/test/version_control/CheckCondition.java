package com.github.jklasd.test.version_control;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.VersionController;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.interf.register.ConditionClassManagerI;

/**
 * Boot 1.5 版本 spring boot condition 校验类
 * @author jubin.zhang
 *
 */
public class CheckCondition implements VersionController,ConditionClassManagerI,ContainerRegister{
	
	public void register() {
		ContainerManager.registComponent(this);
	}

	@Override
	public Class<?>[] getCheckClassArr() {
		return new Class<?>[]{ConditionalOnClass.class,ConditionalOnBean.class,ConditionalOnMissingClass.class};
	}

	@Override
	public Class<?>[] getCheckPropArr() {
		return new Class<?>[]{ConditionalOnProperty.class,ConditionalOnResource.class};
	}

	@Override
	public Class<?>[] getOtherCheckArr() {
		return new Class<?>[]{ConditionalOnCloudPlatform.class,ConditionalOnProperty.class,ConditionalOnResource.class};
	}

	@Override
	public String getBeanKey() {
		return ConditionClassManagerI.class.getSimpleName();
	}
}
