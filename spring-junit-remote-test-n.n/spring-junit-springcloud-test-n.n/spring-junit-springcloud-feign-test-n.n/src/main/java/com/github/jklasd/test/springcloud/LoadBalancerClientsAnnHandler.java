package com.github.jklasd.test.springcloud;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;

import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.interf.handler.ClassAnnHandler;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadBalancerClientsAnnHandler implements ClassAnnHandler,ContainerRegister{

	public static final String BeanName = "LoadBalancerClientsAnnHandler";
	
	@Override
	public String getBeanKey() {
		return BeanName;
	}

	@Override
	public void scanConfig(Class<?> configClass) {
		if(AnnHandlerUtil.isAnnotationPresent(configClass,LoadBalancerClients.class)) {
			//
			log.debug("处理--LoadBalancerClientsAnnHandler==:{}",configClass);
			LoadBalancerClientSpecification lbcs = new LoadBalancerClientSpecification(configClass.getName(),new Class<?>[] {configClass});
			LazyListableBeanFactory.getInstance().registerSingleton(lbcs.getName(), lbcs);
		}
	}

}
