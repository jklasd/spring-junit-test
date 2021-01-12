package com.junit.test.mapper;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.Resource;

import com.github.pagehelper.PageHelper;
import com.google.common.collect.Maps;
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
	
	public static Map<Class,Object> mapper = Maps.newHashMap();
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
	
	private static SqlSession session;
	@SuppressWarnings("unchecked")
	private static Object getMapper(Class classBean) throws Exception {
		if(mapper.containsKey(classBean)) {
			return mapper.get(classBean);
		}
//		if(((PooledDataSource)dataSource).getPoolState().getActiveConnectionCount()>5) {
//			log.info("连接数=>{}",((PooledDataSource)dataSource).getPoolState().getActiveConnectionCount());
//		}
		if(session == null) {
			session = factory.getObject().openSession();
		}
		Object tag = session.getMapper(classBean);
		mapper.put(classBean, tag);
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
			dataSourceTmp.setUrl(TestUtil.getPropertiesValue("jdbc.url"));
			dataSourceTmp.setUsername(TestUtil.getPropertiesValue("jdbc.username"));
			dataSourceTmp.setPassword(TestUtil.getPropertiesValue("jdbc.password"));
			dataSourceTmp.setDriver(TestUtil.getPropertiesValue("jdbc.driver"));
			dataSourceTmp.setPoolMaximumIdleConnections(1);
			dataSource = dataSourceTmp;
		}else {
			log.info("dataSource已存在");
		}
	}

}