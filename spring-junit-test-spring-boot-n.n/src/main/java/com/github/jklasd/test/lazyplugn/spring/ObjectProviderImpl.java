package com.github.jklasd.test.lazyplugn.spring;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.OrderComparator;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.github.jklasd.test.core.facade.scan.BeanCreaterScan;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectProviderImpl<T> implements ObjectProvider<T>, Serializable{
	/**
     *
     */
    private static final long serialVersionUID = 3004906006576222643L;
    private Type type;
    
    T objCache;
    
	public ObjectProviderImpl(Type type) {
		this.type = type;
	}

	@Override
	public T getObject() throws BeansException {
		if(objCache == null) {
			return getIfAvailable();
		}
		return objCache;
	}

	@Override
	public T getObject(Object... args) throws BeansException {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getIfAvailable() throws BeansException {
		if(type != null) {
			if(type instanceof Class) {
				Class<?> tagC = (Class<?>) type;
				try {
					Object obj = LazyBean.findCreateBeanFromFactory(tagC, null);
					if(obj != null) {
						return (T) obj;
					}
					
					Class<?> builderC = JunitClassLoader.getInstance().loadClass(tagC.getName()+"$Builder");
					Method[] ms = builderC.getDeclaredMethods();
					for(Method m : ms) {
						if(m.getReturnType() == tagC) {
							objCache = (T) m.invoke(builderC.newInstance()); 
							return objCache;
						}
					}
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.warn("ObjectProvider#getIfAvailable##############{}##############",type);
				} catch (Exception e) {
					log.warn("ObjectProvider#getIfAvailable##############{}##############",type,e);
				}
			}else {
				
				if(type instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) type;
					Class<?> rtClass = (Class<?>) pt.getRawType();
					if (Collection.class.isAssignableFrom(rtClass) && rtClass.isInterface()) {
						Class<?> itemClass = (Class<?>)pt.getActualTypeArguments()[0];
						List<?> list = LazyBean.findListBeanExt(itemClass);
						TypeConverter typeConverter = lazyBeanFactory.getTypeConverter();
						objCache = (T) typeConverter.convertIfNecessary(list, (Class<?>)pt.getRawType());
						return objCache;
					}else {
						log.warn("其他类型:{}",rtClass);
					}
				}
			}
		}
		return null;
	}

	@Override
	public T getIfUnique() throws BeansException {
		return null;
	}
	
	private LazyApplicationContext applicationContext = LazyApplicationContext.getInstance();
	private LazyListableBeanFactory lazyBeanFactory = (LazyListableBeanFactory) applicationContext.getDefaultListableBeanFactory();
	private BeanCreaterScan beanCreaterScan = BeanCreaterScan.getInstance();
	/**
	 * Spring boot 2.3.1 支持，返回通过tagC扫描的对象集合
	 */
	public Stream<T> orderedStream() {//临时处理
		Class<?> tagC = (Class<?>) type;
		if(tagC.getName().startsWith("org.springframework.boot")) {
			Map<String, T> matchingBeans = new LinkedHashMap<>();
			
			JunitMethodDefinition jmd = beanCreaterScan.findCreateBeanFactoryClass(tagC);
			if(jmd != null) {
//				String name = BeanNameUtil.getBeanName(tagC);
				matchingBeans.put(jmd.getBeanName(),getIfAvailable());
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
			return new ArrayList<T>().stream();
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
