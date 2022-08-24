package com.github.jklasd.test.common.component;

import java.util.List;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;
import com.google.common.collect.Lists;

public class ScannerRegistrarComponent {
	
	private static List<ScannerRegistrarI> scannerList = Lists.newArrayList();
	
	public static void load(String[] className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		for(String hclass :className) {
			Class<?> handlerClass = JunitClassLoader.getInstance().loadClass(hclass);
			ScannerRegistrarI scanner = (ScannerRegistrarI) handlerClass.newInstance();
			scannerList.add(scanner);
		}
	}
	
	/**
	 * 扫描java代码相关配置
	 * 
	 * 待支持 spring.factories
	 */
	public static void process() {
		
		scannerList.stream().filter(item->item.using()).forEach(item->{
			item.scannerAndRegister();
		});
		
	}
}
