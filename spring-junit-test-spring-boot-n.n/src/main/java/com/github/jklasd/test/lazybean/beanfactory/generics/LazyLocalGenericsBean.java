package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.ObjectProvider;

import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyLocalGenericsBean extends LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{
	
	private ThreadLocal<String[]> localCache = new ThreadLocal<>();
	
	@Override
	public String getName() {
		return "LazyLocalGenericsBean";
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
			
			String[] beanNames = localCache.get();
			for(String beanName : beanNames) {
				Object tmpObj = beanFactory.getBean(beanName);
				BeanInitModel initModel = new BeanInitModel();
				initModel.setObj(tmpObj);
				initModel.setTagClass(tmpObj.getClass());
				initModel.setBeanName(beanName);
	    		LazyBean.getInstance().processAttr(initModel);// 递归注入代理对象
			}
		}
		localCache.remove();
	}
	
	@Override
	public Object buildBean(BeanModel model) {
		if(localCache.get()!=null) {
			/**
			 * 开始创建对象
			 */
			String[] beanNames = localCache.get();
			List<Object> tmpList = Lists.newArrayList();
			for(String beanName : beanNames) {
				Object tmpObj = beanFactory.getBean(beanName);
				tmpList.add(tmpObj);
			}
			TypeConverter typeConverter = beanFactory.getTypeConverter();
			return typeConverter.convertIfNecessary(tmpList, model.getTagClass());
		}
		return null;
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		
		/**
		 * 如果在本地找到实现类
		 */
		Class<?> tagC = beanModel.getTagClass(); 
		if(tagC.isInterface() && Collection.class.isAssignableFrom(tagC) && !ObjectProvider.class.isAssignableFrom(tagC)) {
			
			Type tmpType = beanModel.getClassGeneric()[0];
			if(tmpType instanceof ParameterizedType) {
				return false;
			}
			Class<?> itemC = (Class<?>) beanModel.getClassGeneric()[0];
			BeanModel asse = new BeanModel();
			asse.setTagClass(itemC);
			List<BeanInitModel> models = LazyBean.findModelsFromFactory(asse);
			String[] beanNames = models.stream().filter(item->!Objects.equals(beanModel.getExcludeBean(), item.getBeanName())).map(item->item.getBeanName()).collect(Collectors.toList()).toArray(new String[0]);
			localCache.set(beanNames);
			return true;
		}
		localCache.remove();
		return false;
	}

	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyLocalGenericsBean.class);
	}


}
