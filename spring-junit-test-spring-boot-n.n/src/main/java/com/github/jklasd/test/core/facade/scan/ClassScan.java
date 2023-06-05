package com.github.jklasd.test.core.facade.scan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.register.Scan;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.common.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.JunitResourceLoaderManager;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassScan implements Scan{
	private ClassScan() {}
	private static ClassScan scaner;
	private boolean init;
	private static Set<String> classNames = Sets.newConcurrentHashSet();
	Map<String,Class<?>> componentClassPathMap = Maps.newConcurrentMap();
	private static String CLASS_SUFFIX = ".class";
	
	private JunitClassLoader classLoader = JunitClassLoader.getInstance();
	private JunitResourceLoaderManager resourceLoader = JunitResourceLoaderManager.getInstance();
	
	public void loadComponentClass(Class<?> c) {
		componentClassPathMap.put(c.getName(), c);
	}
	public void loadComponentClassForAnnotation(Class<?> c) {
		if(!AnnHandlerUtil.isAnnotationPresent(c, Component.class)) {
			return;
		}
		componentClassPathMap.put(c.getName(), c);
	}
	
	
	@Override
	public void scan() {
		if(init) {
			return;
		}
		init = true;
		log.info("=========加载class========");
		AtomicBoolean loadFaile = new AtomicBoolean();
		try {
			Resource[] resources = ScanUtil.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" );
			JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(resources)).setExecutorService(1).forEach(r->{
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
								JunitCountDownLatchUtils.buildCountDownLatch(jFile.stream().collect(Collectors.toList())).setExecutorService(2).runAndWait(JarEntry->{
									String name = JarEntry.getName();
									if(name.endsWith(CLASS_SUFFIX)) {
										classNames.add(name.replace("/", ".").replace("\\", "."));
									}else {
										try {
											resourceLoader.loadResource(name,jFile.getInputStream(JarEntry));
										} catch (IOException e) {
											log.error("加载资源{}异常",name,e);
										}
									}
								});
							}
						} catch (Exception e) {
							log.error("不能加载jar文件=>{}",url.getPath());
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
		log.info("=============加载class结束=============");
		List<Class<?>> springBoot = ScanUtil.findClassWithAnnotation(SpringBootApplication.class,applicationAllClassMap);
		if(springBoot.isEmpty()) {
			throw new JunitException("未找到 SpringBootApplication 注解相关类", true);
		}
		for(Class<?> bootClass : springBoot) {
			try {
				ConfigurationScan.getInstance().scanConfigClass(bootClass);
			} catch (IOException e) {
				throw new JunitException("scanConfigClass处理 SpringBootApplication类", true);
			}
		}
//		loadContextPathClass();
		log.debug("=============加载class结束=============");
	}
	@Getter
	static Map<String,Class<?>> applicationAllClassMap = Maps.newConcurrentMap();
	public void loadClass(File file,String rootPath) throws UnsupportedEncodingException{
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				loadClass(f,rootPath);
			} else if (f.getName().endsWith(CLASS_SUFFIX)) {
				String p = f.getPath();
				File tmp = new File(rootPath);
				p = p.replace(URLDecoder.decode(tmp.getPath(),"UTF-8")+"\\", "").replace(tmp.getPath()+"/", "").replace("/", ".").replace("\\", ".").replace(CLASS_SUFFIX, "");
				// 查看是否class
				try {
					Class<?> c = classLoader.loadClass(p);
					if(c!=null) {
						classNames.add(p+CLASS_SUFFIX);
						applicationAllClassMap.put(p,c);
					}
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					log.error("未找到类=>{}",p);
				}catch(Exception e) {
					log.error("加载类异常",e);
				}catch (VerifyError e) {
					log.error("加载类校验异常>{}=>{}",p,e.getMessage());
				}catch (Error e) {
					log.error("加载类异常>{}",p,e);
				}
			}else {
				try {
					resourceLoader.loadResource(f.getName(),new FileInputStream(f));
				} catch (IOException e) {
					log.error("加载资源{}异常",f.getName(),e);
				}
//				log.debug("=============其他文件=={}===========",file);
			}
		}
	}

//	public void loadContextPathClass() {
////		Set<String> classPaths =TestUtil.getInstance().getScanClassPath();
//		JunitCountDownLatchUtils.buildCountDownLatch(classNames.stream().filter(cn->TestUtil.getInstance().isScanClassPath(cn)).collect(Collectors.toList()))
//		.runAndWait(name->{
//			if(name.endsWith(CLASS_SUFFIX) && !componentClassPathMap.containsKey(name)) {
//				name = name.replace("/", ".").replace("\\", ".").replace(CLASS_SUFFIX, "");
//				// 查看是否class
//				try {
//					Class<?> c = classLoader.junitloadClass(name);
//					try {
//						ConfigurationScan.getInstance().scanConfigClass(c);
//					} catch (IOException e) {
//						log.error("scanConfigClass for loadContextPathClass");
//					}
//					
//					if(CheckUtil.checkClassExists(c)
//							&& CheckUtil.checkProp(c)) {
//						componentClassPathMap.putIfAbsent(name,c);
//					}
//				} catch (ClassNotFoundException | NoClassDefFoundError e) {
//					log.error("加载{}=>未找到类{}",name,e.getMessage());
//				}catch(Error e) {
//					log.error("未找到类{}=>{}",name,e.getMessage());
//				}
//			}
//		});
//	}
	
	public static ClassScan getInstance() {
		if(scaner==null) {
			scaner = new ClassScan();
		}
		return scaner;
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
					if(annValue == null) {
//						log.warn("{} beanName=>{}",tagClass,annValue);
						return;
					}
					log.debug("put beanClass=>{}",annValue);
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
	Map<Class<?>,List<Class<?>>> cacheInterfaceImpls = Maps.newConcurrentMap();
	public List<Class<?>> findClassImplInterface(Class<?> interfaceClass) {
		
		if(cacheInterfaceImpls.containsKey(interfaceClass)) {
			return cacheInterfaceImpls.get(interfaceClass);
		}
		
		List<Class<?>> list = Lists.newCopyOnWriteArrayList();
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
		if(!list.isEmpty()) {
			cacheInterfaceImpls.put(interfaceClass, list);
		}
		return list;
	}
	
	public List<Class<?>> findClassForList(String scanPath) {
		return classNames.stream().filter(cn->cn.contains(scanPath)).map(className ->{
			className = className.replace("/", ".").replace("\\", ".").replace(".class", "");
			// 查看是否class
			try {
				Class<?> c = classLoader.loadClass(className);
				return c;
			} catch (ClassNotFoundException | NoClassDefFoundError e) {
				if(TestUtil.getInstance().isScanClassPath(className)) {
					log.error("加载{}=>未找到类{}",className,e.getMessage());
				}
			}catch(Error e) {
				log.error("未找到类{}=>{}",className,e.getMessage());
			}
			return null;
		}).collect(Collectors.toList());
	}
	
	private static Map<String,Map<String,Class<?>>> pathForClass = Maps.newConcurrentMap();
	@Deprecated
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
					Class<?> c = classLoader.loadClass(name);
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
		if(cacheInterfaceImpls.containsKey(abstractClass)) {
			return cacheInterfaceImpls.get(abstractClass);
		}
		List<Class<?>> list = Lists.newCopyOnWriteArrayList();
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
		if(!list.isEmpty()) {
			cacheInterfaceImpls.put(abstractClass, list);
		}
		return list;
	}

	@Override
	public String getBeanKey() {
		return Scan.class.getSimpleName();
	}
}
