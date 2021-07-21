package com.github.jklasd.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
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
	public static final String SPRING_PACKAGE = "org.springframework";
	public static final String BOOT_AUTO_CONFIG = "org.springframework.boot.autoconfigure";
	private static String CLASS_SUFFIX = ".class";
	static Map<String,Class> nameMap = Maps.newHashMap();
	private static PathMatchingResourcePatternResolver resourceResolver;
	
	/**
	 * 扫描路径下资源
	 * @param path classpath:下的文件路径
	 * @return 返回存在的资源路径数组
	 * @throws IOException 读取文件异常
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
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					log.error("未找到类=>{}",p);
				}catch(Exception e) {
					log.error("加载类异常",e);
				}
			}else {
//				log.debug("=============其他文件=={}===========",file);
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
		JunitCountDownLatchUtils.buildCountDownLatch(classNames.stream().filter(cn->cn.contains(scanPath)).collect(Collectors.toList()))
		.runAndWait(name->{
			if(name.endsWith(CLASS_SUFFIX)) {
				name = name.replace("/", ".").replace("\\", ".").replace(".class", "");
				// 查看是否class
				try {
					Class<?> c = Class.forName(name,false,ScanUtil.class.getClassLoader());
					nameMapTmp.put(name,c);
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					if(TestUtil.getInstance().isScanClassPath(name)) {
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
//			log.debug("=============开始加载class=============");
			Resource[] resources = getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" );
//			log.debug("=============加载class={}============",resources.length);
			for (Resource r : resources) {
				URL url = r.getURL();
				if("file".equals(url.getProtocol())) {
					File f = r.getFile();
//					log.debug("=======加载{}内的====class=========",f);
					loadClass(f,url.getFile());
				}else if("jar".equals(url.getProtocol())){
					if(url.getPath().contains("jre/lib")) {
						continue;
					}
//					log.debug("=======加载{}内的====class=========",url.getPath());
					try {
						URLConnection connection = url.openConnection();
						if (connection instanceof JarURLConnection) {
							JarFile jFile = ((JarURLConnection) connection).getJarFile();
							JunitCountDownLatchUtils.buildCountDownLatch(jFile.stream().collect(Collectors.toList())).runAndWait(JarEntry->{
							    String name = JarEntry.getName();
                              if(name.contains(".class")) {
                                  classNames.add(name.replace("/", ".").replace("\\", "."));
                              }else {
                                  if(name.contains("spring.handlers")) {
                                      try {
                                          InputStream is = jFile.getInputStream(JarEntry);
                                          BufferedReader br = new BufferedReader(new InputStreamReader(is));
                                          String line = null;
                                          while((line = br.readLine()) != null) {
                                              String[] url_handler = line.split("=");
                                              Class nameSpaceHandlerC = ScanUtil.loadClass(url_handler[1]);
                                              if(nameSpaceHandlerC!=null) {
                                                  XmlBeanUtil.getInstance().putNameSpace(url_handler[0].replace("\\", ""), nameSpaceHandlerC);
                                              }
                                          }
                                    } catch (IOException e) {
                                         e.printStackTrace();
                                    }
                                  }
                              }
							});
						}
					} catch (Exception e) {
						log.error("不能加载class文件=>{}",url.getPath());
					}
				}
			}
			List<Class<?>> springBoot = findClassWithAnnotation(SpringBootApplication.class);
			springBoot.forEach(startClass ->{
				TestUtil.getInstance().loadScanPath(startClass.getPackage().getName());
				/**
				 * 查看导入资源
				 */
				ImportResource resource = startClass.getAnnotation(ImportResource.class);
				if(resource != null) {
					XmlBeanUtil.getInstance().loadXmlPath(resource.value());
				}
			});
			loadContextPathClass();
			log.info("=============加载class结束=============");
		} catch (IOException e1) {
			log.error("读取文件异常",e1);
		}
	}
	
	public static void loadContextPathClass() {
		JunitCountDownLatchUtils.buildCountDownLatch(classNames.stream().filter(cn->TestUtil.getInstance().isScanClassPath(cn)).collect(Collectors.toList()))
		.runAndWait(name->{
			if(name.endsWith(CLASS_SUFFIX) && !nameMap.containsKey(name)) {
				name = name.replace("/", ".").replace("\\", ".").replace(".class", "");
				// 查看是否class
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
	
	public static Class findClassByName(String beanName) {
		List<Class> list = Lists.newArrayList();
		
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.runAndWait(name ->{
			if (beanName.toLowerCase().equals(name.replace(CLASS_SUFFIX, ""))) {
				list.add(nameMap.get(name));
			} else {
				Class<?> tagClass = nameMap.get(name);
				String annValue = LazyBean.getBeanName(tagClass);
				
				if (Objects.equals(annValue, beanName)) {
					list.add(tagClass);
				}
			}
		});
		return list.isEmpty()?null:list.get(0);
	}
	
	
	public static Boolean isInScanPath(Class<?> requiredType) {
		return nameMap.containsKey(requiredType.getName());
	}
	/**
	 * 扫描继承abstractClass 的类
	 * @param abstractClass 接口
	 * @return 返回继承abstractClass 的类
	 */
	public static List<Class<?>> findClassExtendAbstract(Class abstractClass){
		return findClassExtendAbstract(abstractClass, null,null);
	}
	public static List<Class<?>> findClassExtendAbstract(Class abstractClass,Map<String,Class> classMap,String ClassName){
		Map<String,Class> tmp = Maps.newHashMap();
		if(classMap!=null) {
			tmp.putAll(classMap);
		}
		tmp.putAll(nameMap);
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(tmp.keySet()))
		.runAndWait(name ->{
			if(ClassName!=null && name.equals(ClassName)) {
				return;
			}
			Class<?> tmpClass = tmp.get(name);
			if(isExtends(tmpClass,abstractClass)) {
				if((tmpClass.getAnnotation(Component.class)!=null || tmpClass.getAnnotation(Service.class)!=null)
						&& !Modifier.isAbstract(tmpClass.getModifiers())) {
					list.add(tmpClass);
				}
			}
		});
		return list;
	}
	public static Class findClassImplInterfaceByBeanName(Class interfaceClass,Map<String,Class> classMap,String beanName){
	    if(StringUtils.isBlank(beanName)) {
	        throw new JunitException();
	    }
        Map<String,Class> tmp = Maps.newHashMap();
        if(classMap!=null) {
            tmp.putAll(classMap);
        }
        tmp.putAll(nameMap);
        List<Class> list = Lists.newArrayList();
        JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(tmp.keySet()))
        .runAndWait(name ->{
            Class<?> tmpClass = tmp.get(name);
            if(isImple(tmpClass,interfaceClass)) {
                if(Objects.equals(beanName, LazyBean.getBeanName(tmpClass))) {
                    list.add(tmpClass);
                }
            }
        });
        if(list.isEmpty()) {
            log.warn("没有找到相关实现类========{}======={}==",interfaceClass,beanName);
        }
        return list.isEmpty() ? null : list.get(0);
    }
	public static List<Class<?>> findClassImplInterface(Class interfaceClass,Map<String,Class> classMap,String ClassName){
		Map<String,Class> tmp = Maps.newHashMap();
		if(classMap!=null) {
			tmp.putAll(classMap);
		}
		tmp.putAll(nameMap);
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(tmp.keySet()))
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
	 * @param interfaceClass 接口
	 * @return 返回实现 interfaceClass 的类
	 */
	public static List<Class<?>> findClassImplInterface(Class interfaceClass){
		return findClassImplInterface(interfaceClass, null,null);
	}
	/**
	 * 判断 c 是否是interfaceC的实现类
	 * @param implClass 实现类型
	 * @param interfaceClass 接口类型
	 * @return  true/ false
	 */
	public static boolean isImple(Class implClass,Class<?> interfaceClass) {
		return !implClass.isInterface() && interfaceClass.isAssignableFrom(implClass);
	}
	/**
	 * 判断 subClass 是否继承 abstractClass
	 * @param subClass 子类
	 * @param abstractClass 父类
	 * @return true/false
	 */
	public static boolean isExtends(Class subClass,Class<?> abstractClass) {
		return abstractClass.isAssignableFrom(subClass);
	}
	
	/**
	 * 扫描类 for class
	 * @param annotationType 注解类型
	 * @return 存在 annotationType 注解的类
	 */
	public static List<Class<?>> findClassWithAnnotation(Class<? extends Annotation> annotationType){
		return findClassWithAnnotation(annotationType, nameMap);
	}
	
	public static List<Class<?>> findClassWithAnnotation(Class<? extends Annotation> annotationType,Map<String,Class> nameMapTmp){
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(nameMapTmp.keySet().stream().filter(name->!notFoundSet.contains(name)).collect(Collectors.toList()))
		.setException((name,e)->notFoundSet.add(name))
		.runAndWait(name ->{
			Class<?> c = nameMapTmp.get(name);
//			try {
				Annotation type = c.getDeclaredAnnotation(annotationType);
				if(type != null) {
					list.add(c);
				}
//			} catch (Exception e) {
//				log.error("#findClassWithAnnotation ERROR",e);
//			}
		});
		return list;
	}
	public static List<Class<?>> findStaticMethodClass() {
		Set<Class<?>> list = Sets.newHashSet();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
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
					if(Modifier.isStatic(m.getModifiers())
							&& !m.getName().contains("lambda$")//非匿名方法
						&& !m.getName().contains("access$")) {//非匿名方法
						Class<?> returnType = m.getReturnType();
						if(!returnType.getName().contains("void")) {
							log.debug("method=>{}",m);
							list.add(c);
							return;
						}
//						log.debug(returnType.getName());
					}
				}
			}else if(configuration == null) {
				
			}
		});
		return Lists.newArrayList(list);
	}
	private static Set<String> notFoundSet = Sets.newConcurrentHashSet();
	public synchronized static Object[] findCreateBeanFactoryClass(final AssemblyDTO assemblyData) {
		Map<String,Class> finalNameMap = Maps.newHashMap();
		finalNameMap.putAll(nameMap);
		if(assemblyData.getNameMapTmp() != null) {
			finalNameMap.putAll(assemblyData.getNameMapTmp());
		}
		Object[] address = new Object[2];
		Object[] tmp = new Object[2];
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(finalNameMap.keySet()).stream().filter(name->!notFoundSet.contains(name))
				.collect(Collectors.toList()))
		.setException((name,e)->{
			notFoundSet.add(name);
		}).setError((name,e)->{
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
								if(StringUtils.isNoneBlank(assemblyData.getBeanName())) {
								    String[] beanNames = beanA.value();
							        for(String beanName : beanNames) {
							            if(Objects.equals(beanName, assemblyData.getBeanName())) {
							                address[0]=c;
							                address[1]=m;
							                break;
							            }
							        }
								    beanNames = beanA.name();
							        for(String beanName : beanA.name()) {
							            if(Objects.equals(beanName, assemblyData.getBeanName())) {
							                address[0]=c;
							                address[1]=m;
							                break;
							            }
							        }
								}
								if(tagC.isInterface()?
										(m.getReturnType().isInterface()?
												(ScanUtil.isExtends(m.getReturnType(), tagC) || m.getReturnType() == tagC)
												:ScanUtil.isImple(m.getReturnType(), tagC)
												):
													(ScanUtil.isExtends(m.getReturnType(), tagC) || m.getReturnType() == tagC)) {
									tmp[0] = c;
									tmp[1] = m;
									break;
								}
							}
						}
					}
			}
		});
		if(address[0] ==null || address[1]==null) {
			return tmp;
		}
		return address;
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
	
	public static Resource getRecourceAnyOne(String... paths) throws IOException {
		for(String path: paths) {
			Resource r = getRecource(path);
			if(r!=null && r.exists()) {
				return r;
			}
		}
		return null;
	}
	private static Set<Class> existsProp = Sets.newHashSet();
	public static boolean findCreateBeanForConfigurationProperties(Class tag) {
		if(existsProp.contains(tag))
			return true;
		
		if(tag.getName().contains(SPRING_PACKAGE)) {
			List<Class<?>> list = findClassWithAnnotation(EnableConfigurationProperties.class);
			if(!list.isEmpty()) {
				readPropAnno(tag, list);
			}
			if(!existsProp.contains(tag)) {
				if(tag.getName().contains(BOOT_AUTO_CONFIG)) {
					list = findClassWithAnnotation(EnableConfigurationProperties.class,findClassMap(BOOT_AUTO_CONFIG));
					if(!list.isEmpty()) {
						readPropAnno(tag, list);
					}
				}
				if(!existsProp.contains(tag)) {
					list = findClassWithAnnotation(EnableConfigurationProperties.class,findClassMap(SPRING_PACKAGE));
					if(!list.isEmpty()) {
						readPropAnno(tag, list);
					}
				}
			}
		}
		return existsProp.contains(tag);
	}
	private static boolean readPropAnno(Class tag, List<Class<?>> list) {
		for(Class<?> enablePropC : list) {
			EnableConfigurationProperties ecp = (EnableConfigurationProperties) enablePropC.getDeclaredAnnotation(EnableConfigurationProperties.class);
			for(Class c : ecp.value()) {
				if(c == tag) {
					existsProp.add(tag);
					return true;
				}
			}
		}
		return false;
	}
	public static Class loadClass(String className) {
		try {
			Class classObj = Class.forName(className, false, JavaBeanUtil.class.getClassLoader());
			return classObj;
		} catch (ClassNotFoundException e) {
			log.warn("#loadClass=>{}",className);
		}
		return null;
	}
	
	public static  boolean isBasicClass(Class cal){
		return cal == Integer.class || cal == int.class
				|| cal == Boolean.class || cal == boolean.class
				|| cal == Short.class || cal == short.class
				|| cal == Double.class || cal == double.class
				|| cal == Long.class || cal == long.class
				|| cal == Float.class || cal == float.class;
	}
    public static Type[] getGenericType(Class<?> tagClass) {
        for(Type t:tagClass.getGenericInterfaces()) {
            if(t instanceof ParameterizedType) {
                ParameterizedType  pType = (ParameterizedType) t;
                return pType.getActualTypeArguments();
            }
        }
        return null;
    }
}