package org.springframework.aop.framework;

public class AopContextSuppert extends AopContext{
	public static void setProxyObj(Object obj) {
		setCurrentProxy(obj);
	}
}
