package com.github.jklasd.test.lazyplugn.spring;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyListableBeanFactory extends DefaultListableBeanFactory {
	public final static int XML_TYPE = 1;
	public final static int ANN_TYPE = 2;
	private int type;

	public LazyListableBeanFactory(int type) {
		super();
		this.type = type;
	}

//    public LazyListableBeanFactory(BeanFactory arg0) {
//        super(arg0);
//    }
	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
//         super.registerBeanDefinition(beanName, beanDefinition);
		if (type == XML_TYPE) {
			registerXmlBean(beanName, beanDefinition);
		} else {
			registerAnnBean(beanName, beanDefinition);
		}
	}

	private void registerAnnBean(String beanName, BeanDefinition beanDefinition) {
		log.debug("registerAnnBean registerBeanDefinition===={}", beanName);
		super.registerBeanDefinition(beanName, beanDefinition);
	}

	private void registerXmlBean(String beanName, BeanDefinition beanDefinition) {
		log.debug("registerXmlBean registerBeanDefinition===={}", beanName);
		if (!TestUtil.getInstance().getApplicationContext().containsBean(beanName)) {
			Class<?> tagC = ScanUtil.loadClass(beanDefinition.getBeanClassName());
			BeanModel beanModel = new BeanModel();
			beanModel.setBeanName(beanName);
			beanModel.setPropValue(beanDefinition.getPropertyValues());
			beanModel.setConstructorArgValue(beanDefinition.getConstructorArgumentValues());
			beanModel.setTagClass(tagC);
			beanModel.setXmlBean(true);
			Object newBean = LazyBean.getInstance().buildProxy(beanModel);
			TestUtil.getInstance().getApplicationContext().registBean(beanName, newBean, tagC);
		}
	}
}
