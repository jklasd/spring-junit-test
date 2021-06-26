package com.github.jklasd.test.spring.suppert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.springframework.boot.autoconfigure.domain.EntityScanPackages;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 *
 * EntityScanPackages临时类
 */
@Slf4j
public class EntityScanPackagesConstructor{
    private static EntityScanPackages bean;
	public static synchronized EntityScanPackages getBean() {
		if(bean == null) {
		    Constructor<?>[] cons = EntityScanPackages.class.getDeclaredConstructors();
		    cons[0].setAccessible(true);
		    try {
		        bean = (EntityScanPackages)cons[0].newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
                 log.error("EntityScanPackages 临时类",e);
            }
		}
		return bean;
	}
}
