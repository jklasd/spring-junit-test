package com.github.jklasd.test.core.facade.scan;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.FacadeLoader;
import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.github.jklasd.test.core.facade.Scan;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassScan implements Scan{

	private boolean init;
	private static Set<String> classNames = Sets.newConcurrentHashSet();
	Map<String,Class<?>> classPathAutoConfigMap = Maps.newConcurrentMap();
	private static String CLASS_SUFFIX = ".class";
	
	private JunitResourceLoader xmlResourceLoader = FacadeLoader.getFacadeUtil("com.github.jklasd.test.core.facade.loader.XMLResourceLoader");
	private JunitResourceLoader annoResourceLoader = FacadeLoader.getFacadeUtil("com.github.jklasd.test.core.facade.loader.AnnotationResourceLoader");
	
	@Override
	public void scan() {
		try {
			if(init) {
				return;
			}
			init = true;
			Resource[] resources = ScanUtil.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" );
			for (Resource r : resources) {
				URL url = r.getURL();
				if("file".equals(url.getProtocol())) {
					File f = r.getFile();
					loadClass(f,url.getFile());
				}else if("jar".equals(url.getProtocol())){
					if(url.getPath().contains("jre/lib")) {
						continue;
					}
					try {
						URLConnection connection = url.openConnection();
						if (connection instanceof JarURLConnection) {
							JarFile jFile = ((JarURLConnection) connection).getJarFile();
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
                            		  log.error("jFile.getInputStream(JarEntry)",e);
                            	  }
                              }
							});
						}
					} catch (Exception e) {
						log.error("不能加载class文件=>{}",url.getPath());
					}
				}
			}
			loadContextPathClass();
			log.info("=============加载class结束=============");
		} catch (IOException e1) {
			log.error("读取文件异常",e1);
		}
	}
	static Map<String,Class<?>> nameMap = Maps.newConcurrentMap();
	public void loadClass(File file,String rootPath){
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
				}catch (VerifyError e) {
					log.error("加载类校验异常>{}=>{}",p,e.getMessage());
				}
			}else {
//				log.debug("=============其他文件=={}===========",file);
			}
		}
	}
	public void loadContextPathClass() {
		JunitCountDownLatchUtils.buildCountDownLatch(classNames.stream().filter(cn->TestUtil.getInstance().isScanClassPath(cn)).collect(Collectors.toList()))
		.runAndWait(name->{
			if(name.endsWith(CLASS_SUFFIX) && !nameMap.containsKey(name)) {
				name = name.replace("/", ".").replace("\\", ".").replace(".class", "");
				// 查看是否class
				try {
					Class<?> c = ScanUtil.class.getClassLoader().loadClass(name);
					classPathAutoConfigMap.put(name,c);
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					log.error("加载{}=>未找到类{}",name,e.getMessage());
				}catch(Error e) {
					log.error("未找到类{}=>{}",name,e.getMessage());
				}
			}
		});
	}
	@Override
	public Object[] findCreateBeanFactoryClass(AssemblyDTO assemblyData) {
		// TODO Auto-generated method stub
		return null;
	}

}
