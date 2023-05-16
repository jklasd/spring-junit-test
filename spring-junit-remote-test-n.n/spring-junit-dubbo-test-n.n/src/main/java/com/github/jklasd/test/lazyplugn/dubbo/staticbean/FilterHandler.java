package com.github.jklasd.test.lazyplugn.dubbo.staticbean;

import java.lang.invoke.MethodType;
import java.util.List;

import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyDubboBean;
import com.github.jklasd.test.util.JunitInvokeUtil;

/**
 * 处理开发人员自定义 dubbo filter中调用了spring bean 静态方法
 * @author jubin.zhang
 *
 */
public class FilterHandler {

	Class<?> Filter = ScanUtil.loadClass("org.apache.dubbo.rpc.Filter");
	Class<?> ExtensionLoader = ScanUtil.loadClass("org.apache.dubbo.common.extension.ExtensionLoader");
	
	//见ProtocolFilterWrapper # refer
	public void exec(String url) {
		if(!LazyDubboBean.useDubbo()) {
			return;
		}
		
		MethodType methodType = MethodType.methodType(Class.class);
		Object extensionLoader = JunitInvokeUtil.invokeStaticMethod(ExtensionLoader, "getExtensionLoader", methodType, Filter);
		
//		List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);
		String key = "reference.filter";
		String group = "consumer";
		
		List filters = (List) JunitInvokeUtil.invokeMethod(extensionLoader, ExtensionLoader, "getActivateExtension",url,key,group);
		
		//检查Filter#invoke方法
		
	}
}
