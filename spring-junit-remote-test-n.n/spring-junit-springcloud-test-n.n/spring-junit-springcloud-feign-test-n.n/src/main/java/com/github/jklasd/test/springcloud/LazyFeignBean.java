package com.github.jklasd.test.springcloud;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyAbstractPlugnBeanFactory;

/**
 * 参考 FeignClientsRegistrar
 * @author jubin.zhang
 *
 */
public class LazyFeignBean extends LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{

	private static final Class<?> FeignClientsRegistrar = ScanUtil.loadClass("org.springframework.cloud.openfeign.FeignClientsRegistrar");
	private static final Class<? extends Annotation> FeignClient = ScanUtil.loadClass("org.springframework.cloud.openfeign.FeignClient");
	
	public boolean canBeInstance(){
		return FeignClientsRegistrar!=null && FeignClient!=null;
	}
	
	@Override
	public String getName() {
		return "LazyFeignBean";
	}
	
	/**
	 * 配置拓展实例bean构造器顺序，小到大执行
	 * @return
	 */
	public Integer getOrder() {
		return 250;
	}
	
	public boolean isInterfaceBean() {
		return true;
	}
	
	public void afterPropertiesSet(Object obj,BeanModel model) {
		if(obj!=null) {
			BeanInitModel initModel = new BeanInitModel();
			initModel.setObj(obj);
			initModel.setTagClass(obj.getClass());
			initModel.setBeanName(model.getBeanName());
    		LazyBean.getInstance().processAttr(initModel);// 递归注入代理对象
		}
		localCache.remove();
	}

	@Override
	public Object buildBean(BeanModel beanModel) {
		FeignClientClassAnnHandler handler =  ContainerManager.getComponent(FeignClientClassAnnHandler.BeanName);
		return handler.buildBean(localCache.get());
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		FeignClientClassAnnHandler handler =  ContainerManager.getComponent(FeignClientClassAnnHandler.BeanName);
		BeanDefinition tmp = handler.finded(beanModel);
		localCache.set(tmp);
		return tmp!=null;
	}

}
