package com.github.jklasd.test.spring.suppert;

import java.util.Map;

import javax.sql.DataSource;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.DatabaseInitialization;
import com.github.jklasd.test.common.util.LazyDataSourceUtil;
import com.google.common.collect.Maps;

public final class JunitDataSourceBuild {
	private static Map<Integer,DataSource> rebuildedDataSource = Maps.newHashMap();
	public static DataSource rebuildDataSource(DataSource dataSource) {
		if(rebuildedDataSource.containsKey(dataSource.hashCode())) {
			return rebuildedDataSource.get(dataSource.hashCode());
		}
		
		
		LazyDataSourceUtil.defaultDataSouce(dataSource);
		DatabaseInitialization dataInitia = ContainerManager.getComponent(DatabaseInitialization.class.getName());
		if(dataInitia!=null) {
			rebuildedDataSource.put(dataSource.hashCode(), dataInitia.build(dataSource));
			return (DataSource) dataInitia;
		}else {
			rebuildedDataSource.put(dataSource.hashCode(), dataSource);
			return dataSource;
		}
	}
	
}
