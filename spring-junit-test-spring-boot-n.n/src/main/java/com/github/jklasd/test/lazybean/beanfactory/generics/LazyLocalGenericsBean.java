package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.util.List;

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
				if(!beanFactory.containsBeanDefinition(model.getBeanName())) {
					Class<?> tagClass = localCache.get().get(0);
					//修改beanName
					Object obj = beanFactory.getBean(tagClass);
					return obj;
				}
				return beanFactory.getBean(model.getBeanName());
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

	static LazyLocalGenericsBean instance;
	public static LazyLocalGenericsBean getInstance() {
		if(instance != null) {
			return instance;
		}
		
		synchronized (LazyLocalGenericsBean.class) {
			if(instance == null) {
				instance = new LazyLocalGenericsBean();
			}
		}
		
		return instance;
	}


}
