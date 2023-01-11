package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.component.MockAnnHandlerComponent;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.interf.DatabaseInitialization;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutingDataSourceExt extends AbstractRoutingDataSource implements ContainerRegister,DatabaseInitialization{
	
	String junitDbKey = "mysqlContainers";
	String defaultKey = "default";
	
	@Override
	protected Object determineCurrentLookupKey() {
		if(MockAnnHandlerComponent.isMock(JunitMysqlContainerSelected.class.getName())) {
			return junitDbKey;
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
	private DataSource msyqlSource;
	private Map<String,DataSource> mysqlSourceMap = Maps.newHashMap();
	void setDataSource(DataSource msyqlSource) {
		this.msyqlSource = msyqlSource;
	}
	boolean inited;
	void initFunction(DataSource h2Source) {
		if(inited) {
			return;
		}
//		try {
			inited = true;
//			Statement statement = h2Source.getConnection().createStatement();
//			statement.execute("CREATE ALIAS IF NOT EXISTS \"ISNULL_\" FOR \"com.github.jklasd.test.core.common.methodann.mock.h2.MysqlToH2Functions.ISNULL_\";");
//			statement.execute("CREATE ALIAS IF NOT EXISTS \"IF_\" FOR \"com.github.jklasd.test.core.common.methodann.mock.h2.MysqlToH2Functions.IF_\";");
//		} catch (SQLException e) {
//			log.error("initFunction",e);
//		}
	}
	
	/**
	 * 封装多数据源
	 */
	public DataSource build(DataSource dataSource) {
		
		if(superSource != null) {
			return routingDataSourceExt;
		}
		if(msyqlSource == null) {
			return dataSource;
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
				cache.put(junitDbKey, msyqlSource);
				setTargetDataSources(cache);
			}else {
				superSource.put(junitDbKey, msyqlSource);
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
	public void putDataSource(String dbName, EmbeddedDatabase database) {
		mysqlSourceMap.put(dbName, database);
	}
	@Override
	public boolean isInitDataSource() {
		return msyqlSource!=null;
	}
	
	public void handInsertResource(String[] insertResource) {
		
	}
	
}
