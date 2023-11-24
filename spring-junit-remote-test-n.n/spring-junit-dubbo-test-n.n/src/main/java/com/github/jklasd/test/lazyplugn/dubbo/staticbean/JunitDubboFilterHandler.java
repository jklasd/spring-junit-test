package com.github.jklasd.test.lazyplugn.dubbo.staticbean;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.Contants;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.github.jklasd.test.common.util.MethodSnoopUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.common.util.viewmethod.PryMethodInfoI;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.google.common.collect.Sets;

import javassist.CannotCompileException;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理开发人员自定义 dubbo filter中调用了spring bean 静态方法
 * @author jubin.zhang
 *
 */
@Slf4j
public class JunitDubboFilterHandler {

	Class<?> Filter = ScanUtil.loadClass("org.apache.dubbo.rpc.Filter");
	Class<?> ExtensionLoader = ScanUtil.loadClass("org.apache.dubbo.common.extension.ExtensionLoader");
	Class<?> InvokerInvocationHandler = ScanUtil.loadClass("org.apache.dubbo.rpc.proxy.InvokerInvocationHandler");
	
	private LazyBeanI lazyBean = ContainerManager.getComponent(LazyBeanI.class.getSimpleName());
	
	//见ProtocolFilterWrapper # refer
	Set<String> cacheHanded = Sets.newConcurrentHashSet();
	public void exec(Object refer) {
		
		Object handler =  JunitInvokeUtil.invokeReadField("handler", refer);//v 2.7.22
		//InvokerInvocationHandler
		if(!InvokerInvocationHandler.isInstance(handler)) {
			return;
		}
		//org.apache.dubbo.common.URL
		Object url = JunitInvokeUtil.invokeReadField("url", handler);//v 2.7.22
		Object extensionLoader = JunitInvokeUtil.invokeStaticMethod(ExtensionLoader, "getExtensionLoader",new Class<?>[] {Class.class},  Filter);
		String key = "reference.filter";
		String group = "consumer";
		
		List<?> filters = (List<?>) JunitInvokeUtil.invokeMethod(extensionLoader, ExtensionLoader, "getActivateExtension",url,key,group);
		
		//检查Filter#invoke方法
		filters.forEach(filterObj->{
			//org.apache.dubbo
			//com.alibaba.dubbo
			String cName = filterObj.getClass().getName();
			if(!cName.startsWith("org.apache.dubbo")
					&& !cName.startsWith("com.alibaba.dubbo")) {
				if(cacheHanded.contains(cName)) {
					return;
				}
				cacheHanded.add(cName);
				//开发人员自定义Filter
				Method[] ms = filterObj.getClass().getDeclaredMethods();
				for(Method tagMethod:ms) {
					if(tagMethod.getName().equals("invoke")) {
						try {
							PryMethodInfoI methodInfo = MethodSnoopUtil.findNotPublicMethodForClass(tagMethod);
							if(Contants.runPrepareStatic) {
								if(!methodInfo.getFindToStatic().isEmpty()) {
									//处理静态方法
									methodInfo.getFindToStatic().forEach(tagClass->lazyBean.processStatic(tagClass));
								}
							}
						} catch (CannotCompileException e) {
							log.error("JunitDubboFilterHandler 处理异常",e);
						}
						break;
					}
				}
				
			}
		});
	}
}
