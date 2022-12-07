package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyLocalGenericsBean extends LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{
	
	private ThreadLocal<List<Class<?>>> localCache = new ThreadLocal<>();
	
	LazyListableBeanFactory beanFactory = LazyListableBeanFactory.getInstance();

	@Override
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
	public Object buildBean(BeanModel model) {
		if(localCache.get()!=null) {
			/**
			 * 开始创建对象
			 */
			if(localCache.get().size()==1) {
				String beanName = model.getBeanName();
				if(StringUtils.isBlank(beanName)) {
					beanName = model.getFieldName();
				}
				if(!beanFactory.containsBeanDefinition(beanName)) {//这里beanName 不能为空。临时使用fieldName替代
					Class<?> tagClass = localCache.get().get(0);
					//修改beanName
					Object obj = beanFactory.getBean(tagClass);
					return obj;
				}
				BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
				if(beanDef instanceof AbstractBeanDefinition) {
					return beanFactory.getBean(beanName);
				}
			}else {
				log.warn("活的多个bean;{}",model.getTagClass());
			}
		}
		return null;
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		
		/**
		 * 如果在本地找到实现类
		 */
		if(beanModel.getTagClass().isInterface()) {
			List<Class<?>> impleList = ScanUtil.findClassImplInterface(beanModel.getTagClass());
			if(!impleList.isEmpty()) {
				localCache.set(impleList);
				return true;
			}
		}
		localCache.remove();
		return false;
	}

	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyLocalGenericsBean.class);
	}


}
