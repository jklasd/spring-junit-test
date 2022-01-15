package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.loader.PropResourceLoader;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;
import com.github.jklasd.test.lazyplugn.spring.ObjectProviderImpl;
import com.github.jklasd.test.lazyplugn.spring.configprop.LazyConfPropBind;
import com.github.jklasd.test.util.BeanNameUtil;
import com.github.jklasd.test.util.DebugObjectView;
import com.github.jklasd.test.util.ScanUtil;
import com.github.jklasd.test.util.StackOverCheckUtil;
import com.google.common.base.Objects;
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
public class LazyBean {
	public static final String PROXY_BEAN_FIELD = "CGLIB$CALLBACK_0";
	public static Map<Class<?>, List<Object>> singleton = Maps.newHashMap();
	public static Map<String, Object> singletonName = Maps.newHashMap();
	private TestUtil util = TestUtil.getInstance();
	private LazyBean() {}
	private static LazyBean bean;
	public static LazyBean getInstance() {
	    if(bean==null) {
	        bean = new LazyBean();
	    }
	    return bean;
	}
	/**
	 * 
	 * 构建代理对象 classBean 
	 * @param classBean 代理类型
	 * @param classGeneric 代理类的泛型类型
	 * @return 代理对象
	 */
	public Object buildProxyForGeneric(Class classBean,Type[] classGeneric) {
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
	public Object buildProxy(BeanModel beanModel) {
		return StackOverCheckUtil.observeIgnore(()->{
			Object obj = null;
		    if(StringUtils.isNotBlank(beanModel.getBeanName())) {
		        obj = util.getApplicationContext().getBeanByClassAndBeanName(beanModel.getBeanName(), beanModel.getTagClass());
		        if(obj!=null) {
		            return obj;
		        }
		    }else {
		        obj = util.getApplicationContext().getBeanByClass(beanModel.getTagClass());
	            if(obj!=null) {
	                return obj;
	            }
	            beanModel.setBeanName(BeanNameUtil.getBeanName(beanModel.getTagClass()));
		    }
		    
		    if(beanModel.getTagClass() == ApplicationContext.class
		        || ScanUtil.isExtends(beanModel.getTagClass(), ApplicationContext.class)
		        || ScanUtil.isImple(beanModel.getTagClass(), ApplicationContext.class)) {
		        return util.getApplicationContext();
		    }
		    obj = createBean(beanModel);
		    util.getApplicationContext().registBean(beanModel.getBeanName(), obj, beanModel.getTagClass());
	        return obj;
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
                tag = Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] { beanClass }, handler);
            } else {
                LazyCglib handler = new LazyCglib(beanModel);
                if(!handler.hasFinalMethod() && handler.findPublicConstrucors()) {
                    if(handler.getArgumentTypes().length>0) {
                        Enhancer enhancer = new Enhancer();
                        enhancer.setSuperclass(beanClass);
                        enhancer.setCallback(handler);
                        tag = enhancer.create(handler.getArgumentTypes(), handler.getArguments());
                    }else {
                        tag = Enhancer.create(beanClass, handler);
                    }
                }else {
                    tag = handler.getTagertObj();
                }
                if(tag == null) {
                    log.error("不存在公开无参构造函数");
                }
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
        }
        return tag;
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
	
	public void setObj(Field f,Object obj,Object proxyObj) {
		log.debug("{}注入属性:{}",obj.getClass(),f.getName());
		setObj(f, obj, proxyObj, null);
	}
	/**
	 * 反射写入值。
	 * @param f	属性
	 * @param obj	属性所属目标对象
	 * @param proxyObj 写入属性的代理对象
	 * @param proxyBeanName 存在bean名称时，可传入。
	 * 
	 * 
	 */
	public static void setObj(Field f,Object obj,Object proxyObj,String proxyBeanName) {
		if(proxyObj == null) {//延迟注入,可能启动时，未加载到bean
//			util.loadLazyAttr(obj, f, proxyBeanName);
		}
		try {
			if (!f.isAccessible()) {
				f.setAccessible(true);
			}
			f.set(obj, proxyObj);
		} catch (Exception e) {
			log.error("注入对象异常",e);
		}
	}
	/**
	 * 注入
	 * @param obj
	 * @param objClassOrSuper
	 */
	static Set<String> exist = Sets.newHashSet();
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
//		if(objClassOrSuper.getName().contains("JedisCluster")) {
//			log.info("需要注入=>{}=>{}",objClassOrSuper.getName());
//		}
		if(obj.getClass() == objClassOrSuper) {
			//跳过
			String existKey = obj+"="+objClassOrSuper.getName();
			if(exist.contains(existKey)) {
				return;
			}
			exist.add(existKey);
		}
		Field[] fields = objClassOrSuper.getDeclaredFields();
		processField(obj, fields);
		
//		processMethod(objClassOrSuper,obj);
		
		Class<?> superC = objClassOrSuper.getSuperclass();
		if (superC != null && superC!=Object.class) {
			processAttr(obj, superC);
		}

		Method[] ms = obj.getClass().getDeclaredMethods();
		try {
			BeanInitHandler.getInstance().processMethod(obj, ms,superC);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.error("processMethod=>{}+>{}",objClassOrSuper);
            log.error("processMethod",e);
        }
		
		ConfigurationProperties proconfig = (ConfigurationProperties) objClassOrSuper.getAnnotation(ConfigurationProperties.class);
		if(proconfig!=null) {
			LazyConfPropBind.processConfigurationProperties(obj,proconfig);
		}
	}
    private void processField(Object obj, Field[] fields) {
        for(Field f : fields){
			Autowired aw = f.getAnnotation(Autowired.class);
			if (aw != null) {
				String bName = f.getAnnotation(Qualifier.class)!=null?f.getAnnotation(Qualifier.class).value():null;
				if(f.getType() == List.class) {
					ParameterizedType t = (ParameterizedType) f.getGenericType();
					Type[] item = t.getActualTypeArguments();
					if(item.length == 1) {
						//处理一个集合注入
						try {
							Class<?> c = Class.forName(item[0].getTypeName());
							List<?> list = findListBean(c);
							setObj(f, obj, list);
							log.info("{}注入集合=>{},{}个对象",obj.getClass(),f.getName(),list.size());
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}else {
						log.info("其他特殊情况");
					}
				}else {
				    setObj(f, obj, buildProxy(f.getType(),bName));
				}
			} else {
				Value v = f.getAnnotation(Value.class);
				if (v != null) {
					setObj(f, obj, util.value(v.value().replace("${", "").replace("}", ""), f.getType()));
				} else {
					javax.annotation.Resource c = f.getAnnotation(javax.annotation.Resource.class);
					if (c != null) {
//						if(StringUtils.isNotBlank(c.name())) {
//							setObj(f, obj, buildProxy(f.getType(),c.name()),c.name());
//						}else {
//						}
						setObj(f, obj, buildProxy(f.getType(),c.name()));
					} else {
						log.debug("不需要需要注入=>{}", f.getName());
					}
				}
			}
		}
    }
	private static Class<?> getParamType(Method m, Type paramType) {
		if(paramType instanceof ParameterizedType) {
			ParameterizedType  pType = (ParameterizedType) paramType;
			Type[] item = pType.getActualTypeArguments();
			if(item.length == 1) {
				//处理一个集合注入
				try {
					log.debug("获取paramType泛型类型=>{}",paramType);
					return Class.forName(item[0].getTypeName());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}else {
				log.info("其他特殊情况");
			}
		}else {
			return (Class<?>) paramType;
		}
		return null;
	}

	public Object processStatic(Class<?> c) {
		try {
			Object obj = buildProxy(c);
			if(obj!=null) {
				processAttr(obj, c);
			}
			return obj;
		} catch (Exception e) {
			log.error("处理静态工具类异常=>{}",c);
			return null;
		}
	}
    private Map<Class<?>,Map<String,Method>> methodsCache = Maps.newHashMap();
    private Map<Class<?>,Map<String,Method>> fieldsCache = Maps.newHashMap();
	public boolean setAttr(String field, Object obj,Class<?> superClass,Object value) {
//	        if(field.contains("interface")) {
//	            log.info("断点");
//	        }
//			Object fv = value;
			String mName = "set"+field.substring(0, 1).toUpperCase()+field.substring(1);
			if(methodsCache.containsKey(superClass)) {
			    Method tagM = methodsCache.get(superClass).get(mName);
			    if(tagM!=null) {
			        invokeSet(field, obj, value, tagM);
			    }
			}else {
			    methodsCache.put(superClass, Maps.newHashMap());
			}
			Method[] methods = superClass.getDeclaredMethods();
			for(Method m : methods) {
				if(Objects.equal(m.getName(), mName) && (value!= null && m.getParameterTypes()[0] == value.getClass())) {
					boolean success = invokeSet(field, obj, value, m);
					if(success) {
					    methodsCache.get(superClass).put(mName, m);
					    return success;
					}
				}
			}
			Field[] fields = superClass.getDeclaredFields();
			boolean found = false;
				for(Field f : fields){
				    if(Modifier.isFinal(f.getModifiers())) {
				        continue;
				    }
					if(Objects.equal(f.getName(), field)) {
					    Object fv = value;
						if(value instanceof String) {
							fv = util.value(value.toString(), f.getType());	
						}
						try {
							setObj(f, obj, fv);
						} catch (IllegalArgumentException e) {
							log.error("",e);
							return false;
						}
						return true;
					}
				}
			Class<?> superC = superClass.getSuperclass();
			if (!found && superC != null ) {
				return setAttr(field,obj,superC,value);
			}
		return false;
	}
    private boolean invokeSet(String field, Object obj, Object value, Method m) {
        Object fv = value;
        if(value instanceof String) {
        	fv = util.value(value.toString(), m.getParameterTypes()[0]);	
        }
        try {
            if(fv != null) {
                m.invoke(obj, fv);
            }else {
                log.warn("field:{}=>{}",field,value);
            }
        	return true;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		    log.info("m=>{},field=>{},value=>{}",m,field,value);
        	e.printStackTrace();
        }
        return false;
    }
	
	public static boolean existBean(Class beanClass) {
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
		    return util.getApplicationContext().getBeanByClass(DataSource.class);
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
	 * @param beanName bean名称
	 * @param type bean类型
	 * @return 返回bean对象
	 */
	public Object findBean(String beanName,Class<?> type) {
		if(type.isInterface()) {
			List<Class<?>> classList = ScanUtil.findClassImplInterface(type);
			for(Class<?> c : classList) {
				Service ann = (Service) findByAnnotation(c,Service.class);
				Component cAnn = (Component) findByAnnotation(c,Component.class);
				if ((ann != null && ann.value().equals(beanName)) | (cAnn != null && cAnn.value().equals(beanName))) {
					return buildProxy(c, beanName);
				}
			}
			log.warn("ScanUtil # findBean=>Interface[{}]",type);
		}else if(Modifier.isAbstract(type.getModifiers())) {//抽象类
		}else {
			Object obj = findBean(beanName); 
			try {
				if(type.getConstructors().length>0) {
					return  obj == null?type.newInstance():obj;
				}else {
					throw new NoSuchBeanDefinitionException("没有获取到构造器");
				}
			} catch (InstantiationException | IllegalAccessException e) {
				log.error("不能构建bean=>{}=>{}",beanName,type);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 通过注解查找Bean
	 * 
	 * @param annotationType Annotation类型
	 * @return 返回存在annotationType 的对象
	 */
	public static Map<String, Object> findBeanWithAnnotation(Class<? extends Annotation> annotationType) {
		List<Class<?>> list = ScanUtil.findClassWithAnnotation(annotationType,ClassScan.getApplicationAllClassMap());
		Map<String, Object> annoClass = Maps.newHashMap();
		list.stream().forEach(c ->{
//			String beanName = getBeanName(c);
			annoClass.put(c.getSimpleName(), LazyBean.getInstance().buildProxy(c));
		});
		return annoClass;
	}
	
	public Object findBeanByInterface(Class<?> interfaceClass, Type[] classGeneric) {
		if(classGeneric == null) {
			return findBeanByInterface(interfaceClass);
		}
		if(interfaceClass.getName().startsWith(ScanUtil.SPRING_PACKAGE)) {
			List<Class<?>> cL = ScanUtil.findClassImplInterface(interfaceClass,ScanUtil.findClassMap(ScanUtil.SPRING_PACKAGE),null);
			if(!cL.isEmpty()) {
				Class c = cL.get(0);
				throw new JunitException("待开发");
			}else {
				if(interfaceClass == ObjectProvider.class) {
					return new ObjectProviderImpl(classGeneric[0]);
				}
			}
		}
		return null;
	}
	
	/**
	 * 扫描类 for bean
	 * @param interfaceClass  接口
	 * @return 返回实现接口的对象
	 */
	public Object findBeanByInterface(Class<?> interfaceClass) {
		if(interfaceClass == ApplicationContext.class || ScanUtil.isExtends(ApplicationContext.class, interfaceClass)
				|| ScanUtil.isExtends(interfaceClass,ApplicationContext.class)) {
			return util.getApplicationContext();
		}
		if(interfaceClass == Environment.class
				|| ScanUtil.isExtends(Environment.class,interfaceClass)
				|| ScanUtil.isExtends(interfaceClass, Environment.class)) {
			return util.getApplicationContext().getEnvironment();
		}
		if(interfaceClass.getPackage().getName().startsWith(ScanUtil.SPRING_PACKAGE)) {
//			List<Class<?>> cL = ScanUtil.findClassImplInterface(interfaceClass,ScanUtil.findClassMap(ScanUtil.SPRING_PACKAGE),null);
//			if(!cL.isEmpty()) {
//				return LazyBean.getInstance().buildProxy(cL.get(0));
//			}
			log.error("****************SPRING_PACKAGE 需处理**{}**************",interfaceClass);
			return null;
		}
		List<Class<?>> tags = ScanUtil.findClassImplInterface(interfaceClass);
		if (!tags.isEmpty()) {
			return LazyBean.getInstance().buildProxy(tags.get(0));
		}
		return null;
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
			log.warn("=================重试查找bean=={}=={}=============",classBean,beanName);
		});
		AssemblyDTO asse = new AssemblyDTO();
		asse.setTagClass(classBean);
		asse.setBeanName(beanName);
		Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(asse);
		if(ojb_meth[0] ==null || ojb_meth[1]==null) {
			DebugObjectView.readView(()->{
				log.warn("=================重试查找bean失败2=={}===============",asse);
			});
			return null;
		}
		Object tagObj = JavaBeanUtil.getInstance().buildBean((Class<?>)ojb_meth[0],(Method)ojb_meth[1],asse);
		return tagObj;
	}
	public static Object findCreateBeanFromFactory(Class<?> classBean, String beanName) {
		AssemblyDTO asse = new AssemblyDTO();
		if(classBean!=null) {
			asse.setTagClass(classBean);
			if(classBean.getName().startsWith(ScanUtil.SPRING_PACKAGE)) {
				Object tmpObj = findCreateBeanFromFactory(asse);
				if(tmpObj!=null) {
					return tmpObj;
				}
				asse.setNameMapTmp(ScanUtil.findClassMap(ScanUtil.SPRING_PACKAGE));
			}
		}
		asse.setBeanName(beanName);
		return findCreateBeanFromFactory(asse);
	}
	public static Object findCreateBeanFromFactory(AssemblyDTO assemblyData) {
		return StackOverCheckUtil.observeIgnore(()->StackOverCheckUtil.observe(()->{
			Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(assemblyData);
			if(ojb_meth[0] ==null || ojb_meth[1]==null) {
				return null;
			}
			Object tagObj = JavaBeanUtil.getInstance().buildBean((Class<?>)ojb_meth[0],(Method)ojb_meth[1],assemblyData);
			return tagObj;
		}, assemblyData));
	}
	
	/**
	 * 通过class 查找它的所有继承者或实现者
	 * @param requiredType 接口或者抽象类
	 * @return 放回List对象
	 */
	@SuppressWarnings("unchecked")
	public static List findListBean(Class<?> requiredType) {
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
	/**
	 * 
	 * @param beanName beanName
	 * @param type 目标类型
	 * @return 代理对象构建真实对象
	 */
	public Object createBeanForProxy(String beanName, Class<?> type) {
		Class<?> tagClass = null;
		if(type.isInterface()) {
			List<Class<?>> classList = ScanUtil.findClassImplInterface(type);
			for(Class<?> c : classList) {
				Service ann = (Service) findByAnnotation(c,Service.class);
				Component cAnn = (Component) findByAnnotation(c,Component.class);
				if (ann != null  || cAnn != null ) {
				    if ((ann != null && ann.value().equals(beanName)) || (cAnn != null && cAnn.value().equals(beanName))) {
				        tagClass = c;
				        break;
				    }
				    tagClass = c;
				}
			}
			log.warn("ScanUtil # findBean=>Interface[{}]",type);
		}
		Object obj = null;
		if(tagClass == null) {
		    obj = util.getApplicationContext().getBean(type);
			if(obj == null && type.isInterface()) {
				obj = findBeanByInterface(type);
			}
		}else {
		    BeanModel model = new BeanModel();
		    model.setBeanName(beanName);
		    model.setTagClass(tagClass);
			obj = buildProxy(model);
		}
		if(obj != null) {
			singletonName.put(beanName, obj);
		}
		return obj;
	}
	public static Object findCreateByProp(Class<?> tagertC) {
		try {
			return PropResourceLoader.getInstance().findCreateByProp(tagertC);
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
}

