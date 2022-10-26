package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutingDataSourceExt extends AbstractRoutingDataSource implements ContainerRegister,DatabaseInitialization{
	
	String junitH2Key = "junitH2";
	String defaultKey = "default";
	
	@Override
	protected Object determineCurrentLookupKey() {
		if(MockAnnHandlerComponent.isMock(JunitH2Selected.class.getName())) {
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
	private Map<String,DataSource> h2SourceMap = Maps.newHashMap();
	void setDataSource(DataSource h2Source) {
		this.h2Source = h2Source;
	}
	boolean inited;
	void initFunction(DataSource h2Source) {
		if(inited) {
			return;
		}
		try {
			inited = true;
			Statement statement = h2Source.getConnection().createStatement();
			statement.execute("CREATE ALIAS IF NOT EXISTS \"ISNULL_\" FOR \"com.github.jklasd.test.core.common.methodann.mock.h2.MysqlToH2Functions.ISNULL_\";");
			statement.execute("CREATE ALIAS IF NOT EXISTS \"IF_\" FOR \"com.github.jklasd.test.core.common.methodann.mock.h2.MysqlToH2Functions.IF_\";");
		} catch (SQLException e) {
			log.error("initFunction",e);
		}
	}
	
	/**
	 * 封装多数据源
	 */
	public DataSource build(DataSource dataSource) {
		
		if(superSource != null) {
			return routingDataSourceExt;
		}
		if(h2Source == null) {
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
	public void putDataSource(String dbName, EmbeddedDatabase database) {
		h2SourceMap.put(dbName, database);
	}
	@Override
	public boolean isInitDataSource() {
		return h2Source!=null;
	}
	
	/**
	 * 开放用户自定义插入脚本配置
	 * @param insertResource
	 */
	public void handInsertResource(String... insertResource) {
		try {
			for(String path : insertResource) {
				if(StringUtils.isBlank(path)) {
					continue;
				}
				Resource resouce = LazyApplicationContext.getInstance().getResource(path);
				EncodedResource encodedScript = new EncodedResource(resouce);
				ScriptUtilsExt.executeSqlScript(h2Source.getConnection(), encodedScript);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
