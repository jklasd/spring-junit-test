package org.springframework.aop.framework;
/**
 * 
 * @author jubin.zhang
 * AopContext 工具类
 */
public class AopContextSuppert{
	public static void setProxyObj(Object obj) {
		AopContext.setCurrentProxy(obj);
	}
}
