package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.util.BeanNameUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyCollectionBean extends LazyAbstractPlugnBeanFactory{

	@Override
	public String getName() {
		return "LazyCollectionBean";
	}
	@Override
	public boolean isInterfaceBean() {
		return true;
	}
	@Override
	public Integer getOrder() {//
		return super.getOrder();
	}

	@Override
	public Object buildBean(BeanModel model) {
		if(model.getTagClass() == List.class) {
			Type[] item = model.getClassGeneric();
			if(item.length == 1) {
				//处理一个集合注入
				try {
					Class<?> c = JunitClassLoader.getInstance().loadClass(item[0].getTypeName());
					List list = LazyBean.findListBean(c);
					if(list.isEmpty()) {
						String[] beanNames = LazyApplicationContext.getInstance().getBeanNamesForType(c);
						for(String beanName : beanNames) {
							list.add(LazyApplicationContext.getInstance().getBean(beanName));
						}
					}
					return list;
				} catch (ClassNotFoundException e) {
					log.error("ClassNotFoundException",e);
					throw new JunitException("ClassNotFoundException", true);
				}
			}else {
				//TODO 待优化
				log.info("其他特殊情况");
			}
		}
		else if(model.getTagClass() == Map.class) {//暂时就支持一层
			//TODO 待优化
			Type[] item = model.getClassGeneric();
			if(item.length==2) {
				try {
					Class<?> c = JunitClassLoader.getInstance().loadClass(item[1].getTypeName());
					List<Object> list = LazyBean.findListBean(c);
					Map<Object, Object> map = new HashMap<>();
					if(list.isEmpty()) {
						String[] beanNames = LazyApplicationContext.getInstance().getBeanNamesForType(c);
						for(String beanName : beanNames) {
							map.put(beanName, LazyApplicationContext.getInstance().getBean(beanName));
						}
					}else {
						list.forEach(tmpObj->{
							if(LazyProxyManager.isProxy(tmpObj)) {
								String beanName = LazyProxyManager.getProxy(tmpObj).getBeanModel().getBeanName();
								map.put(beanName, tmpObj);
							}else {
								map.put(BeanNameUtil.getBeanNameFormAnno(tmpObj.getClass()), tmpObj);
							}
						});
					}
					return map;
				} catch (ClassNotFoundException e) {
					throw new JunitException("ClassNotFoundException", true);
				}
			}
		}
		return null;
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		return beanModel.getTagClass() == Map.class
				|| beanModel.getTagClass() == List.class;
	}

}
