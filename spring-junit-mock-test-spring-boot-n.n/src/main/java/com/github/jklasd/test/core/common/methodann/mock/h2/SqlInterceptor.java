package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.util.ScanUtil;

public class SqlInterceptor implements ContainerRegister{

	private String ibatisInterceptor = "org.apache.ibatis.plugin.Interceptor";
	
	public Object buildInterceptor() {
		Class<?> interceptor = ScanUtil.loadClass(ibatisInterceptor);
		if(interceptor!=null) {
			return Proxy.newProxyInstance(JunitClassLoader.getInstance(), new Class[] { interceptor }, new IbatisSqlInterceptor());
		}
		return null;
	}
	
	public class IbatisSqlInterceptor implements InvocationHandler{
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
			case "intercept":
				break;
			case "plugin":
				break;
			case "setProperties":
				break;
			default:
				break;
			};
			return null;
		}
	}

	@Override
	public void register() {
		ContainerManager.registComponent(this);
	}

	@Override
	public String getBeanKey() {
		return ContainerManager.NameConstants.SqlInterceptor;
	}

}
