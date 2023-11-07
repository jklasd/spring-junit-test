package com.github.jklasd.test.common.component;

import java.util.List;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;
import com.google.common.collect.Lists;

public class ScannerRegistrarComponent extends AbstractComponent{
	
	private static List<ScannerRegistrarI> scannerList = Lists.newArrayList();
	
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

	@Override
	<T> void add(T component) {
		scannerList.add((ScannerRegistrarI) component);
	}
}
