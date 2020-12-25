package org.springframework.aop.framework;

public class AopContextSuppert{
	public static void setProxyObj(Object obj) {
		AopContext.setCurrentProxy(obj);
	}
}
