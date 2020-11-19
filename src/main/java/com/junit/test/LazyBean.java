package com.junit.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.junit.util.CountDownLatchUtils;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 * 2020-11-19
 */
@Slf4j
public class LazyBean {

	private static String welab;

	public static Map<Class, Object> singleton = Maps.newHashMap();
	/**
	 * 构建代理对象
	 * @param classBean
	 * @return
	 */
	public static Object buildProxy(Class classBean) {
		Object obj = buildRabbit(classBean);
		if (obj != null) {
			return obj;
		}
		if (classBean.isInterface()) {
			InvocationHandler handler = new LazyImple(classBean);
			return Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] { classBean }, handler);

		} else {
			MethodInterceptor handler = new LazyCglib(classBean);
			return Enhancer.create(classBean, handler);
		}
	}
	/**
	 * 构建rabbit应用
	 * @param classBean
	 * @return
	 */
	private static RabbitTemplate obj = null;
	private static Object buildRabbit(Class classBean) {
		if (classBean == RabbitTemplate.class) {
			if (obj != null) {
				return obj;
			}
			try {
				obj = (RabbitTemplate) classBean.newInstance();
				// 定义一个连接工厂
				ConnectionFactory factory = new ConnectionFactory();
				// 设置服务端地址（域名地址/ip）
				factory.setHost(TestUtil.getValue("spring.rabbitmq.host"));
				// 设置服务器端口号
				factory.setPort(5672);
				// 设置虚拟主机(相当于数据库中的库)
				factory.setVirtualHost("/");
				// 设置用户名
				factory.setUsername(TestUtil.getValue("spring.rabbitmq.username"));
				// 设置密码
				factory.setPassword(TestUtil.getValue("spring.rabbitmq.password"));
				Connection connection = factory.newConnection();
				obj.setConnectionFactory(new CachingConnectionFactory(factory));

				return obj;
			} catch (InstantiationException | IllegalAccessException | IOException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (classBean == RabbitMessagingTemplate.class) {
			try {
				RabbitMessagingTemplate objM = (RabbitMessagingTemplate) classBean.newInstance();
				objM.setRabbitTemplate(obj == null ? (RabbitTemplate) buildProxy(RabbitTemplate.class) : obj);
				return objM;
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	public static void setObj(Field f,Object obj,Object proxyObj) {
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
	public static void processAttr(Object obj, Class objClassOrSuper) {
		Field[] fields = objClassOrSuper.getDeclaredFields();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(fields)).runAndWait(f->{
			Autowired aw = f.getAnnotation(Autowired.class);
			if (aw != null) {
//				if(f.getName().equals("productConfigService")) {
//					log.info("需要注入=>{}=>{}",f.getName(),f.getType().getName());
//				}
				String className = f.getType().getName();
				if (className.contains("Mapper")) {
					try {
						if (!f.isAccessible()) {
							f.setAccessible(true);
						}
						Object value = f.get(obj);
						if (value == null) {
							setObj(f, obj, TestUtil.getExistBean(f.getType(), f.getName()));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (className.contains("Impl") || className.contains("Service")) {
					Object proxyObj = null;
					if (!singleton.containsKey(f.getType())) {
						proxyObj = buildProxy(f.getType());
						singleton.put(f.getType(), proxyObj);
					}
					setObj(f, obj, singleton.get(f.getType()));
				} else if (className.contains("jedis")) {
					setObj(f, obj, TestUtil.getExistBean(f.getType(), f.getName()));
				} else {
					if(f.getType() == List.class) {
						ParameterizedType t = (ParameterizedType) f.getGenericType();
						Type[] item = t.getActualTypeArguments();
						if(item.length == 1) {
							//处理一个集合注入
							try {
								Class<?> c = Class.forName(item[0].getTypeName());
								setObj(f, obj, ScanUtil.findListBean(c));
								System.out.println("注入成功");
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}
						}else {
							System.out.println("其他特殊情况");
						}
					}else {
						Object proxyObj = null;
						if (!singleton.containsKey(f.getType())) {
							proxyObj = buildProxy(f.getType());
							singleton.put(f.getType(), proxyObj);
						}
						setObj(f, obj, singleton.get(f.getType()));
					}
				}
			} else {
				Value v = f.getAnnotation(Value.class);
				if (v != null) {
					setObj(f, obj, TestUtil.value(v.value().replace("${", "").replace("}", ""), f.getType()));
				} else {
					Component c = f.getAnnotation(Component.class);
					if (c != null) {

					} else {
//						log.info("不需要需要注入=>{}", f.getName());
					}
				}
			}
		});
		Class superC = objClassOrSuper.getSuperclass();
		if (superC != null) {
			processAttr(obj, superC);
		}

		Method[] ms = obj.getClass().getDeclaredMethods();
		for (Method m : ms) {
			if (m.getAnnotation(PostConstruct.class) != null) {
				try {
					m.invoke(obj, null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static String getWelab() {
		if(welab == null) {
			welab = "com."+TestUtil.getValue("app.id").replace("-", ".");
		}
		return welab;
	}
	public static void processStatic(Class c) {
		Object obj = buildProxy(c);
		Method[] ms = c.getDeclaredMethods();
		for (Method m : ms) {
			if (m.getAnnotation(PostConstruct.class) != null) {
				try {
					m.invoke(obj, null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class LazyCglib implements MethodInterceptor {
	private Class tag;

	public LazyCglib(Class tag) {
		this.tag = tag;
	}

	@Override
	public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
		try {
			return arg1.invoke(getTagertObj(), arg2);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private Object tagertObj;

	private Object getTagertObj() {
		if (tagertObj == null) {
			try {
				tagertObj = tag.newInstance();
				LazyBean.processAttr(tagertObj, tag);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return tagertObj;
	}

}

@Slf4j
class LazyImple implements InvocationHandler {

	private Class tag;
	private Object tagertObj;

	public LazyImple(Class tag) {
		this.tag = tag;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke(getTagertObj(), args);
		} catch (Exception e) {
			e.printStackTrace();
			//throw e;
		}
		return null;
	}

	private Object getTagertObj() {
		if (tagertObj == null) {
			if (!tag.getName().contains(LazyBean.getWelab())) {
				// 设定为dubbo
				tagertObj = buildDubboService(tag);
			} else {
				if(tag.getName().contains("Mapper")) {
					//延迟处理
					tagertObj = TestUtil.getExistBean(tag);
				}else {
					// 本地bean
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
				}
			}
		}
		return tagertObj;
	}
	
	private static RegistryConfig registryConfig;
	
	private static Map<Class<?>,Object> dubboSingle = Maps.newHashMap();
	public static Object buildDubboService(Class<?> dubboClass) {
		if(dubboSingle.containsKey(dubboClass)) {
			return dubboSingle.get(dubboClass);
		}
		ReferenceConfig<?> referenceConfig = new ReferenceConfig<>();
		referenceConfig.setInterface(dubboClass);
		String groupStr = dubboClass.getName().replace("com.welab.", "");
		String[] keys = groupStr.split("\\.");
		for (String k : keys) {
			String v = TestUtil.getValue("dubbo.group." + k);
			if (v != null) {
				groupStr = v;
				log.info("{}=>{}", dubboClass.getName(), groupStr);
				break;
			}
		}
		referenceConfig.setGroup(groupStr);
		ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-examples-consumer");
		if(registryConfig == null) {
			registryConfig = new RegistryConfig("zookeeper://" + TestUtil.getValue("zookeeper.url"));
			registryConfig.setUsername(TestUtil.getValue("zookeeper.username"));
			registryConfig.setPassword(TestUtil.getValue("zookeeper.password"));
			registryConfig.setClient("curator");
			registryConfig.setSubscribe(true);
			registryConfig.setRegister(false);
		}
		referenceConfig.setApplication(applicationConfig);
		referenceConfig.setRegistry(registryConfig);
		Object obj = referenceConfig.get();
		dubboSingle.put(dubboClass,obj);
		return obj;
	}

}
@Slf4j
class ScanUtil{
	
	static Map<String,Class> nameMap = Maps.newHashMap();
	private static void loadClass(File file) throws ClassNotFoundException {
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				loadClass(f);
			} else if (f.getName().endsWith(".class")) {
				String p = f.getPath();
				p = p.substring(p.indexOf(LazyBean.getWelab().replace(".", "\\"))).replace("\\", ".").replace(".class", "");
				// 查看是否class
				Class<?> c = Class.forName(p);
				nameMap.put(p,c);
			}else {
				log.info("=============其他文件=============");
			}
		}
	}
	public static void loadAllClass() {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver()
					.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" + LazyBean.getWelab().replace(".", "/"));
			for (Resource r : resources) {
				loadClass(r.getFile());
			}
		} catch (IOException | ClassNotFoundException e1) {
			e1.printStackTrace();
		}
	}
	public static Map<String,Object> beanMaps = Maps.newHashMap();
	public static Object findBean(String beanName) {
		if(beanMaps.containsKey(beanName)) {
			return beanMaps.get(beanName);
		}
		Object bean = null;
		Class tag = findClassByName(beanName);
		if (tag != null) {
			if(tag.isInterface() && !tag.getName().contains(LazyBean.getWelab())) {
				bean = LazyImple.buildDubboService(tag);
			}else {
				bean = LazyBean.buildProxy(tag);
			}
			beanMaps.put(beanName, bean);
		}
		return bean;
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Class findClassByName(String beanName) {
		List<Class> list = Lists.newArrayList();
		
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			if (beanName.toLowerCase().equals(name.replace(".class", ""))) {
				list.add(nameMap.get(name));
			} else {
				Class c = nameMap.get(name);
				Service ann = (Service) c.getAnnotation(Service.class);
				Component cAnn = (Component)c.getAnnotation(Component.class);
				if (ann != null) {
					if (Objects.equals(ann.value(), beanName)) {
						list.add(c);
					}
				}else if(cAnn != null) {
					if (Objects.equals(cAnn.value(), beanName)) {
						list.add(c);
					}
				}
			}
		});
		return list.isEmpty()?null:list.get(0);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List findListBean(Class<?> requiredType) {
		List list = Lists.newArrayList();
		if(requiredType.isInterface()) {
			List<Class> tags = findClassByInterface(requiredType);
			if (!tags.isEmpty()) {
				tags.stream().forEach(item ->list.add(LazyBean.buildProxy(item)));
			}
		}else {
			/**
			 * @TODO 存在要查继承类问题
			 */
			Class<?> tag = findClass(requiredType);
			if (tag != null) {
				list.add(LazyBean.buildProxy(tag));
			}
		}
		return list;
	}
	public static Object findBean(Class<?> requiredType) {
		if(requiredType.isInterface() && !requiredType.getName().contains(LazyBean.getWelab())) {
			return LazyImple.buildDubboService(requiredType);
		}
		
		if(requiredType.isInterface()) {
			List<Class> tag = findClassByInterface(requiredType);
			if (!tag.isEmpty()) {
				return LazyBean.buildProxy(tag.get(0));
			}
		}else {
			Class tag = findClass(requiredType);
			if (tag != null) {
				return LazyBean.buildProxy(tag);
			}
		}
		return null;
	}
	private static Class findClass(Class<?> requiredType) {
		List<Class> list = Lists.newArrayList();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			Class<?> c = nameMap.get(name);
			if(c == requiredType) {
				if(c.getAnnotation(Component.class)!=null ||
						c.getAnnotation(Service.class)!=null ) {
					list.add(c);
				}
			}
		});
		return list.isEmpty()?null:list.get(0);
	}
	/**
	 * 扫描类 for bean
	 * @param file
	 * @param beanName
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Object findBeanByInterface(Class interfaceClass) {
		List<Class> tags = findClassByInterface(interfaceClass);
		if (!tags.isEmpty()) {
			return LazyBean.buildProxy(tags.get(0));
		}
		return null;
	}
	/**
	 * 扫描类 for class
	 * @param file
	 * @param interfaceClass
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static List<Class> findClassByInterface(Class interfaceClass){
		List<Class> list = Lists.newArrayList();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			Class<?> c = nameMap.get(name);
			if(isImple(c,interfaceClass)) {
				if(c.getAnnotation(Component.class)!=null ||
						c.getAnnotation(Service.class)!=null ) {
					list.add(c);
				}
			}
		});
		return list;
	}
	public static boolean isImple(Class c,Class interfaceC) {
		Class[] ics = c.getInterfaces();
		for(Class c2 : ics) {
			if(c2 == interfaceC) {
				return true;
			}
		}
		Class sc = c.getSuperclass();
		if(sc!=null) {
			return isImple(sc, interfaceC);
		}
		return false;
	}
}