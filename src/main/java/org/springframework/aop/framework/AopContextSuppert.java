package org.springframework.aop.framework;
/**
 * 
 * @author jubin.zhang
 *
 */
public class AopContextSuppert{
	public static void setProxyObj(Object obj) {
		AopContext.setCurrentProxy(obj);
	}
}
