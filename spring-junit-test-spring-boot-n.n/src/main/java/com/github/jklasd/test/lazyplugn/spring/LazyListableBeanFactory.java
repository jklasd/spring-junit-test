package com.github.jklasd.test.lazyplugn.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.MethodOverrides;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.OrderComparator;
import org.springframework.core.OrderComparator.OrderSourceProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.abstrac.JunitListableBeanFactory;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyListableBeanFactory extends JunitListableBeanFactory {
	protected LazyListableBeanFactory() {}
	private static LazyListableBeanFactory beanFactory = new LazyListableBeanFactory();
	public static LazyListableBeanFactory getInstance() {
		return beanFactory;
	}

//    public LazyListableBeanFactory(BeanFactory arg0) {
//        super(arg0);
//    }
	
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		if(cacheProxyBean.containsKey(name)){
			return LazyProxyManager.getProxyTagClass(cacheProxyBean.get(name));
		}
		return super.getType(name);
	}
	/**
	 * TODO 待优化
	 */
	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		Map<String, T> result = super.getBeansOfType(type);
		
		//获取名字
		String[] beanNameArr = StringUtils.toStringArray(beanNameSet);
		for(String beanName:beanNameArr) {
			Object bean = getBean(beanName);
			if(type.isAssignableFrom(LazyProxyManager.getProxyTagClass(bean))) {
				result.put(beanName, (T) bean);
			}
		}
		
		if(result.isEmpty()) {
			//获取子类
			List<Class<?>> subClass = ScanUtil.findClassExtendAbstract(type);
			subClass.forEach(subC ->{
				Object bean = LazyBean.getInstance().buildProxy(subC);
				result.put(BeanNameUtil.getBeanName(subC), (T) bean);
			});
		}
		
		return result;
	}
	
	Map<Class<?>,Object> cacheClassMap = Maps.newConcurrentMap();
	public void registerResolvableDependency(Class<?> dependencyType, Object autowiredValue) {
		Assert.notNull(dependencyType, "Dependency type must not be null");
		if(autowiredValue!=null) {
			cacheClassMap.putIfAbsent(dependencyType, autowiredValue);
			if(LazyProxyManager.isProxy(autowiredValue)) {//代理bean不注册到spring容器中
				return;
			}
		}
		super.registerResolvableDependency(dependencyType, autowiredValue);
	}
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		if(cacheClassMap.containsKey(requiredType)) {
			return (T) cacheClassMap.get(requiredType);
		}else {
			List<T> list = Lists.newArrayList();
			cacheClassMap.entrySet().forEach(entry->{
				// 父类 isAssignableFrom 子类
				if(requiredType.isAssignableFrom(entry.getKey())) {
					list.add((T) entry.getValue());
				}
			});
			if(list.size()>1) {
				log.warn("{}=>找到两个对象",requiredType);
				return list.get(0);
			}
		}
		try {
			List<String> matchBean = matchName(requiredType, isCacheBeanMetadata(), false);
			if(!matchBean.isEmpty()) {
				log.info("matchBean=>{}",matchBean);
 				return (T) doCreateBean(matchBean.get(0), RootBeanDefinitionBuilder.build(beanDefMap.get(matchBean.get(0))), null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//存在jdbcTemplate、 mongoTemplate等bean需要获取
//		else if(requiredType.getName().startsWith("org.springframework")) {
//			return null;
//		}
		return super.getBean(requiredType);
	}
	
	Map<String,Object> cacheProxyBean = Maps.newHashMap();
	
	public Object getBean(String beanName) {
		if(super.containsBean(beanName) || !cacheProxyBean.containsKey(beanName)) {
			return super.getBean(beanName);
		}
		return cacheProxyBean.get(beanName);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String beanName, Class<T> requiredType) throws BeansException {
		if(super.containsBean(beanName) || !cacheProxyBean.containsKey(beanName)) {
			return super.getBean(beanName,requiredType);
		}
		return (T) cacheProxyBean.get(beanName);
	}
//	public <T> T getBean(Class<T> requiredType) throws BeansException {
//		if(cacheClassMap.containsKey(requiredType)) {
//			return (T) cacheClassMap.get(requiredType);
//		}
//		Object obj = super.getBean(requiredType);
//		if(obj == null) {
//			obj = LazyBean.getInstance().findBean(requiredType); 
//		}
//		return (T) obj;
//	}
	
	public boolean containsBean(String name) {
		if(cacheProxyBean.containsKey(name)) {
			return true;
		}
		return super.containsBean(name);
	}
	
	Set<String> beanNameSet = Sets.newHashSet();
	
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		beanNameSet.add(beanName);
		if(LazyProxyManager.isProxy(singletonObject)) {
			cacheProxyBean.put(beanName, singletonObject);
			return;
		}
		super.registerSingleton(beanName, singletonObject);
	}

//	private void registerAnnBean(String beanName, BeanDefinition beanDefinition) {
//		log.debug("registerAnnBean registerBeanDefinition===={}", beanName);
//		super.registerBeanDefinition(beanName, beanDefinition);
//	}
	
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
		Object obj = super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
		LazyBean.getInstance().processAttr(obj, obj.getClass());
		return obj;
	}
	
//	protected void resetBeanDefinition(String beanName) {
//		super.resetBeanDefinition(beanName);
//	}

	public String[] getBeanNamesForTypedStream(Type requiredType) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, ResolvableType.forRawClass((Class<?>) requiredType));
	}
	
	public OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProviderExt(Map<String, ?> beans) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method createFactoryAwareOrderSourceProvider = DefaultListableBeanFactory.class.getDeclaredMethod("createFactoryAwareOrderSourceProvider", Map.class);
		if(createFactoryAwareOrderSourceProvider.isAccessible()) {
			createFactoryAwareOrderSourceProvider.setAccessible(true);
		}
		return (OrderSourceProvider) createFactoryAwareOrderSourceProvider.invoke(this,beans);
	}

	@Override
	public void register() {
		ContainerManager.registComponent( this);
	}
	
	@Override
	public String getBeanKey() {
		return JunitListableBeanFactory.class.getSimpleName();
	}

	protected void releaseBean(Class<?> tagC, Object tmp) {
		cacheProxyBean.entrySet().stream().filter(entry->entry.getValue()==tmp).forEach(entry->{
			BeanModel model = new BeanModel();
			model.setBeanName(entry.getKey());
			model.setTagClass(tagC);
			entry.setValue(LazyBean.createLazyCglib(model));//重新构建一个
		});
		
		cacheClassMap.remove(tagC);
	}
	
	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		if(cacheProxyBean.containsKey(name)) {
			return true;
		}
		return super.isSingleton(name);
	}
	
	List<BeanDefinition> beanDefList = Lists.newArrayList();
	Map<String,BeanDefinition> beanDefMap = Maps.newHashMap();
	List<String> beanNames = Lists.newArrayList();
	Set<String> beanNameDefSet = Sets.newHashSet();
	@Override
	public synchronized void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
		beanDefList.add(beanDefinition);
		beanDefMap.put(beanName, beanDefinition);
		beanNames.add(beanName);
	}
	
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
    	if(beanName.startsWith("org.springframework")) {
    		return beanDefMap.get(beanName);
    	}
    	return null;
	}
	
	
	/**
	 * 通过class 匹配 beanName
	 * @param tagClass
	 * @param includeNonSingletons
	 * @param allowEagerInit
	 * @return
	 */
	public List<String> matchName(Class<?> tagClass,boolean includeNonSingletons, boolean allowEagerInit) {
		List<String> result = Lists.newArrayList();
		ResolvableType type = ResolvableType.forRawClass(tagClass);
		for(String beanName : beanNames) {
			try {
//				if(beanName.equalsIgnoreCase("SqlSessionFactory")
//						|| beanName.startsWith("org.apache.ibatis.session")
//						|| beanName.startsWith("org.mybatis.spring")) {
//					log.info("断点1");
//				}
				BeanDefinition mbd = beanDefMap.get(beanName);
				// Only check bean definition if it is complete.
				if (!mbd.isAbstract() && (allowEagerInit ||
						( !mbd.isLazyInit() || isAllowEagerClassLoading()))) {// && !requiresEagerInitForType(mbd.getFactoryBeanName())mbd.hasBeanClass() ||
					if(isTypeMatch(beanName, type, false)) {
						result.add(beanName);
					}
				}
			}
			catch (CannotLoadBeanClassException | BeanDefinitionStoreException ex) {
				if (allowEagerInit) {
					throw ex;
				}
				// Probably a placeholder: let's ignore it for type matching purposes.
				LogMessage message = (ex instanceof CannotLoadBeanClassException ?
						LogMessage.format("Ignoring bean class loading failure for bean '%s'", beanName) :
							LogMessage.format("Ignoring unresolvable metadata in bean definition '%s'", beanName));
				logger.trace(message, ex);
				// Register exception, in case the bean was accidentally unresolvable.
				onSuppressedException(ex);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Bean definition got removed while we were iterating -> ignore.
			}
		}
		return result;
	}
	
	protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);
		BeanDefinition beanDef = beanDefMap.get(beanName);
		if(beanDef != null) {
			if(beanDef instanceof RootBeanDefinition) {
				RootBeanDefinition tmpDef = (RootBeanDefinition)beanDef;
				if(tmpDef.getBeanClass()!=null) {
					boolean match = tmpDef.getBeanClass().isAssignableFrom(typeToMatch.getRawClass());
					if(match) {
						log.info("===匹配===");
					}
					return match;
				}
			}else if (beanDef instanceof ScannedGenericBeanDefinition) {//注解扫描
				ScannedGenericBeanDefinition tmpDef = (ScannedGenericBeanDefinition)beanDef;
				if(tmpDef.hasBeanClass()) {
					boolean match = tmpDef.getBeanClass().isAssignableFrom(typeToMatch.getRawClass());
					if(match) {
						log.info("===匹配===");
					}
					return match;
				}else {
					String beanClassName = tmpDef.getBeanClassName();
					Class<?> tagClass = ScanUtil.loadClass(beanClassName);
					tmpDef.setBeanClass(tagClass);
					boolean match = tmpDef.getBeanClass().isAssignableFrom(typeToMatch.getRawClass());
					if(match) {
						log.info("===匹配===");
					}
					return match;
				}
			}else if(beanDef instanceof GenericBeanDefinition) {//XML BeanDef
				GenericBeanDefinition tmpDef = (GenericBeanDefinition)beanDef;
				if(tmpDef.hasBeanClass()) {
					if(FactoryBean.class.isAssignableFrom(tmpDef.getBeanClass())) {
						Class<?> tagC = (Class<?>)ScanUtil.getGenericType(tmpDef.getBeanClass())[0];
						boolean match = tagC.isAssignableFrom(typeToMatch.getRawClass());
						return match;
					}else {
						boolean match = tmpDef.getBeanClass().isAssignableFrom(typeToMatch.getRawClass());
						if(match) {
							log.info("===匹配===");
						}
						return match;
					}
				}else {
					String beanClassName = tmpDef.getBeanClassName();
					Class<?> tagClass = ScanUtil.loadClass(beanClassName);
					tmpDef.setBeanClass(tagClass);
					if(FactoryBean.class.isAssignableFrom(tmpDef.getBeanClass())) {
						Class<?> tagC = (Class<?>)ScanUtil.getGenericType(tmpDef.getBeanClass())[0];
						boolean match = tagC.isAssignableFrom(typeToMatch.getRawClass());
						return match;
					}
					
					boolean match = tmpDef.getBeanClass().isAssignableFrom(typeToMatch.getRawClass());
					if(match) {
						log.info("===匹配===");
					}
					return match;
				}
			}
			Object source = beanDef.getSource();
			if(source instanceof Class) {
				return ((Class)source).isAssignableFrom(typeToMatch.getRawClass());
			}
			log.info("============================匹配真实对象={}={}==========================",name,typeToMatch.getRawClass());
		}
		return false;
	}
//	
//	protected <T> T doGetBean(
//			String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
//			throws BeansException {
//		if(requiredType!=null && requiredType.getName().startsWith("org.springframework")) {
//			return super.doGetBean(name, requiredType, args, typeCheckOnly);
//		}
//		return null;
//	}
	
	@Override
	protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
		log.info("============================构建真实对象={}===========================",beanName);
		BeanWrapper bw = createBeanInstance(beanName, mbd, args);
		Object obj = bw.getWrappedInstance();
		return obj;
	}
	
	public static class RootBeanDefinitionBuilder{

		public static RootBeanDefinition build(BeanDefinition original) {
			RootBeanDefinition beanDef = new RootBeanDefinition();
			beanDef.setParentName(original.getParentName());
			beanDef.setBeanClassName(original.getBeanClassName());
			beanDef.setScope(original.getScope());
			beanDef.setAbstract(original.isAbstract());
			beanDef.setFactoryBeanName(original.getFactoryBeanName());
			beanDef.setFactoryMethodName(original.getFactoryMethodName());
			beanDef.setRole(original.getRole());
			beanDef.setSource(original.getSource());
			copyAttributesFrom(beanDef,original);

			if (original instanceof AbstractBeanDefinition) {
				AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
				if (originalAbd.hasBeanClass()) {
					beanDef.setBeanClass(originalAbd.getBeanClass());
				}
				if (originalAbd.hasConstructorArgumentValues()) {
					beanDef.setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
				}
				if (originalAbd.hasPropertyValues()) {
					beanDef.setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
				}
				if (originalAbd.hasMethodOverrides()) {
					beanDef.setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
				}
				Boolean lazyInit = originalAbd.getLazyInit();
				if (lazyInit != null) {
					beanDef.setLazyInit(lazyInit);
				}
				beanDef.setAutowireMode(originalAbd.getAutowireMode());
				beanDef.setDependencyCheck(originalAbd.getDependencyCheck());
				beanDef.setDependsOn(originalAbd.getDependsOn());
				beanDef.setAutowireCandidate(originalAbd.isAutowireCandidate());
				beanDef.setPrimary(originalAbd.isPrimary());
				beanDef.copyQualifiersFrom(originalAbd);
				beanDef.setInstanceSupplier(originalAbd.getInstanceSupplier());
				beanDef.setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
				beanDef.setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
				beanDef.setInitMethodName(originalAbd.getInitMethodName());
				beanDef.setEnforceInitMethod(originalAbd.isEnforceInitMethod());
				beanDef.setDestroyMethodName(originalAbd.getDestroyMethodName());
				beanDef.setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
				beanDef.setSynthetic(originalAbd.isSynthetic());
				beanDef.setResource(originalAbd.getResource());
			}
			else {
				beanDef.setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
				beanDef.setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
				beanDef.setLazyInit(original.isLazyInit());
				beanDef.setResourceDescription(original.getResourceDescription());
			}
			return beanDef;
		}
		
		protected static void copyAttributesFrom(AttributeAccessor source, BeanDefinition original) {
			Assert.notNull(source, "Source must not be null");
			String[] attributeNames = source.attributeNames();
			for (String attributeName : attributeNames) {
				original.setAttribute(attributeName, source.getAttribute(attributeName));
			}
		}
		
	}
}
