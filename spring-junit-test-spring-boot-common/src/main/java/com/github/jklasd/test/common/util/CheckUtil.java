package com.github.jklasd.test.common.util;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.register.BootAnnCheckI;

import lombok.extern.slf4j.Slf4j;

/**
 * 需要加載完处理
 * @author jubin.zhang
 *
 */
@Slf4j
public class CheckUtil {
	public static boolean checkClassExists(Class<?> configClass) {
		BootAnnCheckI baci = ContainerManager.getComponent(BootAnnCheckI.class);
		return baci==null || baci.checkClassExists(configClass);
	}

	public static boolean checkClassExistsForMethod(Method m) {
		BootAnnCheckI baci = ContainerManager.getComponent(BootAnnCheckI.class);
		return baci==null || baci.checkClassExistsForMethod(m);
	}

	public static boolean checkProp(Class<?> staticClass) {
		BootAnnCheckI baci = ContainerManager.getComponent(BootAnnCheckI.class);
		return baci==null || baci.checkProp(staticClass);
	}

}
