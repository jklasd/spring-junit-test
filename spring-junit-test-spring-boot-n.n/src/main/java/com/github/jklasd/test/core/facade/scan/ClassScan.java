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
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.register.Scan;
import com.github.jklasd.test.common.util.CheckUtil;
import com.github.jklasd.test.common.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.ResourceLoader;
import com.github.jklasd.test.core.facade.loader.XMLResourceLoader;
import com.github.jklasd.test.exception.JunitException;
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
	private ResourceLoader resourceLoader = ResourceLoader.getInstance();
	
	public void loadComponentClass(Class<?> c) {
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
									if(name.contains(".class")) {
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
				} catch(JunitException e) {
					log.error("getURL",e);
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
		springBoot.forEach(startClass ->{
			TestUtil.getInstance().loadScanPath(startClass.getPackage().getName());
			/**
			 * 查看导入资源
			 */
			ImportResource resource = startClass.getAnnotation(ImportResource.class);
			if(resource != null) {
				XMLResourceLoader.getInstance().loadResource(resource.value());
			}
		});
		loadContextPathClass();
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
					Class<?> c = classLoader.junitloadClass(p);
					if(c!=null) {
						classNames.add(p+CLASS_SUFFIX);
						applicationAllClassMap.put(p,c);
					}
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					log.error("未找到类=>{}",p);
					if(p.startsWith("com.github.jdkasd.test")) {
						log.warn(tmp.getPath());
						throw new JunitException(e);
					}
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
					Class<?> c = classLoader.junitloadClass(name);
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
	public static ClassScan getInstance() {
		if(scaner==null) {
			scaner = new ClassScan();
		}
		return scaner;
	}
	
//	public List<Class<?>> findStaticMethodClass() {
//		Set<Class<?>> list = Sets.newHashSet();
//		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(componentClassPathMap.keySet()))
//		.setException((name,e)->{
//		    log.error("遗漏#findStaticMethodClass#=>{}",name);
//		})
//		.runAndWait(name ->{
//			Class<?> c = componentClassPathMap.get(name);
//			if(c.getAnnotations().length>0) {
//				if(c.isAnnotationPresent(Configuration.class)
//						|| c.isAnnotationPresent(Service.class)
//						|| c.isAnnotationPresent(Component.class)
//						|| c.isAnnotationPresent(Repository.class)) {
//					if(hasStaticMethod(c)) {
//						list.add(c);
//					}
//				}
//			}
//		});
//		return Lists.newArrayList(list);
//	}
	
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
					Class<?> c = classLoader.junitloadClass(name);
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

	@Override
	public void register() {
		ContainerManager.registComponent( this);
	}
	@Override
	public String getBeanKey() {
		return Scan.class.getSimpleName();
	}
}
