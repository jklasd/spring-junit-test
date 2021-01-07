package com.junit.test;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.junit.test.dubbo.LazyDubboBean;
import com.junit.util.CountDownLatchUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScanUtil{
	
	static Map<String,Class> nameMap = Maps.newHashMap();
	private static PathMatchingResourcePatternResolver resourceResolver;
	/**
	 * 扫描路径下资源
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static Resource[] getResources(String path) throws IOException {
		if(resourceResolver == null) {
			resourceResolver = new PathMatchingResourcePatternResolver(); 
		}
		return resourceResolver.getResources(path);
	}
	private static void loadClass(File file,String rootPath){
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				loadClass(f,rootPath);
			} else if (f.getName().endsWith(".class")) {
				String p = f.getPath();
				File tmp = new File(rootPath);
				p = p.replace(tmp.getPath()+"\\", "").replace(tmp.getPath()+"/", "").replace("/", ".").replace("\\", ".").replace(".class", "");
				// 查看是否class
				try {
					Class<?> c = TestUtil.class.getClassLoader().loadClass(p);
					nameMap.put(p,c);
				} catch (ClassNotFoundException e) {
					log.error("未找到类=>{}",p);
				}catch(Exception e) {
					log.error("加载类异常",e);
				}
			}else {
//				log.info("=============其他文件=={}===========",file);
			}
		}
	}
	/**
	 * 加载所有class，缓存起来
	 */
	public static void loadAllClass() {
		try {
			Resource[] resources = getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" );
			for (Resource r : resources) {
				URL url = r.getURL();
				if(!url.getPath().contains(".jar")) {
					File f = r.getFile();
					loadClass(f,url.getFile());
				}
			}
		} catch (IOException e1) {
			log.error("读取文件异常",e1);
		}
	}
	public static Map<String,Object> beanMaps = Maps.newHashMap();
	/**
	 * 通过BeanName 获取bean
	 * @param beanName
	 * @return
	 */
	public static Object findBean(String beanName) {
		if(beanMaps.containsKey(beanName)) {
			return beanMaps.get(beanName);
		}
		Object bean = null;
		Class tag = findClassByName(beanName);
		if(tag == null) {
			tag = findClassByClassName(beanName.substring(0, 1).toUpperCase()+beanName.substring(1));
		}
		if (tag != null) {
			if(LazyDubboBean.isDubbo(tag)) {
				return LazyDubboBean.buildBean(tag);
			}else {
				bean = LazyBean.buildProxy(tag);
			}
			beanMaps.put(beanName, bean);
		}
		return bean;
	}
	private static Class findClassByClassName(String beanName) {
		List<Class> list = Lists.newArrayList();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			if(name.replace(".class", "").endsWith(beanName.substring(0, 1).toUpperCase()+beanName.substring(1))) {
				list.add(nameMap.get(name));
			}
		});
		return list.isEmpty()?null:list.get(0);
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
	/**
	 * 通过class 查找它的所有继承者或实现者
	 * @param requiredType
	 * @return
	 */
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
	/**
	 * 通过class 获取 bean
	 * @param requiredType
	 * @return
	 */
	public static Object findBean(Class<?> requiredType) {
		if(LazyDubboBean.isDubbo(requiredType)) {
			return LazyDubboBean.buildBean(requiredType);
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
//		if(interfaceClass.getName().contains("ApplicationService")) {
//			log.info("断点");
//		}
		List<Class> list = Lists.newArrayList();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			Class<?> tmpClass = nameMap.get(name);
			if(isImple(tmpClass,interfaceClass)) {
				if((tmpClass.getAnnotation(Component.class)!=null || tmpClass.getAnnotation(Service.class)!=null)
						&& !Modifier.isAbstract(tmpClass.getModifiers())) {
					list.add(tmpClass);
				}
			}else if(interfaceClass.getPackage().getName().contains(TestUtil.mapperScanPath)
					&& tmpClass == interfaceClass) {
				list.add(tmpClass);
			}
		});
		return list;
	}
	/**
	 * 判断 c 是否是interfaceC的实现类
	 * @param c
	 * @param interfaceC
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
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
	/**
	 * 
	 * 通过注解查找Bean
	 * 
	 * @param annotationType
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, Object> findBeanWithAnnotation(Class<? extends Annotation> annotationType) {
		List<Class> list = findClassWithAnnotation(annotationType);
		Map<String, Object> annoClass = Maps.newHashMap();
		list.stream().forEach(c ->{
			String beanName = getBeanName(c);
			annoClass.put(c.getSimpleName(), LazyBean.buildProxy(c));
		});
		return annoClass;
	}
	/**
	 * 获取BeanName
	 * @param c
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getBeanName(Class c) {
		String beanName = null;
		if(c.getAnnotation(Component.class)!=null) {
			beanName = ((Component)c.getAnnotation(Component.class)).value();
		}else if(	c.getAnnotation(Service.class)!=null ) {
			beanName = ((Service)c.getAnnotation(Service.class)).value();
		}/*else if(	c.getAnnotation(Configuration.class)!=null ) {
			beanName = ((Configuration)c.getAnnotation(Configuration.class)).value();
		}*/
//		if(StringUtils.isBlank(beanName)){
//			beanName = c.getSimpleName().substring(0, 1).toLowerCase()+c.getSimpleName().substring(1);
//		}
		return beanName;
	}
	
	/**
	 * 扫描类 for class
	 * @param file
	 * @param interfaceClass
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static List<Class> findClassWithAnnotation(Class<? extends Annotation> annotationType){
		List<Class> list = Lists.newArrayList();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			Class<?> c = nameMap.get(name);
			Annotation type = c.getDeclaredAnnotation(annotationType);
			if(type != null) {
				list.add(c);
			}
		});
		return list;
	}
	
	public static List<Class> findStaticMethodClass() {
		Set<Class> list = Sets.newHashSet();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			Class<?> c = nameMap.get(name);
			Annotation comp = c.getAnnotation(Component.class);
			Annotation service = c.getAnnotation(Service.class);
			Annotation configuration = c.getAnnotation(Configuration.class);
			if(comp != null
					|| service != null
					|| configuration != null) {
				Method[] methods = c.getDeclaredMethods();
				for(Method m : methods) {
					if(Modifier.isStatic(m.getModifiers())) {
						Class returnType = m.getReturnType();
						if(!returnType.getName().contains("void")) {
							list.add(c);
							return;
						}
//						log.info(returnType.getName());
					}
				}
			}
		});
		return Lists.newArrayList(list);
	}
}