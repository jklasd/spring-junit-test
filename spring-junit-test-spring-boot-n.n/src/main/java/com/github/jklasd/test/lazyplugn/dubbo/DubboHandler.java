package com.github.jklasd.test.lazyplugn.dubbo;

public interface DubboHandler {
	public Object buildBeanNew(Class<?> dubboClass,String beanName);
	public boolean isDubboNew(Class<?> classBean);
	public void registerDubboService(Class<?> exportService);
}
