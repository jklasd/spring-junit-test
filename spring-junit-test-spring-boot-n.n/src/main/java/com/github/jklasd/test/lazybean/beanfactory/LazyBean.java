package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.common.util.SignalNotificationUtil;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.core.facade.scan.PropResourceManager;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.configprop.LazyConfPropBind;
import com.github.jklasd.test.util.BeanNameUtil;
import com.github.jklasd.test.util.DebugObjectView;
import com.github.jklasd.test.util.StackOverCheckUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 * 2020-11-19
 */
@Slf4j
public class LazyBean implements LazyBeanI{
//	public static Map<Class<?>, List<Object>> singleton = Maps.newHashMap();
//	public static Map<String, Object> singletonname = Maps.newHashMap();
	private static ObjenesisStd objenesis = new ObjenesisStd();
//	private TestUtil util = TestUtil.getInstance();
	private LazyBean() {}
	private static LazyBean bean;
	public static LazyBean getInstance() {
	    if(bean==null) {
	    	synchronized(objenesis) {
	    		if(bean==null) {
	    			bean = new LazyBean();
	    		}
	    	}
	    }
	    return bean;
	}
	public static <T> T invokeBuildObject(Class<T> tagClass){
		return objenesis.newInstance(tagClass);
	}
	public Object buildProxyForGeneric(Class<?> classBean,Type[] classGeneric,String excludeBean) {
		Object tagObject = null;
		if (classBean.isInterface()) {
		    BeanModel model = new BeanModel();
		    model.setTagClass(classBean);
		    model.setExcludeBean(excludeBean);
		    model.setClassGeneric(classGeneric);
			tagObject = createBean(model);
		}
		return tagObject;
	}
	/**
	 * 
	 * 构建代理对象 classBean 
	 * @param classBean 代理类型
	 * @param classGeneric 代理类的泛型类型
	 * @return 代理对象
	 */
	public Object buildProxyForGeneric(Class<?> classBean,Type[] classGeneric) {
		Object tagObject = null;
		if (classBean.isInterface()) {
		    BeanModel model = new BeanModel();
		    model.setTagClass(classBean);
		    model.setClassGeneric(classGeneric);
			tagObject = createBean(model);
		}
		return tagObject;
	}
	/**
	 * 
	 * 构建代理对象
	 * @param beanClass 需要代理的类型
	 * @param beanName 对象Name
	 * @return 代理对象
	 */
	public Object buildProxy(Class<?> beanClass,String beanName) {
	    if(beanClass == null)
	        throw new JunitException();
	    
	    BeanModel model = new BeanModel();
	    model.setBeanName(beanName);
	    model.setTagClass(beanClass);
		return buildProxy(model);
	}
	/**
	 * 开始构建对象
	 * @param beanModel 对象配置信息
	 * @return 代理对象
	 */
	public Object buildProxy(final BeanModel beanModel) {
		return StackOverCheckUtil.observeIgnoreException(()->{
			if(StringUtils.isBlank(beanModel.getBeanName())) {
				beanModel.setBeanName(BeanNameUtil.getBeanName(beanModel.getTagClass()));
			}
			/**
			 * 不能直接设置beanName,否则会出现其他问题
			 */
			String proxyBeanName = beanModel.getBeanName();
			if(StringUtils.isBlank(proxyBeanName)) {
				proxyBeanName = beanModel.getTagClass().getName();
			}
			
			TestUtil util = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
			
			Object proxyBean = util.getApplicationContext().getProxyBeanByClassAndBeanName(proxyBeanName, beanModel.getTagClass());
			if(proxyBean!=null && beanModel.getTagClass().isInstance(proxyBean)) {
                return proxyBean;
            }
		    
		    proxyBean = createBean(beanModel);
		    util.getApplicationContext().registProxyBean(proxyBeanName, proxyBean, beanModel.getTagClass());
	        return proxyBean;
		});
	}
	
    private static Object createBean(BeanModel beanModel) {
        Class<?> beanClass = beanModel.getTagClass();
        Object tag = null;
        /**
         * 构建其他类型的代理对象
         * 【Interface类型】 构建 InvocationHandler 代理对象，JDK自带
         * 【Class类型】构建MethodInterceptor代理对象，Cglib jar
         */
        try {
            if (beanClass.isInterface()) {
                LazyImple handler = new LazyImple(beanModel);
                tag = Proxy.newProxyInstance(JunitClassLoader.getInstance(), new Class[] { beanClass }, handler);
            } else {
                tag = buildCglibBean(beanModel);
            }
        } catch (Exception e) {
        	//查看是否本地构建
        	DebugObjectView.openView();
        	tag = LazyBean.refindCreateBeanFromFactory(beanModel.getTagClass(),beanModel.getBeanName());
        	DebugObjectView.clear();
        	if(tag == null) {
        		log.error("============重试查找bean失败============");
	        	if(e.getCause()!=null) {
	        		log.error("构建代理类异常=>beanClass:{},beanName:{}=>getCause：{};",beanClass,beanModel.getBeanName(),e.getCause().getMessage());
	        	}else {
	        		log.error("构建代理类异常=>beanClass:{},beanName:{}=>{}",beanClass,beanModel.getBeanName(),e.getMessage());
	        	}
        		throw e;
        	}
        } catch(NoClassDefFoundError e) {
        	throw new JunitException(e);
        }
        return tag;
    }
	protected static Object buildCglibBean(BeanModel beanModel) {
		try {
			SignalNotificationUtil.put(beanModel.getTagClass().getName(), "true");
			Class<?> tagClass = beanModel.getTagClass();
			ObjenesisStd objenesis = new ObjenesisStd();
			if(Modifier.isFinal(tagClass.getModifiers())) {
				return objenesis.newInstance(tagClass);
			}
			
//			if(tagClass.getName().contains("ReportMethodConfiguration")) {
//				log.info("ReportMethodConfiguration");
//			}
			
			Enhancer enhancer = new EnhancerExt();
	        Class<?>[] interfaces = tagClass.getInterfaces();
	        Class<?>[] allMockedTypes = prepend(tagClass, interfaces);
			enhancer.setClassLoader(JunitClassLoader.getInstance());
	        enhancer.setUseFactory(true);
	        if (tagClass.isInterface()) {
	            enhancer.setSuperclass(Object.class);
	            enhancer.setInterfaces(allMockedTypes);
	        } else {
	            enhancer.setSuperclass(tagClass);
	            enhancer.setInterfaces(interfaces);
	        }
	        enhancer.setCallbackTypes(new Class[]{LazyCglib.class, NoOp.class});
	        enhancer.setCallbackFilter(new CallbackFilter() {
				@Override
				public int accept(Method method) {
					return method.isBridge() ? 1 : 0;
				}
			});
	        enhancer.setSerialVersionUID(42L);
	        
			Class<?> proxyClass = enhancer.createClass();
			
			Factory factory = (Factory) objenesis.newInstance(proxyClass);
			factory.setCallbacks(new Callback[]{new LazyCglib(beanModel),NoOp.INSTANCE});
			
			
			return factory;
		} catch (Exception e) {
			log.error("buildCglibBean=>{}",beanModel,e);
			return null;
		}finally {
			SignalNotificationUtil.remove(beanModel.getTagClass().getName());
		}
	}
	private static class EnhancerExt extends Enhancer{
		protected void filterConstructors(Class sc, List constructors) {
            // Don't filter
        }
	}
	private static Class<?>[] prepend(Class<?> first, Class<?>... rest) {
        Class<?>[] all = new Class<?>[rest.length+1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }
	/**
	 * 构建代理对象
	 * @param beanClass 需要代理的类型
	 * @return 返回ProxyObject
	 */
	public Object buildProxy(Class<?> beanClass) {
	    BeanModel model = new BeanModel();
		model.setTagClass(beanClass);
		model.setBeanName(BeanNameUtil.getBeanNameFormAnno(beanClass));
		return buildProxy(model);
	}
	
	/**
	 * 注入
	 * @param obj
	 * @param objClassOrSuper
	 */
	static Set<String> exist = Sets.newHashSet();
	
	public void processAttr(BeanInitModel model) {
		Object obj = model.getObj();
		Class<?> objClassOrSuper = model.getTagClass();
		Class<?> objClass = LazyProxyManager.isProxy(obj)? LazyProxyManager.getProxyTagClass(obj): obj.getClass();
		if(objClass == objClassOrSuper) {
			//跳过
			String existKey = obj+"="+objClassOrSuper.getName();
			if(exist.contains(existKey)) {
				return;
			}
			exist.add(existKey);
		}
		Field[] fields = objClassOrSuper.getDeclaredFields();
		processField(obj, fields);
		
		Class<?> superC = objClassOrSuper.getSuperclass();
		if (superC != null && superC!=Object.class) {
			BeanInitModel tmp = new BeanInitModel();
			BeanUtils.copyProperties(model, tmp);
			tmp.setTagClass(superC);
			processAttr(tmp);
		}

		BeanInitHandler.getInstance().processMethod(model);
		
		ConfigurationProperties proconfig = (ConfigurationProperties) objClassOrSuper.getAnnotation(ConfigurationProperties.class);
		if(proconfig!=null) {
			LazyConfPropBind.processConfigurationProperties(obj,proconfig);
		}
	}
	
	/**
	 * 注入对应的属性值
	 * 
	 * 查询相应的属性注解
	 * 【Autowired】进行注入 相应类的代理对象
	 * 【Resource】进行注入 相应类的代理对象
	 * 【Value】注入相应的配置值。
	 * 
	 * @param obj 目标对象
	 * @param objClassOrSuper 目标对象父类，用于递归注入。
	 */
	public void processAttr(Object obj, Class<?> objClassOrSuper) {
		BeanInitModel model = new BeanInitModel();
		model.setObj(obj);
		model.setTagClass(objClassOrSuper);
		model.setBeanName(objClassOrSuper.getName());
//		model.setStatic(false);
		processAttr(model);
	}
    private void processField(Object obj, Field[] fields) {
        for(Field f : fields){
        	FieldAnnComponent.handlerField(new FieldDef(f,obj));
		}
    }
    Set<Class<?>> processed = Sets.newHashSet();
	public Object processStatic(Class<?> c) {
		if(processed.contains(c)) {
			return null;
		}
		try {
			processed.add(c);
			Object obj = buildProxy(c);
			if(obj!=null) {
				BeanInitModel model = new BeanInitModel();
				model.setObj(obj);
				model.setTagClass(c);
				model.setStatic(true);
				processAttr(model);
			}
			return obj;
		} catch (JunitException e) {
			if(e.isNeed_throw()) {
				throw e;
			}
			log.warn("处理静态工具类异常=>{}",c);
			return null;
		} catch (Exception e) {
			log.error("处理静态工具类异常=>{}",c,e);
			return null;
		}
	}
    private Map<Class<?>,Map<String,Method>> methodsCache = Maps.newHashMap();
    private Map<Class<?>,Map<String,Field>> fieldsCache = Maps.newHashMap();

    
    /**
     * 注解赋值
     * @param fieldName 赋值的成员变量
     * @param obj 需要赋值的对象
     * @param superClass 对象父级类
     * @param value 赋值对象
     * @return true 赋值成功
     */
	public boolean setFieldValueFromExpression(String fieldName, Object obj,Class<?> superClass,Object value) {
		/**
		 * 存在重载方法的可能
		 */
		boolean handlerSuccess = handlerByReload(fieldName, obj, superClass, value);
		if(handlerSuccess) {
			return true;
		}
		
		
		if(fieldsCache.containsKey(superClass)) {
			Field tagM = fieldsCache.get(superClass).get(fieldName);
		    if(tagM!=null) {
		    	if(value instanceof String) {
		    		TestUtil util = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
		    		value = util.valueFromEnvForAnnotation(value.toString(), tagM.getType());	
				}
		    	FieldAnnComponent.setObj(tagM, obj, value);
		    	return true;
		    }
		}
		boolean found = findAndSet(fieldName, obj, superClass, value);
		Class<?> superC = superClass.getSuperclass();
		if (!found && superC != null ) {
			return setFieldValueFromExpression(fieldName,obj,superC,value);
		}
		return false;
	}
	private boolean handlerByReload(String fieldName, Object obj, Class<?> superClass, Object value) {
		if(value == null) {
			return false;
		}
		String mName = "set"+fieldName.toLowerCase();
		String mNameKey = "set"+fieldName.toLowerCase()+value.getClass().getName();
		if(methodsCache.containsKey(superClass)) {
		    Method tagM = methodsCache.get(superClass).get(mNameKey);
		    if(tagM!=null) {
		    	boolean success = invokeSet(fieldName, obj, value, tagM);
		    	if(success) {
		    		return true;
		    	}
		    }
		}else {
		    methodsCache.put(superClass, Maps.newHashMap());
		}
		Method[] methods = superClass.getDeclaredMethods();
		for(Method m : methods) {
			if(Objects.equals(m.getName().toLowerCase(), mName) && (m.getParameterTypes()[0] == value.getClass())) {
				boolean success = invokeSet(fieldName, obj, value, m);
				if(success) {
				    methodsCache.get(superClass).put(mNameKey, m);
				    return true;
				}
			}
		}
		return false;
	}
	private boolean findAndSet(String fieldName, Object obj, Class<?> superClass, Object value) {
		Field[] fields = superClass.getDeclaredFields();
		for(Field field : fields){
		    if(Modifier.isFinal(field.getModifiers())) {
		        continue;
		    }
			if(Objects.equals(field.getName(), fieldName)) {
			    Object fv = value;
				if(value instanceof String) {
					TestUtil util = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
					fv = util.valueFromEnvForAnnotation(value.toString(), field.getType());	
				}
				FieldAnnComponent.setObj(field, obj, fv);
				if(!fieldsCache.containsKey(superClass)) {
					fieldsCache.put(superClass, Maps.newHashMap());
				}
				fieldsCache.get(superClass).put(fieldName, field);
				return true;
			}
		}
		return false;
	}
    private boolean invokeSet(String field, Object obj, Object value, Method m) {
        Object fv = value;
        if(value instanceof String) {
        	TestUtil util = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
        	fv = util.valueFromEnvForAnnotation(value.toString(), m.getParameterTypes()[0]);	
        }
        try {
            if(fv != null) {
                m.invoke(obj, fv);
            }else {
                log.warn("field:{}=>{}",field,value);
            }
        	return true;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		    log.warn("m=>{},field=>{},value=>{}",m,field,value,e);
        }
        return false;
    }
	
	public static boolean existBean(Class<?> beanClass) {
		Annotation[] anns = beanClass.getDeclaredAnnotations();
		for(Annotation ann : anns) {
			Class<?> type = ann.annotationType();
			if ((type == Component.class || type.getAnnotation(Component.class)!=null)
					|| (type == Service.class || type.getAnnotation(Service.class)!=null)
					|| (type == Configuration.class || type.getAnnotation(Configuration.class)!=null)
//					|| (type == RestController.class || type.getAnnotation(RestController.class)!=null)
					|| (type == Controller.class || type.getAnnotation(Controller.class)!=null)
					) {
				return true;
			}
		}
		return false;
	}
	/**
	 * 
	 * @param beanClass 目标类
	 * @param tagAnn 获取Annotation 
	 * @return 返回存在的 Annotation
	 */
	public static Annotation findByAnnotation(Class beanClass,Class<? extends Annotation> tagAnn) {
		Annotation[] anns = beanClass.getDeclaredAnnotations();
		for(Annotation ann : anns) {
			Class<?> type = ann.annotationType();
			if ((type == tagAnn)) {
				return ann;
			}
		}
		return null;
	}
	
	public static Map<String,Object> beanMaps = Maps.newHashMap();
	/**
	 * 通过BeanName 获取bean
	 * @param beanName beanName
	 * @return 返回bean
	 */
	public Object findBean(String beanName) {
		if(beanName.equals("DEFAULT_DATASOURCE")) {
			TestUtil util = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
		    return util.getApplicationContext().getProxyBeanByClass(DataSource.class);
		}
		Class<?> tagC = ScanUtil.findClassByName(beanName);
		if(tagC!=null) {
		    BeanModel beanModel = new BeanModel();
		    beanModel.setBeanName(beanName);
		    beanModel.setTagClass(tagC);
		    return LazyBean.createBean(beanModel);
		}else {
			return findCreateBeanFromFactory(null,beanName);
		}
	}
	
	/**
	 * 
	 * 通过注解查找Bean
	 * 
	 * @param annotationType Annotation类型
	 * @return 返回存在annotationType 的对象
	 */
	private static Map<Class<?>,Map<String,Object>> cacheMap = Maps.newConcurrentMap();
	public static Map<String, Object> findBeanWithAnnotation(Class<? extends Annotation> annotationType) {
		if(cacheMap.containsKey(annotationType)) {
			return cacheMap.get(annotationType);
		}
		List<Class<?>> list = ScanUtil.findClassWithAnnotation(annotationType,ClassScan.getApplicationAllClassMap());
		Map<String, Object> annoClass = Maps.newHashMap();
		list.stream().forEach(c ->{
//			String beanName = getBeanName(c);
			annoClass.put(c.getSimpleName(), LazyBean.getInstance().buildProxy(c));
		});
		cacheMap.put(annotationType, annoClass);
		return annoClass;
	}
	
	/**
	 * 通过class 获取 bean
	 * @param requiredType bean类型
	 * @return 返回bean
	 */
	public Object findBean(Class<?> requiredType) {
		return buildProxy(requiredType);
	}
	public static Object refindCreateBeanFromFactory(Class<?> classBean, String beanName) {
		DebugObjectView.readView(()->{
			log.debug("=================重试查找bean=={}=={}=============",classBean,beanName);
		});
		BeanModel asse = new BeanModel();
		asse.setTagClass(classBean);
		asse.setBeanName(beanName);
		JunitMethodDefinition ojb_meth = ScanUtil.findCreateBeanFactoryClass(asse);
		if(ojb_meth==null) {
			DebugObjectView.readView(()->{
				log.debug("=================重试查找bean失败2=={}===============",asse);
			});
			return null;
		}
		Object tagObj = JavaBeanUtil.getInstance().buildBean(ojb_meth.getConfigurationClass(),ojb_meth.getMethod(),asse);
		return tagObj;
	}
	public static BeanInitModel findCreateMethodFromFactory(Class<?> classBean) {
		return findCreateMethodFromFactory(classBean, null);
	}
	public static BeanInitModel findCreateMethodFromFactory(Class<?> classBean, String beanName) {
		BeanModel asse = new BeanModel();
		asse.setBeanName(beanName);
		asse.setTagClass(classBean);
		asse.setCreateBean(false);
		/**
		 * 优先用户自定义Bean
		 */
		return findCreateBeanFromFactory(asse);
	}
	
	public static Object findCreateBeanFromFactory(Class<?> classBean, String beanName) {
		BeanModel asse = new BeanModel();
		asse.setBeanName(beanName);
		asse.setTagClass(classBean);
		asse.setCreateBean(true);
		/**
		 * 优先用户自定义Bean
		 */
		BeanInitModel tmpObj = findCreateBeanFromFactory(asse);
		if(tmpObj!=null) {
			return tmpObj.getObj();
		}
		return null;
	}
	
	public static BeanInitModel findCreateBeanFromFactory(Class<?> classBean) {
		if(!ScanUtil.isInit()) {
			return null;
		}
		BeanModel asse = new BeanModel();
		asse.setTagClass(classBean);
		/**
		 * 优先用户自定义Bean
		 */
		asse.setCreateBean(true);
		BeanInitModel tmpObj = findCreateBeanFromFactory(asse);
		return tmpObj;
	}
	public static BeanInitModel findCreateBeanFromFactory(BeanModel assemblyData) {
		return (BeanInitModel) StackOverCheckUtil.observeIgnoreException(()->StackOverCheckUtil.observe(()->{
			JunitMethodDefinition jmd = ScanUtil.findCreateBeanFactoryClass(assemblyData);
			if(jmd==null) {
				return null;
			}
			BeanInitModel model = new BeanInitModel();
			if(assemblyData.isCreateBean()) {
				Object tagObj = JavaBeanUtil.getInstance().buildBean(jmd.getConfigurationClass(),jmd.getMethod(),assemblyData);
				model.setObj(tagObj);
			}
			Bean beanAnn = jmd.getMethod().getAnnotation(Bean.class);
			if(beanAnn!=null && beanAnn.value().length>0) {
				model.setBeanName(beanAnn.value()[0]);
			}else {
				model.setBeanName(jmd.getMethod().getName());
			}
			return model;
		}, assemblyData));
	}
	
	@SuppressWarnings("unchecked")
	public static List<BeanInitModel> findModelsFromFactory(BeanModel assemblyData) {
		if(!ScanUtil.isInit()) {
			return null;
		}
		return (List<BeanInitModel>) StackOverCheckUtil.observeIgnoreException(()->StackOverCheckUtil.observe(()->{
			List<JunitMethodDefinition> jmds = ScanUtil.findCreateBeanFactoryClasses(assemblyData);
			return jmds.stream().map(item->{
				BeanInitModel model = new BeanInitModel();
				if(assemblyData.isCreateBean()) {
					Object tagObj = JavaBeanUtil.getInstance().buildBean(item.getConfigurationClass(),item.getMethod(),assemblyData);
					model.setObj(tagObj);
				}
				Bean beanAnn = item.getMethod().getAnnotation(Bean.class);
				if(beanAnn!=null && beanAnn.value().length>0) {
					model.setBeanName(beanAnn.value()[0]);
				}else {
					model.setBeanName(item.getMethod().getName());
				}
				model.setJmd(item);
				return model;
			}).collect(Collectors.toList());
		}, assemblyData));
	}
//	public static Object findCreateBeanFromFactory(BeanModel assemblyData) {
//		
//	}
	
	public static List<?> findListBeanExt(Class<?> requiredType) {
		List list = Lists.newArrayList();
		list.addAll(findListBean(requiredType));
		
		LazyListableBeanFactory beanFactory = LazyListableBeanFactory.getInstance(); 
		String[] beanNames = beanFactory.getBeanNamesForType(requiredType);
		for(String name:beanNames) {
			list.add(beanFactory.getBean(name));
		}
		
		return list;
	}
	
	/**
	 * 通过class 查找它的所有继承者或实现者
	 * @param requiredType 接口或者抽象类
	 * @return 放回List对象
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List findListBean(Class<?> requiredType) {
		/*
		 * TODO List 处理
		 */
		List list = Lists.newArrayList();
		List<Class<?>> tags = null;
		if(requiredType.isInterface()) {
			tags = ScanUtil.findClassImplInterface(requiredType);
		}else {
			tags = ScanUtil.findClassExtendAbstract(requiredType);
		}
		if (!tags.isEmpty()) {
			tags.stream().forEach(item ->list.add(LazyBean.getInstance().buildProxy(item)));
		}
		return list;
	}
	
	public static Object findCreateByProp(Class<?> tagertC) {
		try {
			return PropResourceManager.getInstance().findCreateByProp(tagertC);
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
	
	@Override
	public String getBeanKey() {
		return LazyBeanI.class.getSimpleName();
	}
	public static Callback createLazyCglib(BeanModel model) {
		return new LazyCglib(model);
	}
	
	public static BeanModel buildBeanModel(Annotation[] annotations,Class<?> tagClass) {
		BeanModel model = new BeanModel();
		model.setTagClass(tagClass);
		for(Annotation annotation : annotations) {
			Class<?> annClass = annotation.annotationType();
			if(annClass == Resource.class) {
				model.setBeanName(((Resource)annotation).name());
				break;
			}else if(annClass == Qualifier.class) {
				model.setBeanName(((Qualifier)annotation).value());
				break;
			}
		}
		if(model.getBeanName() == null) {
			model.setBeanName(BeanNameUtil.getBeanNameFormAnno(tagClass));
		}
		return model;
	}
}

