package com.github.jklasd.test.core.facade.scan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.github.jklasd.test.core.facade.Scan;
import com.github.jklasd.test.core.facade.loader.AnnotationResourceLoader;
import com.github.jklasd.test.core.facade.loader.XMLResourceLoader;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;
import com.github.jklasd.test.util.BeanNameUtil;
import com.github.jklasd.test.util.CheckUtil;
import com.github.jklasd.test.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassScan implements Scan{

	private boolean init;
	private static Set<String> classNames = Sets.newConcurrentHashSet();
	Map<String,Class<?>> componentClassPathMap = Maps.newConcurrentMap();
	private static String CLASS_SUFFIX = ".class";
	
	private JunitResourceLoader xmlResourceLoader = XMLResourceLoader.getInstance();
	private JunitResourceLoader annoResourceLoader = AnnotationResourceLoader.getInstance();
	
	private ClassLoader classLoader = JavaBeanUtil.class.getClassLoader();
	
	public void loadComponentClass(Class<?> c) {
		componentClassPathMap.put(c.getName(), c);
	}
	
	@Override
	public void scan() {
		if(init) {
			return;
		}
		init = true;
		log.debug("=========加载class========");
		AtomicBoolean loadFaile = new AtomicBoolean();
		try {
			Resource[] resources = ScanUtil.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" );
			JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(resources)).forEach(r->{
				try {
					URL url = r.getURL();
					if("file".equals(url.getProtocol())) {
						File f = r.getFile();
						loadClass(f,url.getFile());
					}else if("jar".equals(url.getProtocol())){
						if(url.getPath().contains("jre/lib")) {
							return;
						}
						try {
							URLConnection connection = url.openConnection();
							if (connection instanceof JarURLConnection) {
								JarFile jFile = ((JarURLConnection) connection).getJarFile();
								if(jFile.getName().contains("util")) {
									log.debug("断点");
								}
								JunitCountDownLatchUtils.buildCountDownLatch(jFile.stream().collect(Collectors.toList())).runAndWait(JarEntry->{
									String name = JarEntry.getName();
									if(name.contains(".class")) {
										classNames.add(name.replace("/", ".").replace("\\", "."));
									}else {
										try {
											if(name.contains("spring.handlers")) {
												xmlResourceLoader.loadResource(jFile.getInputStream(JarEntry));
											}else if(name.contains("spring.factories")) {
												annoResourceLoader.loadResource(jFile.getInputStream(JarEntry));
											}
										} catch (IOException e) {
											loadFaile.set(true);
											log.error("jFile.getInputStream(JarEntry)",e);
										}
									}
								});
							}
						} catch (Exception e) {
							log.error("不能加载class文件=>{}",url.getPath());
						}
					}
				} catch (IOException e1) {
					log.error("getURL",e1);
					loadFaile.set(true);
				}
			});
		} catch (IOException e1) {
			log.error("读取文件异常",e1);
			loadFaile.set(true);
		}
		if(loadFaile.get()) {
			throw new JunitException();
		}
		log.debug("=============加载class结束=============");
		List<Class<?>> springBoot = ScanUtil.findClassWithAnnotation(SpringBootApplication.class,applicationAllClassMap);
		springBoot.forEach(startClass ->{
			TestUtil.getInstance().loadScanPath(startClass.getPackage().getName());
			/**
			 * 查看导入资源
			 */
			ImportResource resource = startClass.getAnnotation(ImportResource.class);
			if(resource != null) {
				XMLResourceLoader.getInstance().loadXmlPath(resource.value());
			}
		});
		loadContextPathClass();
		log.debug("=============加载class结束=============");
	}
	@Getter
	static Map<String,Class<?>> applicationAllClassMap = Maps.newConcurrentMap();
	public void loadClass(File file,String rootPath){
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				loadClass(f,rootPath);
			} else if (f.getName().endsWith(CLASS_SUFFIX)) {
				String p = f.getPath();
				File tmp = new File(rootPath);
				p = p.replace(tmp.getPath()+"\\", "").replace(tmp.getPath()+"/", "").replace("/", ".").replace("\\", ".").replace(CLASS_SUFFIX, "");
				// 查看是否class
				try {
					Class<?> c = TestUtil.class.getClassLoader().loadClass(p);
					classNames.add(p+CLASS_SUFFIX);
					applicationAllClassMap.put(p,c);
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					log.error("未找到类=>{}",p);
				}catch(Exception e) {
					log.error("加载类异常",e);
				}catch (VerifyError e) {
					log.error("加载类校验异常>{}=>{}",p,e.getMessage());
				}
			}else {
				try {
					if(f.getName().contains("spring.handlers")) {
						xmlResourceLoader.loadResource(new FileInputStream(f));
					}else if(f.getName().contains("spring.factories")) {
						annoResourceLoader.loadResource(new FileInputStream(f));
					}
				} catch (IOException e) {
					log.error("jFile.getInputStream(JarEntry)",e);
				}
//				log.debug("=============其他文件=={}===========",file);
			}
		}
	}
	public void loadContextPathClass() {
//		Set<String> classPaths =TestUtil.getInstance().getScanClassPath();
		JunitCountDownLatchUtils.buildCountDownLatch(classNames.stream().filter(cn->TestUtil.getInstance().isScanClassPath(cn)).collect(Collectors.toList()))
		.runAndWait(name->{
//			if(name.contains("SpringContextUtil")) {
//				log.info("短点");
//			}
			if(name.endsWith(CLASS_SUFFIX) && !componentClassPathMap.containsKey(name)) {
				name = name.replace("/", ".").replace("\\", ".").replace(CLASS_SUFFIX, "");
				// 查看是否class
				try {
					Class<?> c = ScanUtil.class.getClassLoader().loadClass(name);
					try {
						ConfigurationScan.getInstance().scanConfigClass(c);
					} catch (IOException e) {
						log.error("scanConfigClass for loadContextPathClass");
					}
					
					if(CheckUtil.checkClassExists(c)
							&& CheckUtil.checkProp(c)) {
						componentClassPathMap.putIfAbsent(name,c);
					}
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					log.error("加载{}=>未找到类{}",name,e.getMessage());
				}catch(Error e) {
					log.error("未找到类{}=>{}",name,e.getMessage());
				}
			}
		});
	}
	private static ClassScan scaner = new ClassScan();
	private ClassScan() {}
	public static ClassScan getInstance() {
		return scaner;
	}
	/**
	 * 个别自定义类
	 * @param configClass
	 */
	public boolean hasStaticMethod(Class<?> configClass) {
		try {
			Method[] methods = configClass.getDeclaredMethods();
			return Lists.newArrayList(methods).stream().anyMatch(m->{
				if(Modifier.isStatic(m.getModifiers())
						&& !m.getName().contains("lambda$")//非匿名方法
						&& !m.getName().contains("access$")) {//非匿名方法
					Class<?> returnType = m.getReturnType();
					if(!returnType.getName().contains("void")) {
						log.debug("method=>{}",m);
						return true;
					}
				}
				return false;
			});
		}catch(Exception e) {
			log.error("hasStaticMethod",e);
		}
		return false;
	}
	public List<Class<?>> findStaticMethodClass() {
		Set<Class<?>> list = Sets.newHashSet();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(componentClassPathMap.keySet()))
		.setException((name,e)->{
		    log.error("遗漏#findStaticMethodClass#=>{}",name);
		})
		.runAndWait(name ->{
			Class<?> c = componentClassPathMap.get(name);
			if(c.getAnnotations().length>0) {
				if(c.isAnnotationPresent(Configuration.class)
						|| c.isAnnotationPresent(Service.class)
						|| c.isAnnotationPresent(Component.class)
						|| c.isAnnotationPresent(Repository.class)) {
					if(hasStaticMethod(c)) {
						list.add(c);
					}
				}
			}
		});
		return Lists.newArrayList(list);
	}
	
	volatile Map<String,Class<?>> cacheBeanNameClass = Maps.newConcurrentMap();
	public Class<?> findClassByName(String beanName) {
		if(!cacheBeanNameClass.isEmpty()) {
			return cacheBeanNameClass.get(beanName);
		}
		synchronized (cacheBeanNameClass) {
			if(!cacheBeanNameClass.isEmpty()) {
				return cacheBeanNameClass.get(beanName);
			}	
			AtomicReference<Class<?>> findClass = new AtomicReference<Class<?>>();
			JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(componentClassPathMap.keySet()))
			.runAndWait(name ->{
				Class<?> tagClass = componentClassPathMap.get(name);
				try {
					String annValue = BeanNameUtil.getBeanName(tagClass);
					log.info("put beanClass=>{}",annValue);
					cacheBeanNameClass.put(annValue, tagClass);
					if (Objects.equals(annValue, beanName)) {
						findClass.set(tagClass);
					}
				} catch (Exception e) {
				}
			});
			return findClass.get();
		}
	}
	public List<Class<?>> findClassImplInterface(Class<?> interfaceClass) {
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(componentClassPathMap.keySet()))
		.runAndWait(name ->{
			Class<?> tmpClass = componentClassPathMap.get(name);
			if(ScanUtil.isImple(tmpClass,interfaceClass)) {
				if((tmpClass.getAnnotation(Component.class)!=null || tmpClass.getAnnotation(Service.class)!=null)
						&& !Modifier.isAbstract(tmpClass.getModifiers())) {
					list.add(tmpClass);
				}
			}
		});
		return list;
	}
	public static Map<String,Map<String,Class<?>>> pathForClass = Maps.newConcurrentMap();
	public Map<String, Class<?>> findClassMap(String scanPath) {
		if(pathForClass.containsKey(scanPath)) {
			return pathForClass.get(scanPath);
		}
		Map<String,Class<?>> nameMapTmp = Maps.newHashMap();
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
	public Boolean isInScanPath(Class<?> requiredType) {
		return componentClassPathMap.containsKey(requiredType.getName());
	}
	public List<Class<?>> findClassExtendAbstract(Class<?> abstractClass) {
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(componentClassPathMap.keySet()))
		.runAndWait(name ->{
			Class<?> tmpClass = componentClassPathMap.get(name);
			if(ScanUtil.isExtends(tmpClass,abstractClass)) {
				if((tmpClass.getAnnotation(Component.class)!=null || tmpClass.getAnnotation(Service.class)!=null)
						&& !Modifier.isAbstract(tmpClass.getModifiers())) {
					list.add(tmpClass);
				}
			}
		});
		return list;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

}
