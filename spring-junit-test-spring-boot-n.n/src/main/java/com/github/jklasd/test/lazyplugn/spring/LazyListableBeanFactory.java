package com.github.jklasd.test.lazyplugn.spring;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MethodOverrides;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.OrderComparator;
import org.springframework.core.OrderComparator.OrderSourceProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.abstrac.JunitListableBeanFactory;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.scan.BeanCreaterScan;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.core.facade.scan.ConfigurationScan;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.google.common.base.Objects;
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
	
	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {
		return super.getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}
	
	private Map<String,BeanInitModel> findBeanByMethodSets = Maps.newConcurrentMap();
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		if (type != null) {
			if(cacheClassForName.containsKey(type)) {
				return new String[] {cacheClassForName.get(type)};
			}
			try {
				//查一次
				BeanModel asse = new BeanModel();
				asse.setTagClass(type);
				asse.setIncludeNonSingletons(includeNonSingletons);
				List<BeanInitModel> models = LazyBean.findModelsFromFactory(asse);
				if(models!=null && !models.isEmpty()) {
					return models.stream().map(item->{
						findBeanByMethodSets.put(item.getBeanName(),item);
						return item.getBeanName();
					}).collect(Collectors.toList()).toArray(new String[0]);
				}
			} catch (Exception e) {
				log.warn("getBeanNamesForType:"+type,e);
				throw new JunitException("getBeanNamesForType:"+type, true);
			}
		}
		return super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}
	@Override
	protected <T> T doGetBean(String name, Class<T> requiredType, Object[] args, boolean typeCheckOnly)
			throws BeansException {
		
		if(beanDefMap.containsKey(name)) {
			return (T) beanDefMap.get(name);
		}else {
			if(findBeanByMethodSets.containsKey(name)) {
				return (T) LazyBean.findCreateBeanFromFactory(requiredType, name);
			}else {
				log.debug ("未找到:{}",name);
			}
		}
		
		return super.doGetBean(name, requiredType, args, typeCheckOnly);
	}
	/**
	 * 父级调用，防止有插件，自定义构造器
	 */
	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		Class<?> resolved = type.resolve();
		if (resolved != null && !type.hasGenerics()) {
			
			String[] beanNames = getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
			return beanNames;
		}
		else {
			return super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		}
	}
	protected boolean isPrimary(String beanName, Object beanInstance) {
		if(findBeanByMethodSets.containsKey(beanName)) {
			Primary pri = findBeanByMethodSets.get(beanName).getJmd().getMethod().getAnnotation(Primary.class);
			return pri!=null;
		}
		return super.isPrimary(beanName, beanInstance);
	}
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		if(cacheBeanMap.containsKey(name)){
			return cacheBeanMap.get(name).getClass();
		}
		if(beanDefMap.containsKey(name)) {
			BeanDefinition beanDef = beanDefMap.get(name);
			return ScanUtil.loadClass(beanDef.getBeanClassName());
		}
		if(findBeanByMethodSets.containsKey(name)) {
			return findBeanByMethodSets.get(name).getJmd().getReturnType();
		}
		return super.getType(name);
	}
	Map<Class<?>,String> cacheClassForName = Maps.newConcurrentMap();
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
				log.debug("matchBean=>{}",matchBean);
				String beanDefName = findMatchOne(requiredType, matchBean);
 				Object obj = doCreateBean(beanDefName, RootBeanDefinitionBuilder.build(beanDefMap.get(beanDefName)), null);
 				cacheClassForName.put(requiredType, beanDefName);
 				cacheClassMap.put(requiredType, obj);
 				return (T) obj;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//存在jdbcTemplate、 mongoTemplate等bean需要获取
//		else if(requiredType.getName().startsWith("org.springframework")) {
//			return null;
//		}
		try {
			return super.getBean(requiredType);
		} catch (NoSuchBeanDefinitionException e) {
			BeanInitModel model = LazyBean.findCreateBeanFromFactory(requiredType);
			if(model!=null) {
				cacheClassForName.put(requiredType, model.getBeanName());
				cacheClassMap.put(requiredType, model.getObj());
				return (T) model.getObj();
			}
			return null;
		}
	}

	public <T> String findMatchOne(Class<T> requiredType, List<String> matchBean) {
		if(matchBean.size() == 1) {
			return matchBean.get(0);
		}
		return matchBean.stream().filter(beanName -> {
			String className = beanDefMap.get(beanName).getBeanClassName();
			Class<?> c = ScanUtil.findClassByName(className);
			if(c!=null && c.isAssignableFrom(requiredType)) {
				return true;
			}
			return false;
		}).findFirst().orElse(matchBean.get(0));
	}
	
//	Map<String,Object> cacheProxyBean = Maps.newHashMap();
	
	public Object getBean(String beanName) {
		if(findBeanByMethodSets.containsKey(beanName)) {
			BeanInitModel bim = findBeanByMethodSets.get(beanName);
			BeanModel assemblyData = new BeanModel();
			assemblyData.setTagClass(bim.getJmd().getReturnType());
			assemblyData.setBeanName(bim.getBeanName());
			return JavaBeanUtil.getInstance().buildBean(bim.getJmd().getConfigurationClass(),bim.getJmd().getMethod(),assemblyData);
		}
		if(cacheBeanMap.get(beanName)!=null) {
			return cacheBeanMap.get(beanName);
		}
		if(beanDefMap.containsKey(beanName)) {
			return doCreateBean(beanName, RootBeanDefinitionBuilder.build(beanDefMap.get(beanName)), null);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String beanName, Class<T> requiredType) throws BeansException {
		if(super.containsBean(beanName)) {
			return super.getBean(beanName,requiredType);
		}
		if(cacheBeanMap.containsKey(beanName)) {
			return (T) cacheBeanMap.get(beanName);
		}
		if(beanDefMap.containsKey(beanName)) {
			RootBeanDefinition beanDef = RootBeanDefinitionBuilder.build(beanDefMap.get(beanName));
			Class<?> tagClass = ScanUtil.loadClass(beanDef.getBeanClassName());
			if(requiredType.isAssignableFrom(tagClass)) {
				return (T) doCreateBean(beanName, beanDef, null);
			}
		}
		return null;
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
		if(cacheBeanMap.containsKey(name)) {
			return true;
		}
		return super.containsBean(name);
	}
	
	Set<String> beanNameSet = Sets.newHashSet();
	
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		if(cacheBeanMap.containsKey(beanName)) {
			throw new JunitException(beanName+"单例Bean注册失败", true);
		}
		beanNameSet.add(beanName);
		if(LazyProxyManager.isProxy(singletonObject)) {
//			cacheProxyBean.put(beanName, singletonObject);
			throw new JunitException("不应该执行",true);
		}
		cacheBeanMap.put(beanName, singletonObject);
		cacheClassMap.put(singletonObject.getClass(), singletonObject);
		cacheClassForName.put(singletonObject.getClass(), beanName);
		super.registerSingleton(beanName, singletonObject);
	}

//	private void registerAnnBean(String beanName, BeanDefinition beanDefinition) {
//		log.debug("registerAnnBean registerBeanDefinition===={}", beanName);
//		super.registerBeanDefinition(beanName, beanDefinition);
//	}
	
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
		Object obj = super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
//		LazyBean.getInstance().processAttr(obj, obj.getClass());
		BeanInitModel initModel = new BeanInitModel();
		initModel.setObj(obj);
		initModel.setTagClass(obj.getClass());
		initModel.setBeanName(beanName);
		LazyBean.getInstance().processAttr(initModel);// 递归注入代理对象
		return obj;
	}
	
//	protected void resetBeanDefinition(String beanName) {
//		super.resetBeanDefinition(beanName);
//	}
	
	@Override
	public String[] getBeanNamesForType(Class<?> type) {
		return matchName(type, true, true).toArray(new String[0]);
	}

//	public String[] getBeanNamesForTypedStream(Type requiredType) {
//		return matchName((Class<?>) requiredType, true, true).toArray(new String[0]);
////		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, ResolvableType.forRawClass((Class<?>) requiredType));
//	}
	
	public OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProviderExt(Map<String, ?> beans) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method createFactoryAwareOrderSourceProvider = DefaultListableBeanFactory.class.getDeclaredMethod("createFactoryAwareOrderSourceProvider", Map.class);
		if(createFactoryAwareOrderSourceProvider.isAccessible()) {
			createFactoryAwareOrderSourceProvider.setAccessible(true);
		}
		return (OrderSourceProvider) createFactoryAwareOrderSourceProvider.invoke(this,beans);
	}

//	@Override
//	public void register() {
//		ContainerManager.registComponent( this);
//	}
//	
//	@Override
//	public String getBeanKey() {
//		return JunitListableBeanFactory.class.getSimpleName();
//	}

	protected void releaseBean(Class<?> tagC, Object tmp) {
		cacheBeanMap.entrySet().stream().filter(entry->entry.getValue()==tmp).forEach(entry->{
			BeanModel model = new BeanModel();
			model.setBeanName(entry.getKey());
			model.setTagClass(tagC);
			entry.setValue(LazyBean.createLazyCglib(model));//重新构建一个
		});
		
		cacheClassMap.remove(tagC);
	}
	
	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		if(cacheBeanMap.containsKey(name)) {
			return true;
		}
		return super.isSingleton(name);
	}
	
	List<BeanDefinition> beanDefList = Lists.newArrayList();
	Map<String,BeanDefinition> beanDefMap = Maps.newHashMap();
	List<String> beanNames = Lists.newArrayList();
	Set<String> beanNameDefSet = Sets.newHashSet();
	
	
	BeanCreaterScan beanCreaterScan = BeanCreaterScan.getInstance();
	@Override
	public synchronized void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
		
		AbstractBeanDefinition tmpDef =  (AbstractBeanDefinition) beanDefinition;
		if(!tmpDef.hasBeanClass()) {//未转化，先转化
			String beanClassName = tmpDef.getBeanClassName();
			Class<?> tagC = ScanUtil.loadClass(beanClassName);
			tmpDef.setBeanClass(tagC);
		}
		if(tmpDef.getBeanClass().isAnnotationPresent(Configuration.class)) {
			//configuration
			log.debug("处理{}",tmpDef.getBeanClass());
			beanCreaterScan.loadConfigurationClass(tmpDef.getBeanClass());
			try {
				ConfigurationScan.getInstance().scanConfigClass(tmpDef.getBeanClass());
			} catch (IOException e) {
				log.error("AnnotationResourceLoader#scanConfigClass",e);
			}
		}
		//xml,ann
		ClassScan.getInstance().loadComponentClass(tmpDef.getBeanClass());
		if(beanDefMap.containsKey(beanName)) {
			if(!Objects.equal(beanDefMap.get(beanName), beanDefinition)) {
				throw new JunitException("bean "+beanName +"注冊重复", true);
			}
			return;
		}
		
		beanDefList.add(beanDefinition);
		beanDefMap.put(beanName, beanDefinition);
		beanNames.add(beanName);
	}
	
	@Override
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
			throws BeanDefinitionStoreException {
		return super.getMergedBeanDefinition(beanName, bd);
	}
		
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
    	return beanDefMap.get(beanName);
	}
	
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		Assert.hasText(beanName, "'beanName' must not be empty");

		BeanDefinition bd = beanDefMap.remove(beanName);
		beanNames.remove(beanName);
		beanDefList.remove(bd);
	}
	
	/**
	 * 通过class 匹配 beanName
	 * @param tagClass 目标类
	 * @param includeNonSingletons 是否导入非单例对象
	 * @param allowEagerInit 允许初始化
	 * @return 返回匹配好的所有beanName
	 */
	public List<String> matchName(Class<?> tagClass,boolean includeNonSingletons, boolean allowEagerInit) {
		List<String> result = Lists.newArrayList();
		ResolvableType type = ResolvableType.forRawClass(tagClass);
		for(String beanName : beanNames) {
			try {
				BeanDefinition mbd = beanDefMap.get(beanName);
				// Only check bean definition if it is complete.
				if (!mbd.isAbstract() && (allowEagerInit ||
						( !mbd.isLazyInit() || isAllowEagerClassLoading()))) {// && !requiresEagerInitForType(mbd.getFactoryBeanName())mbd.hasBeanClass() ||
					if(isTypeMatch(beanName, type, false)) {
						result.add(beanName);
					}else {
						List<Class<?>> fined = ClassScan.getInstance().findByFactory(tagClass);
						if(fined!=null) {
							for(Class<?> tC : fined) {
								ResolvableType factoryObjType = ResolvableType.forRawClass(tC);
								if(isTypeMatch(beanName, factoryObjType, false)) {
									result.add(beanName);
								}
							}
						}
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
		
		beanNameSet.stream().forEach(singleBeanName->{
			if(tagClass.isAssignableFrom(cacheBeanMap.get(singleBeanName).getClass())) {
				result.add(singleBeanName);
			}
		});
		
		return result;
	}
	
	public boolean containsBeanDefinition(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return beanDefMap.containsKey(beanName);
	}
	
	protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		BeanDefinition beanDef = beanDefMap.get(beanName);
		if(beanDef != null) {
			if(beanDef instanceof AbstractBeanDefinition) {
				AbstractBeanDefinition tmpDef = (AbstractBeanDefinition) beanDef;
				if(FactoryBean.class.isAssignableFrom(tmpDef.getBeanClass())) {
					boolean match = typeToMatch.getRawClass().isAssignableFrom(tmpDef.getBeanClass());
					return match;
				}else {
					boolean match = typeToMatch.getRawClass().isAssignableFrom(tmpDef.getBeanClass());
					return match;
				}
			}
		}
		return false;
	}
	
	Map<String,Object> cacheBeanMap = Maps.newHashMap();
	
	@Override
	public Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
		log.debug("============================构建真实对象={}===========================",beanName);
		if(cacheBeanMap.containsKey(beanName)) {
			return cacheBeanMap.get(beanName);
		}
		Object obj = onlyCreateBean(beanName, mbd, args);
		if(obj instanceof InitializingBean) {
			InitializingBean tmp = (InitializingBean) obj;
			try {
				tmp.afterPropertiesSet();
			} catch (Exception e) {
				log.error("InitializingBean#afterPropertiesSet",e);
			}
		}
		if(obj instanceof FactoryBean) {
			FactoryBean<?> tmp = (FactoryBean<?>) obj;
			try {
				obj = tmp.getObject();
			} catch (Exception e) {
				log.error("FactoryBean#getObject",e);
			}
		}
		if(!obj.getClass().isInterface()) {
			BeanInitModel initModel = new BeanInitModel();
			initModel.setObj(obj);
			initModel.setTagClass(obj.getClass());
			initModel.setBeanName(beanName);
    		LazyBean.getInstance().processAttr(initModel);// 递归注入代理对象
		}
		cacheBeanMap.put(beanName, obj);
		return obj;
	}
	
	public Object onlyCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
		//处理一次properties
		XmlBeanUtil.getInstance().postProcessMutablePropertyValues(mbd.getPropertyValues());
		
		Constructor<?>[] construcs = mbd.getBeanClass().getConstructors();
		if(construcs.length == 1 && construcs[0].getParameterCount()>0) {//处理带参构造函数
			if(mbd.getConstructorArgumentValues().getArgumentCount()<1) {//存在beanDefinition自带构造参数
				args = JavaBeanUtil.getInstance().buildParam(construcs[0].getParameterTypes(), construcs[0].getParameterAnnotations());
			}
		}
		
		BeanWrapper bw = createBeanInstance(beanName, mbd, args);
		populateBean(beanName, mbd, bw);
//		applyBeanPropertyValues(bw, beanName);
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

//		public static Object[] getArgs(RootBeanDefinition rbd) {
//			if(rbd.getConstructorArgumentValues().isEmpty()) {
//				return null;
//			}
//			ConstructorArgumentValues cav = rbd.getConstructorArgumentValues();
//			Object[] args = new Object[cav.getArgumentCount()];
//			for(int i=0;i<args.length;i++) {
//				args[i] = cav.getGenericArgumentValues().get(i).getValue();
//			}
//			return args;
//		}
		
	}

	public BeanDefinition getFirstBeanDefinition(Class<?> tagClass) {
		List<String> matchBean = matchName(tagClass, isCacheBeanMetadata(), false);
		if(!matchBean.isEmpty()) {
			log.info("matchBean=>{}",matchBean);
			String beanDefName = findMatchOne(tagClass, matchBean);
			return beanDefMap.get(beanDefName);
		}
		return null;
	}

	public List<Class<?>> findBeanDefiByClass(Class<?> tagClass) {
		List<String> matchBean = matchName(tagClass, isCacheBeanMetadata(), false);
		if(!matchBean.isEmpty()) {
			List<Class<?>> tagClasses = matchBean.stream().map(beanName->{
				try {
					return JunitClassLoader.getInstance().loadClass(beanDefMap.get(beanName).getBeanClassName());
				} catch (ClassNotFoundException e) {
					return null;
				}
			}).filter(item->item!=null).collect(Collectors.toList());
			return tagClasses;
		}
		return Lists.newArrayList();
	}

}
