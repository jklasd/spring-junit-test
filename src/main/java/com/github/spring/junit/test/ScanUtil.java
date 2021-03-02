package com.github.spring.junit.test;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.github.spring.junit.test.dubbo.LazyDubboBean;
import com.github.spring.junit.test.spring.JavaBeanUtil;
import com.github.spring.junit.test.spring.ObjectProviderImpl;
import com.github.spring.junit.test.spring.XmlBeanUtil;
import com.github.spring.junit.util.CountDownLatchUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class ScanUtil {
	public static final String BOOT_AUTO_CONFIG = "org.springframework.boot.autoconfigure";
	private static String CLASS_SUFFIX = ".class";
	static Map<String,Class> nameMap = Maps.newHashMap();
	private static PathMatchingResourcePatternResolver resourceResolver;
//	private static OverridingClassLoader springClassLoader = new OverridingClassLoader(ScanUtil.class.getClassLoader());
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
	public static boolean exists(Class record) {
		return nameMap.values().contains(record);
	}
	private static void loadClass(File file,String rootPath){
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				loadClass(f,rootPath);
			} else if (f.getName().endsWith(CLASS_SUFFIX)) {
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
				log.debug("=============其他文件=={}===========",file);
			}
		}
	}
	private static Set<String> classNames = Sets.newHashSet();
	public static Map<String,Map<String,Class>> pathForClass = Maps.newHashMap();
	
	public static Map<String, Class> findClassMap(String scanPath) {
		if(pathForClass.containsKey(scanPath)) {
			return pathForClass.get(scanPath);
		}
		Map<String,Class> nameMapTmp = Maps.newHashMap();
		CountDownLatchUtils.buildCountDownLatch(classNames.stream().filter(cn->cn.contains(scanPath)).collect(Collectors.toList()))
		.runAndWait(name->{
			if(name.endsWith(CLASS_SUFFIX)) {
				name = name.replace("/", ".").replace("\\", ".").replace(".class", "");
				// 查看是否class
				try {
					Class<?> c = Class.forName(name,false,ScanUtil.class.getClassLoader());
					
					nameMapTmp.put(name,c);
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					if(TestUtil.isScanClassPath(name)) {
						log.error("加载{}=>未找到类{}",name,e.getMessage());
					}
				}catch(Error e) {
					log.error("未找到类{}=>{}",name,e.getMessage());
				}
			}
		});
		pathForClass.put(scanPath, nameMapTmp);
		return nameMapTmp;
	}
	
	private static boolean init = false;
	/**
	 * 加载所有class，缓存起来
	 * 类似加载 AbstractEmbeddedServletContainerFactory
	 */
	@SuppressWarnings("resource")
	public static void loadAllClass() {
		try {
			if(init) {
				return;
			}
			init = true;
			log.debug("=============开始加载class=============");
			Resource[] resources = getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" );
			log.debug("=============加载class={}============",resources.length);
			for (Resource r : resources) {
				URL url = r.getURL();
				if("file".equals(url.getProtocol())) {
					File f = r.getFile();
					log.debug("=======加载{}内的====class=========",f);
					loadClass(f,url.getFile());
				}else if("jar".equals(url.getProtocol())){
					if(url.getPath().contains("jre/lib")) {
						continue;
					}
					log.debug("=======加载{}内的====class=========",url.getPath());
					try {
						URLConnection connection = url.openConnection();
						if (connection instanceof JarURLConnection) {
							JarFile jFile = ((JarURLConnection) connection).getJarFile();
							Enumeration<JarEntry> jarEntrys = jFile.entries();
							while (jarEntrys.hasMoreElements()) {
								String name = jarEntrys.nextElement().getName();
								classNames.add(name.replace("/", ".").replace("\\", "."));
							}
						}
					} catch (Exception e) {
						log.error("不能加载class文件=>{}",url.getPath());
					}
				}
			}
			List<Class<?>> springBoot = findClassWithAnnotation(SpringBootApplication.class);
			springBoot.forEach(startClass ->{
				TestUtil.loadScanPath(startClass.getPackage().getName());
				/**
				 * 查看导入资源
				 */
				ImportResource resource = startClass.getAnnotation(ImportResource.class);
				if(resource != null) {
					XmlBeanUtil.loadXmlPath(resource.value());
				}
			});
			loadContextPathClass();
			log.info("=============加载class结束=============");
		} catch (IOException e1) {
			log.error("读取文件异常",e1);
		}
	}
	
	public static void loadContextPathClass() {
		CountDownLatchUtils.buildCountDownLatch(classNames.stream().filter(cn->TestUtil.isScanClassPath(cn)).collect(Collectors.toList()))
		.runAndWait(name->{
			if(name.endsWith(CLASS_SUFFIX) && !nameMap.containsKey(name)) {
				name = name.replace("/", ".").replace("\\", ".").replace(".class", "");
				// 查看是否class
//				if(name.contains("RedisAutoConfiguration")) {
//					log.info("断点");
//				}
				try {
					Class<?> c = ScanUtil.class.getClassLoader().loadClass(name);
					nameMap.put(name,c);
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					log.error("加载{}=>未找到类{}",name,e.getMessage());
				}catch(Error e) {
					log.error("未找到类{}=>{}",name,e.getMessage());
				}
			}
		});
	}
	
	public static Map<String,Object> beanMaps = Maps.newHashMap();
	/**
	 * 
	 * @param beanName
	 * @param type
	 * @return
	 */
	public static Object findBean(String beanName,Class<?> type) {
		if(type.isInterface()) {
			List<Class> classList = findClassImplInterface(type);
			for(Class c : classList) {
				Service ann = (Service) c.getAnnotation(Service.class);
				Component cAnn = (Component)c.getAnnotation(Component.class);
				if(ann!=null && ann.value().equals(beanName)) {
					
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
			if(name.replace(CLASS_SUFFIX, "").endsWith(beanName.substring(0, 1).toUpperCase()+beanName.substring(1))) {
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
			if (beanName.toLowerCase().equals(name.replace(CLASS_SUFFIX, ""))) {
				list.add(nameMap.get(name));
			} else {
				Class tagClass = nameMap.get(name);
				Service sAnn = (Service) tagClass.getAnnotation(Service.class);
				Component cAnn = (Component)tagClass.getAnnotation(Component.class);
				
				String annValue = null;
				if (sAnn != null) {
					annValue = sAnn.value();
				}else if(cAnn != null) {
					annValue = cAnn.value();
				}
				
				if (Objects.equals(annValue, beanName)) {
					list.add(tagClass);
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
			List<Class> tags = findClassImplInterface(requiredType);
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
			List<Class> tag = findClassImplInterface(requiredType);
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
		if(interfaceClass.getPackage().getName().startsWith("org.springframework")) {
			Object obj = TestUtil.getExistBean(interfaceClass, null);
			if(obj == null) {
				List<Class> cL = ScanUtil.findClassImplInterface(interfaceClass,findClassMap("org.springframework"),null);
				if(!cL.isEmpty()) {
					Class c = cL.get(0);
				}
			}
			return obj;
		}
		List<Class> tags = findClassImplInterface(interfaceClass);
		if (!tags.isEmpty()) {
			return LazyBean.buildProxy(tags.get(0));
		}
		return null;
	}
	private static List<Class> findClassImplInterface(Class interfaceClass,Map<String,Class> classMap,String ClassName){
		Map<String,Class> tmp = Maps.newHashMap();
		if(classMap!=null) {
			tmp.putAll(classMap);
		}
		tmp.putAll(nameMap);
		List<Class> list = Lists.newArrayList();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(tmp.keySet()))
		.runAndWait(name ->{
			if(ClassName!=null && name.equals(ClassName)) {
				return;
			}
			Class<?> tmpClass = tmp.get(name);
			if(isImple(tmpClass,interfaceClass)) {
				if((tmpClass.getAnnotation(Component.class)!=null || tmpClass.getAnnotation(Service.class)!=null)
						&& !Modifier.isAbstract(tmpClass.getModifiers())) {
					list.add(tmpClass);
				}
			}
		});
		return list;
	}
	/**
	 * 扫描实现了interfaceClass 的类
	 * @param file
	 * @param interfaceClass
	 * @return
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("rawtypes")
	private static List<Class> findClassImplInterface(Class interfaceClass){
		return findClassImplInterface(interfaceClass, null,null);
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
	public static boolean isExtends(Class c,Class abstractC) {
		if(c.isInterface()) {
			Class[] interfaces = c.getInterfaces();
			for(Class item : interfaces) {
				if(item == abstractC || isExtends(item, abstractC)) {
					return true;
				}
			}
		}else {
			if(c.getSuperclass() == abstractC) {
				return true;
			}else if(c.getSuperclass() != null){
				return isExtends(c.getSuperclass(), abstractC);
			}
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
	public static Map<String, Object> findBeanWithAnnotation(Class<? extends Annotation> annotationType) {
		List<Class<?>> list = findClassWithAnnotation(annotationType);
		Map<String, Object> annoClass = Maps.newHashMap();
		list.stream().forEach(c ->{
//			String beanName = getBeanName(c);
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
	public static List<Class<?>> findClassWithAnnotation(Class<? extends Annotation> annotationType){
		return findClassWithAnnotation(annotationType, nameMap);
	}
	
	public static List<Class<?>> findClassWithAnnotation(Class<? extends Annotation> annotationType,Map<String,Class> nameMapTmp){
		List<Class<?>> list = Lists.newArrayList();
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMapTmp.keySet()))
		.runAndWait(name ->{
			Class<?> c = nameMapTmp.get(name);
			try {
				Annotation type = c.getDeclaredAnnotation(annotationType);
				if(type != null) {
					list.add(c);
				}
			} catch (Exception e) {
				log.error("#findClassWithAnnotation ERROR");
				throw e;
			}
		});
		return list;
	}
	public static Boolean isBean(Class beanC) {
		Boolean[] address = new Boolean[] {false};
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			Class<?> c = nameMap.get(name);
			if(beanC == c) {
				Annotation comp = c.getAnnotation(Component.class);
				Annotation service = c.getAnnotation(Service.class);
				Annotation configuration = c.getAnnotation(Configuration.class);
				if(comp != null
						|| service != null
						|| configuration != null) {
					address[0] = true;
				}
			}
		});
		return address[0];
	}
	public static List<Class<?>> findStaticMethodClass() {
		Set<Class<?>> list = Sets.newHashSet();
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
						Class<?> returnType = m.getReturnType();
						if(!returnType.getName().contains("void")) {
							list.add(c);
							return;
						}
						log.debug(returnType.getName());
					}
				}
			}else if(configuration == null) {
				
			}
		});
		return Lists.newArrayList(list);
	}
	private static Set<String> notFoundSet = Sets.newConcurrentHashSet();
	public static Object[] findCreateBeanFactoryClass(final AssemblyUtil assemblyData) {
		Map<String,Class> finalNameMap = Maps.newHashMap();
		finalNameMap.putAll(nameMap);
		if(assemblyData.getNameMapTmp() != null) {
			finalNameMap.putAll(assemblyData.getNameMapTmp());
		}
		Object[] address = new Object[2];
		CountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(finalNameMap.keySet()).stream().filter(name->!notFoundSet.contains(name))
				.collect(Collectors.toList()))
		.setException((name,e)->{
//			log.info("TypeNotPresentExceptionProxy Exception=>"+name);
			notFoundSet.add(name);
		}).setError((name,e)->{
//			log.info("TypeNotPresentExceptionProxy Error=>"+name);
			notFoundSet.add(name);
		}).runAndWait(name ->{
//			if(name.contains("RabbitTemplateConfiguration")) {
//				log.info("断点");
//			}
			Class<?> c = finalNameMap.get(name);
			if(/*Modifier.isPublic(c.getModifiers()) && */!c.isInterface()) {
					Configuration configuration = c.getDeclaredAnnotation(Configuration.class);
					if(configuration != null) {
						Method[] methods = c.getDeclaredMethods();
						for(Method m : methods) {
							Bean beanA = m.getAnnotation(Bean.class);
							if(beanA != null) {
								Class tagC = assemblyData.getTagClass();
								if(tagC.isInterface()?
										(m.getReturnType().isInterface()?
												(ScanUtil.isExtends(m.getReturnType(), tagC) || m.getReturnType() == tagC)
												:ScanUtil.isImple(m.getReturnType(), tagC)
												):
													(ScanUtil.isExtends(m.getReturnType(), tagC) || m.getReturnType() == tagC)) {
									address[0]=c;
									address[1]=m;
									break;
								}
							}
						}
					}
			}
		});
		return address;
	}
	@SuppressWarnings("rawtypes")
	public static Object findCreateBeanFromFactory(Class classBean, String beanName) {
		AssemblyUtil asse = new AssemblyUtil();
		asse.setTagClass(classBean);
		asse.setBeanName(beanName);
		if(classBean.getName().startsWith("org.springframework")) {
			Object tmpObj = findCreateBeanFromFactory(asse);
			if(tmpObj!=null) {
				return tmpObj;
			}
			asse.setNameMapTmp(findClassMap("org.springframework"));
		}
		return findCreateBeanFromFactory(asse);
	}
	public static Object findCreateBeanFromFactory(AssemblyUtil assemblyData) {
		Object[] ojb_meth = findCreateBeanFactoryClass(assemblyData);
		if(ojb_meth[0] ==null || ojb_meth[1]==null) {
			return null;
		}
		Object tagObj = JavaBeanUtil.buildBean((Class)ojb_meth[0],(Method)ojb_meth[1],assemblyData);
		return tagObj;
	}
//	public static Object findCreateBeanFromFactory(Class classBean, String beanName,Map<String,Class> tmpBeanMap) {
//		Object[] ojb_meth = findCreateBeanFactoryClass(classBean, beanName,tmpBeanMap);
//		if(ojb_meth[0] ==null || ojb_meth[1]==null) {
//			return null;
//		}
//		Object tagObj = JavaBeanUtil.buildBean((Class)ojb_meth[0],(Method)ojb_meth[1],classBean,beanName,tmpBeanMap);
//		return tagObj;
//	}
	public static Resource getRecource(String location) throws IOException {
		Resource[] rs = getResources(location);
		return rs.length>0?rs[0]:null;
	}
	public static Class getClassByName(String className) {
		return nameMap.get(className);
	}
	public static Object findBeanByInterface(Class interfaceClass, Type[] classGeneric) {
		if(classGeneric == null) {
			return findBeanByInterface(interfaceClass);
		}
		if(interfaceClass.getName().startsWith("org.springframework")) {
			List<Class> cL = ScanUtil.findClassImplInterface(interfaceClass,findClassMap("org.springframework"),null);
			if(!cL.isEmpty()) {
				Class c = cL.get(0);
			}else {
				if(interfaceClass == ObjectProvider.class) {
					return new ObjectProviderImpl(classGeneric[0]);
				}
//				TestUtil.getExistBean(interfaceClass,classGeneric);
			}
		}
		return null;
	}
	public static Resource getRecourceAnyOne(String... paths) throws IOException {
		// TODO Auto-generated method stub
		for(String path: paths) {
			Resource r = getRecource(path);
			if(r!=null && r.exists()) {
				return r;
			}
		}
		return null;
	}
}