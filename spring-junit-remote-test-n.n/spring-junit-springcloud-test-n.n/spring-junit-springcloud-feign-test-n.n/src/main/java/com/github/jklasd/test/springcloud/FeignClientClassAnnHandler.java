package com.github.jklasd.test.springcloud;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientsRegistrarSupper;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.interf.handler.ClassAnnHandler;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignClientClassAnnHandler implements ClassAnnHandler,ContainerRegister{
	
	public static final String BeanName = "FeignClientClassAnnHandler";
	//EnableFeignClients
	FeignClientsRegistrarSupper fcSupper = new FeignClientsRegistrarSupper();
	
	@Override
	public void scanConfig(Class<?> configClass) {
		if(AnnHandlerUtil.isAnnotationPresent(configClass,EnableFeignClients.class)) {
			//
			log.info("处理--EnableFeignClients==");
			try {
				AnnotationMetadata metadata = AnnHandlerUtil.getInstance().getAnnotationMetadata(configClass);
				fcSupper.registerBeanDefinitions(metadata, LazyApplicationContext.getInstance());
				
				
				BeanInitModel tmp = LazyBean.findCreateBeanFromFactory(FeignContext.class);
				ApplicationContextAware fcAca = (ApplicationContextAware) tmp.getObj();
				fcAca.setApplicationContext(LazyApplicationContext.getInstance());
				
				tmp = LazyBean.findCreateBeanFromFactory(LoadBalancerClientFactory.class);
				ApplicationContextAware lbcfAca = (ApplicationContextAware) tmp.getObj();
				lbcfAca.setApplicationContext(LazyApplicationContext.getInstance());
				
//				LoadBalancerClientFactoryExt extBean = new LoadBalancerClientFactoryExt();
//				LazyListableBeanFactory.getInstance().registerSingleton("loadBalancerClientFactory", extBean);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getBeanKey() {
		return BeanName;
	}
	
	public BeanDefinition finded(BeanModel beanModel) {
		
		return fcSupper.getCandidateComponents().stream().filter(item->{
			String className = item.getBeanClassName();
			Class<?> c = JunitClassLoader.getInstance().junitloadClass(className);
			if(c!=null && beanModel.getTagClass().isAssignableFrom(c)) {
				return true;
			}
			return false;
		}).findFirst().orElse(null);
	}

	public Object buildBean(BeanDefinition candidateComponent) {
		if (candidateComponent instanceof AnnotatedBeanDefinition) {
			// verify annotated class is an interface
			AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
			AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
			Assert.isTrue(annotationMetadata.isInterface(), "@FeignClient can only be specified on an interface");

			Map<String, Object> attributes = annotationMetadata
					.getAnnotationAttributes(FeignClient.class.getCanonicalName());

			String name = fcSupper.getClientName(attributes);
			fcSupper.registerClientConfiguration(LazyApplicationContext.getInstance(), name, attributes.get("configuration"));

			return fcSupper.registerFeignClient(LazyListableBeanFactory.getInstance(), annotationMetadata, attributes);
		}
		return null;
	}

}
