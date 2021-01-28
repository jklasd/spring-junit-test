package com.junit.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.junit.test.db.LazyMongoBean;
import com.junit.test.db.LazyMybatisMapperBean;
import com.junit.test.dubbo.LazyDubboBean;
import com.junit.test.mq.LazyMQBean;
import com.junit.test.spring.LazyConfigurationPropertiesBindingPostProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 * 2020-11-19
 */
@Slf4j
public class LazyBean {

	public static Map<Class, Object> singleton = Maps.newHashMap();
	public static Map<String, Object> singletonName = Maps.newHashMap();
	/**
	 * 构建代理对象
	 * @param classBean
	 * @return
	 */
	public static Object buildProxy(Class classBean,String beanName) {
		/**
		 * 取缓存值
		 */
		if(singletonName.containsKey(beanName)) {
			return singletonName.get(beanName);
		}
		if(StringUtils.isBlank(beanName)) {
			if (singleton.containsKey(classBean)) {
				return singleton.get(classBean);
			}
		}
		/**
		 * 开始构建对象
		 */
		
		Object tag = null;
		/**
		 * 判断是否是MQ类型,是MQ类型，则构建MQ对象
		 */
		if(classBean.getPackage().getName().contains(LazyMQBean.packageName)) {
			try {
				tag = LazyMQBean.buildBean(classBean);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}else {
			/**
			 * 构建其他类型的代理对象
			 * 【Interface类型】 构建 InvocationHandler 代理对象，JDK自带
			 * 【Class类型】构建MethodInterceptor代理对象，Cglib jar
			 */
			try {
				if (classBean.isInterface()) {
					InvocationHandler handler = new LazyImple(classBean,beanName);
					tag = Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] { classBean }, handler);
					
				} else {
					MethodInterceptor handler = new LazyCglib(classBean,beanName);
					tag = Enhancer.create(classBean, handler);
				}
			} catch (Exception e) {
				
				/**
				 * 查询是否有构建bean的configration
				 */
				if(LazyMongoBean.isMongo(classBean)) {
					tag = LazyMongoBean.buildBean(classBean,beanName);
				}else {
					tag = ScanUtil.findCreateBeanFromFactory(classBean,beanName);
					if(tag == null) {
						/**
						 * 当无法构建代理对象时，从spring 容器里取。
						 */
						tag = TestUtil.getExistBean(classBean, beanName);
						if(tag == null) {
							System.out.println("[ERROR]代理Bean=>"+classBean+"=>"+beanName);
						}
					}
				}
			}
		}
		if(tag!=null) {
			if(StringUtils.isNotBlank(beanName)) {
				singletonName.put(beanName, tag);
			}else {
				singleton.put(classBean, tag);
			}
		}
		return tag;
	}
	/**
	 * 构建代理对象
	 * @param classBean
	 * @return
	 */
	public static Object buildProxy(Class classBean) {
		if (singleton.containsKey(classBean)) {
			return singleton.get(classBean);
		}
		Object tag = buildProxy(classBean,null);
		if(tag!=null) {
			singleton.put(classBean, tag);
		}
		return tag;
	}
	public static void setObj(Field f,Object obj,Object proxyObj) {
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
			TestUtil.loadLazyAttr(obj, f, proxyBeanName);
		}
		try {
			if (!f.isAccessible()) {
				f.setAccessible(true);
			}
			f.set(obj, proxyObj);
		} catch (Exception e) {
			e.printStackTrace();
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
	public static void processAttr(Object obj, Class objClassOrSuper) {
//		if(objClassOrSuper.getName().contains("MultiServiceConfiguration")) {
//			log.info("需要注入=>{}=>{}",objClassOrSuper.getName());
//		}
		if(!TestUtil.isTest()) {
			TestUtil.openTest();
		}
		
		if(exist.contains(obj.hashCode()+"="+objClassOrSuper.getName())) {
			return;
		}
		exist.add(obj.hashCode()+"="+objClassOrSuper.getName());
		Field[] fields = objClassOrSuper.getDeclaredFields();
		for(Field f : fields){
			Autowired aw = f.getAnnotation(Autowired.class);
			if (aw != null) {
				if(f.getType() == List.class) {
					ParameterizedType t = (ParameterizedType) f.getGenericType();
					Type[] item = t.getActualTypeArguments();
					if(item.length == 1) {
						//处理一个集合注入
						try {
							Class<?> c = Class.forName(item[0].getTypeName());
							setObj(f, obj, ScanUtil.findListBean(c));
							log.info("注入集合=>{}",f.getName());
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}else {
						log.info("其他特殊情况");
					}
				}else {
					if(ScanUtil.isBean(f.getType()) && TestUtil.getExistBean(f.getType(), f.getName())!=null) {
						setObj(f, obj, TestUtil.getExistBean(f.getType(), f.getName()));
					}else {
						setObj(f, obj, buildProxy(f.getType()));
					}
				}
			} else {
				Value v = f.getAnnotation(Value.class);
				if (v != null) {
					setObj(f, obj, TestUtil.value(v.value().replace("${", "").replace("}", ""), f.getType()));
				} else {
					javax.annotation.Resource c = f.getAnnotation(javax.annotation.Resource.class);
					if (c != null) {
						if(StringUtils.isNotBlank(c.name())) {
							setObj(f, obj, buildProxy(f.getType(),c.name()),c.name());
						}else {
							setObj(f, obj, buildProxy(f.getType()));
						}
					} else {
						log.debug("不需要需要注入=>{}", f.getName());
					}
				}
			}
		}
		Class<?> superC = objClassOrSuper.getSuperclass();
		if (superC != null) {
			processAttr(obj, superC);
		}

		Method[] ms = obj.getClass().getDeclaredMethods();
		postConstruct(obj, ms,superC);
		
		ConfigurationProperties proconfig = (ConfigurationProperties) objClassOrSuper.getAnnotation(ConfigurationProperties.class);
		if(proconfig!=null) {
			LazyConfigurationPropertiesBindingPostProcessor.processConfigurationProperties(obj,proconfig);
		}
	}

	public static Object processStatic(Class c) {
		try {
			Object obj = buildProxy(c);
			processAttr(obj, c);
			return obj;
		} catch (Exception e) {
			log.error("处理静态工具类异常",e);
			return null;
		}
	}
	/**
	 * 对目标对象方法进行处理
	 * @param obj 目标对象
	 * @param ms 方法组
	 * @param sup 父类
	 * 
	 * 主要处理 
	 * 【1】PostConstruct注解方法
	 * 【2】setApplicationContext
	 * 
	 * 当目标对象存在父类时，遍历所有父类对相应方法进行处理
	 */
	private static void postConstruct(Object obj, Method[] ms,Class sup) {
		if(sup != null) {
			ms = sup.getDeclaredMethods();
			postConstruct(obj, ms, sup.getSuperclass());
		}
		for (Method m : ms) {
			if (m.getAnnotation(PostConstruct.class) != null) {//当实际对象存在初始化方法时。
				try {
					if (!m.isAccessible()) {
						m.setAccessible(true);
					}
					m.invoke(obj, null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}else if(m.getName().equals("setApplicationContext")//当对象方法存是setApplicationContext
					&& (sup == null || !sup.getName().contains("AbstractJUnit4SpringContextTests"))) {
				try {
					if(m!=null) {
						try {
							m.invoke(obj,TestUtil.bf);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							log.error("不能注入applicationContext",e);
						}
					}
				} catch (SecurityException e) {
				}
			}
		}
	}
}

@Slf4j
class LazyCglib implements MethodInterceptor {
	private Class tag;
	private String beanName;

	public LazyCglib(Class tag) {
		this.tag = tag;
	}
	public LazyCglib(Class tag,String beanName) {
		this.tag = tag;
		this.beanName = beanName;
	}

	@Override
	public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
		try {
			AopContextSuppert.setProxyObj(arg0);
			return arg1.invoke(getTagertObj(), arg2);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private Object tagertObj;
	/**
	 * CGLIB
	 * 当调用目标对象方法时，对目标对象tagertObj进行实例化
	 * @return
	 */
	private Object getTagertObj() {
//		if(tag.getName().contains("ApiConfig")) {
//			log.info("断点");
//		}
		
		if (tagertObj == null) {
			if(StringUtils.isNotBlank(beanName)) {//若存在beanName。则通过beanName查找
				tagertObj = ScanUtil.findBean(beanName);
				LazyBean.processAttr(tagertObj, tagertObj.getClass());//递归注入代理对象
			}else {
				/**
				 * 待优化
				 */
				try {//直接反射构建目标对象
					tagertObj = tag.newInstance();
					LazyBean.processAttr(tagertObj, tag);//递归注入代理对象
				} catch (InstantiationException | IllegalAccessException e) {
					log.error("构建bean=>{}",tag);
					log.error("构建bean异常",e);
				}
			}
		}
		return tagertObj;
	}

}

@Slf4j
class LazyImple implements InvocationHandler {

	private Class tag;
	private Object tagertObj;
	private String beanName;
	private boolean isDbConnect;

	public LazyImple(Class tag) {
		this.tag = tag;
	}
	public LazyImple(Class tag,String beanName) {
		this.tag = tag;
		this.beanName = beanName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			AopContextSuppert.setProxyObj(proxy);
			Object result = method.invoke(getTagertObj(), args);
			if(!isDbConnect) {
				// 处理openSession
				Transactional transactional = method.getAnnotation(Transactional.class);
			}
			LazyMybatisMapperBean.over();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			//throw e;
		}
		return null;
	}
	/**
	 * 接口类型
	 * 当调用目标对象方法时，对目标对象tagertObj进行实例化
	 * @return
	 */
	private Object getTagertObj() {
		if (tagertObj == null) {
			if(LazyDubboBean.isDubbo(tag)) {//，判断是否是Dubbo服务
				tagertObj = LazyDubboBean.buildBean(tag);
			} else {
				if(tag.getPackage().getName().contains(TestUtil.mapperScanPath)) {//判断是否是Mybatis mapper
					//延迟处理
//					tagertObj = LazyMybatisMapperBean.buildBean(tag);
					isDbConnect = true;
					return LazyMybatisMapperBean.buildBean(tag);//防止线程池执行时，出现获取不到session问题
				}else {
					if(beanName == null) {
						/**
						 * 若是本地接口实现类的bean，则进行bean查找。
						 */
						Object tagImp = ScanUtil.findBeanByInterface(tag);
						if(tagImp == null) {
							log.info("未找到本地Bean=>{}",tag);
						}else {
							/**
							 * 实现类是本地Bean
							 */
							tagertObj = tagImp;
							LazyBean.processAttr(tagImp, tagImp.getClass());
						}
					}else {
						// 本地bean
						Object tagImp = ScanUtil.findBean(beanName);
						if(tagImp == null) {
							log.info("未找到本地Bean=>{}",tag);
						}else {
							/**
							 * 实现类是本地Bean
							 */
							tagertObj = tagImp;
							LazyBean.processAttr(tagImp, tagImp.getClass());
						}
					}
				}
			}
		}
		return tagertObj;
	}
}
