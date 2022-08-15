package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.Field;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.component.MockAnnHandlerComponent;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.interf.DatabaseInitialization;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutingDataSourceExt extends AbstractRoutingDataSource implements ContainerRegister,DatabaseInitialization{
	
	String junitH2Key = "junitH2";
	String defaultKey = "default";
	
	@Override
	protected Object determineCurrentLookupKey() {
		if(MockAnnHandlerComponent.isMock(H2Select.class.getName())) {
			return junitH2Key;
		}
		return defaultKey;
	}
	private RoutingDataSourceExt(){}
	private static RoutingDataSourceExt routingDataSourceExt;
	
	public static RoutingDataSourceExt getInstance() {
		if(routingDataSourceExt == null) {
			routingDataSourceExt = new RoutingDataSourceExt();
			routingDataSourceExt.register();//注册到全局
		}
		return routingDataSourceExt;
	}
	
	Map<Object,DataSource> superSource;
	private DataSource h2Source;
	void setDataSource(DataSource h2Source) {
		this.h2Source = h2Source;
	}
	
	
	public DataSource build(DataSource dataSource) {
		
		if(superSource != null) {
			return routingDataSourceExt;
		}
		
		Map<Object,Object> cache = Maps.newHashMap();
		cache.put(defaultKey, dataSource);
		try {
			Field targetDataSources = AbstractRoutingDataSource.class.getDeclaredField("resolvedDataSources");
			if(!targetDataSources.isAccessible()) {
				targetDataSources.setAccessible(true);
			}
			superSource = (Map<Object, DataSource>) targetDataSources.get(this);
			if(superSource == null) {
				cache.put(junitH2Key, h2Source);
				setTargetDataSources(cache);
			}else {
				superSource.put(junitH2Key, h2Source);
				superSource.put(defaultKey, dataSource);
			}
		} catch (Exception e) {
			log.error("putDataSource",e);
		}
		setDefaultTargetDataSource(dataSource);
		this.afterPropertiesSet();
		return this;
	}
	@Override
	public void register() {
		ContainerManager.registComponent(this);
	}
	@Override
	public String getBeanKey() {
		return DatabaseInitialization.class.getName();
	}

}
