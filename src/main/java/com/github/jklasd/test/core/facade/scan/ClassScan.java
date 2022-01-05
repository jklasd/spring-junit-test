package com.github.jklasd.test.core.facade.scan;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.Facade;
import com.github.jklasd.test.core.facade.Scan;
import com.github.jklasd.test.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Facade("Scan")
public class ClassScan implements Scan{

	private boolean init;
	private static Set<String> classNames = Sets.newConcurrentHashSet();
	Map<String,Class<?>> classPathMap = Maps.newConcurrentMap();
	private static String CLASS_SUFFIX = ".class";
	
	@Override
	public void scan() {
		try {
			if(init) {
				return;
			}
			init = true;
//			log.debug("=============开始加载class=============");
			Resource[] resources = ScanUtil.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" );
//			log.debug("=============加载class={}============",resources.length);
			for (Resource r : resources) {
				URL url = r.getURL();
				if("file".equals(url.getProtocol())) {
					File f = r.getFile();
//					log.debug("=======加载{}内的====class=========",f);
					ScanUtil.loadClass(f,url.getFile());
				}
			}
			loadContextPathClass();
			log.info("=============加载class结束=============");
		} catch (IOException e1) {
			log.error("读取文件异常",e1);
		}
	}
	static Map<String,Class> nameMap = Maps.newConcurrentMap();
	public static void loadClass(File file,String rootPath){
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

}
