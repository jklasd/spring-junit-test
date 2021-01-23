package com.junit.test.mapper;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.Resource;

import com.github.pagehelper.PageHelper;
import com.junit.test.ScanUtil;
import com.junit.test.TestUtil;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyMybatisMapperBean{
	private static DataSource dataSource;
	private static SqlSessionFactoryBean factory;
	
	
	@SuppressWarnings("unchecked")
	public static Object buildBean(Class classBean) {
		try {
			if(dataSource == null) {
				buildDataSource();
			}
			if(factory == null) {
				buildFactory();
			}
			return getMapper(classBean);
		} catch (Exception e) {
			log.error("获取Mapper",e);
		}
		return null;
	}
	
	private static ThreadLocal<SqlSession> sessionList = new ThreadLocal<>();
	
	@SuppressWarnings("unchecked")
	private static Object getMapper(Class classBean) throws Exception {
//		if(((PooledDataSource)dataSource).getPoolState().getActiveConnectionCount()>5) {
//			log.info("连接数=>{}",((PooledDataSource)dataSource).getPoolState().getActiveConnectionCount());
//		}
		if(sessionList.get() != null){
			return sessionList.get().getMapper(classBean);
		}else {
			sessionList.set(factory.getObject().openSession());
		}
		Object tag = sessionList.get().getMapper(classBean);
		return tag;
	}


	private static void buildFactory() throws Exception {
		if(factory == null) {
			factory = new SqlSessionFactoryBean();
			factory.setDataSource(dataSource);
			Resource[] resources = ScanUtil.getResources(TestUtil.getPropertiesValue("mybatis.mapper.path",TestUtil.mapperPath));
			factory.setMapperLocations(resources);
//		factory.setTypeAliasesPackage("");
			factory.setPlugins(new Interceptor[]{new PageHelper()});
			factory.afterPropertiesSet();
		}else {
			log.info("factory已存在");
		}
	}


	/**
	 * <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource" init-method="init" destroy-method="close">
		<property name="url" value="${jdbc.url}" />
		<property name="username" value="${jdbc.username}" />
		<property name="password" value="${jdbc.password}" />
		<property name="maxWait" value="30000" />
		<property name="validationQuery" value="SELECT 'x'" />
		</bean>
	 * @return
	 */
	public static void buildDataSource() {
		if(dataSource == null) {
			PooledDataSource dataSourceTmp = new PooledDataSource();
			if(StringUtils.isBlank(TestUtil.mapperJdbcPrefix)) {
				TestUtil.mapperJdbcPrefix = "jdbc";
			}
			if(StringUtils.isNotBlank(TestUtil.getPropertiesValue(TestUtil.mapperJdbcPrefix+".url"))) {
				dataSourceTmp.setUrl(TestUtil.getPropertiesValue(TestUtil.mapperJdbcPrefix+".url"));
				dataSourceTmp.setUsername(TestUtil.getPropertiesValue(TestUtil.mapperJdbcPrefix+".username"));
				dataSourceTmp.setPassword(TestUtil.getPropertiesValue(TestUtil.mapperJdbcPrefix+".password"));
				if(StringUtils.isNotBlank(TestUtil.getPropertiesValue(TestUtil.mapperJdbcPrefix+".driverClassName"))) {
					dataSourceTmp.setDriver(TestUtil.getPropertiesValue(TestUtil.mapperJdbcPrefix+".driverClassName"));
				}else {
					dataSourceTmp.setDriver(TestUtil.getPropertiesValue(TestUtil.mapperJdbcPrefix+".driver"));
				}
				dataSourceTmp.setPoolMaximumActiveConnections(10);
				dataSourceTmp.setPoolMaximumIdleConnections(1);
				dataSource = dataSourceTmp;
			}else {
				dataSource = TestUtil.dataSource;
			}
		}else {
			log.info("dataSource已存在");
		}
	}


	public static void over() {
		if(sessionList.get()!=null) {
			sessionList.get().commit();
			sessionList.get().close();
			sessionList.remove();
		}
	}

}