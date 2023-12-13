package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;

import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.base.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyLocalInterfaceBean extends LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{
	
	private ThreadLocal<List<Class<?>>> localCache = new ThreadLocal<>();
	
	@Override
	public String getName() {
		return "LazyLocalInterfaceBean";
	}
	@Override
	public boolean isInterfaceBean() {
		return true;
	}
	
	@Override
	public Integer getOrder() {
		return 300;
	}
	
	
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
			String beanName = model.getBeanName();
			if(localCache.get().size()==1 || StringUtils.isBlank(beanName)) {
				Class<?> tagClass = localCache.get().get(0);
				Object obj = beanFactory.getBean(tagClass);
				return obj;
			}else {
				//判断beanName一致
				Class<?> tagClass = localCache.get().stream().filter(classItem->{
					String cBeanName = BeanNameUtil.getBeanName(classItem);
					return Objects.equal(cBeanName, beanName);
				}).findFirst().orElse(null);
				if(tagClass == null) {
					tagClass = localCache.get().stream().filter(classItem->AnnHandlerUtil.isAnnotationPresent(classItem, Primary.class))
							.findFirst().orElse(null);
				}
				if(tagClass != null) {
					Object obj = beanFactory.getBean(tagClass);
					return obj;
				}
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
		Class<?> tagC = beanModel.getTagClass(); 
		if(tagC.isInterface() && !Collection.class.isAssignableFrom(tagC)) {
			List<Class<?>> impleList = ScanUtil.findClassImplInterface(beanModel.getTagClass());
			if(!impleList.isEmpty()) {
				localCache.set(impleList);
				return true;
			}else {
				//从beanFactory找
				impleList = beanFactory.findBeanDefiByClass(beanModel.getTagClass());
				if(!impleList.isEmpty()) {
					localCache.set(impleList);
					return true;
				}
			}
			
		}
		localCache.remove();
		return false;
	}

	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyLocalInterfaceBean.class);
	}


}
