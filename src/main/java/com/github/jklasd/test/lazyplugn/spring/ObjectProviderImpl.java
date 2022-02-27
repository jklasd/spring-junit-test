package com.github.jklasd.test.lazyplugn.spring;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.OrderComparator;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.scan.BeanCreaterScan;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.BeanNameUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectProviderImpl<T> implements ObjectProvider<T>, Serializable{
	/**
     *
     */
    private static final long serialVersionUID = 3004906006576222643L;
    private Type type;
	public ObjectProviderImpl(Class<T> type) {
		this.type = type;
	}

	@Override
	public T getObject() throws BeansException {
		return null;
	}

	@Override
	public T getObject(Object... args) throws BeansException {
		return null;
	}

	@Override
	public T getIfAvailable() throws BeansException {
		if(type != null) {
			Class<?> tagC = (Class<?>) type;
			try {
				Object obj = LazyBean.findCreateBeanFromFactory(tagC, null);
				if(obj != null) {
					return (T) obj;
				}
				
				Class<?> builderC = Class.forName(tagC.getName()+"$Builder");
				Method[] ms = builderC.getDeclaredMethods();
				for(Method m : ms) {
					if(m.getReturnType() == tagC) {
						return (T) m.invoke(builderC.newInstance());
					}
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("ObjectProvider#getIfAvailable##############{}##############",type);
			}
		}
		return null;
	}

	@Override
	public T getIfUnique() throws BeansException {
		return null;
	}
	
	private LazyApplicationContext applicationContext = TestUtil.getInstance().getApplicationContext();
	private LazyListableBeanFactory lazyBeanFactory = (LazyListableBeanFactory) applicationContext.getDefaultListableBeanFactory();
	private BeanCreaterScan beanCreaterScan = BeanCreaterScan.getInstance();
	/**
	 * Spring boot 2.3.1
	 * @return
	 */
	public Stream<T> orderedStream() {//临时处理
		Class<?> tagC = (Class<?>) type;
		if(tagC.getName().startsWith("org.springframework.boot")) {
			Map<String, T> matchingBeans = new LinkedHashMap<>();
			
			Object[] c_m = beanCreaterScan.findCreateBeanFactoryClass(tagC);
			if(c_m[0] == null) {
				
			}else {
				String name = BeanNameUtil.getBeanName(tagC);
				matchingBeans.put(name==null?tagC.getName():name,getIfAvailable());
			}
			
			Stream<T> stream = matchingBeans.values().stream();
			if(matchingBeans.isEmpty()) {
				return stream;
			}
			Comparator<Object> comparator = adaptOrderComparator(matchingBeans);
			if(comparator!=null) {
				return stream.sorted(comparator);
			}else {
				return stream;
			}
		}else {
			return null;
		}
	}

	private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
		Comparator<Object> dependencyComparator = lazyBeanFactory.getDependencyComparator();
		OrderComparator comparator = (dependencyComparator instanceof OrderComparator ?
				(OrderComparator) dependencyComparator : OrderComparator.INSTANCE);
		try {
			return comparator.withSourceProvider(lazyBeanFactory.createFactoryAwareOrderSourceProviderExt(matchingBeans));
		} catch (Exception e) {
			return null;
		}
	}
	
}
