package com.github.jklasd.test.common.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.sql.DataSource;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.util.LazyDataSourceUtil.DataSourceBuilder;

public class LazyDataSourceUtil {
	
	public static interface DataSourceBuilder{
		DataSource build();
	}
	
	public final static DataSource buildLazyDataSource(DataSourceBuilder builder) {
		DataSourceImpl handler = new DataSourceImpl(builder);
        return (DataSource) Proxy.newProxyInstance(JunitClassLoader.getInstance(), new Class[] { DataSource.class }, handler);
	}
	private static DataSource defaultDataSouce;
	public static void defaultDataSouce(DataSource dataSource) {
		defaultDataSouce = dataSource;
	}
	public static DataSource getDefaultDataSouce() {
		return defaultDataSouce;
	}
}

class DataSourceImpl implements InvocationHandler{
	
	DataSource source;
	DataSourceBuilder builder;
	
	public DataSourceImpl(DataSourceBuilder builder) {
		this.builder = builder;
	}

	private boolean builded;
	
	@Override
	public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
		try {
			if(source == null && !builded) {
				source = builder.build();
				builded = true;
			}
			if(source != null) {
				return arg1.invoke(source, arg2);
			}else {
				return arg1.invoke(LazyDataSourceUtil.getDefaultDataSouce(), arg2);
			}
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
	
}
