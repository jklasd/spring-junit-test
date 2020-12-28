package com.junit.test.mapper;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.Resource;

import com.github.pagehelper.PageHelper;
import com.google.common.collect.Maps;
import com.junit.test.ScanUtil;
import com.junit.test.TestUtil;

import lombok.extern.slf4j.Slf4j;

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
	
	
	@SuppressWarnings("unchecked")
	private static Object getMapper(Class classBean) throws Exception {
		if(mapper.containsKey(classBean)) {
			return mapper.get(classBean);
		}
		Object tag = factory.getObject().openSession().getMapper(classBean);
		mapper.put(classBean, tag);
		return tag;
	}


	private static void buildFactory() throws Exception {
		factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);
		Resource[] resources = ScanUtil.getResources(TestUtil.getPropertiesValue("mybatis.mapper.path",TestUtil.mapperPath));
		factory.setMapperLocations(resources);
//		factory.setTypeAliasesPackage("");
		factory.setPlugins(new Interceptor[]{new PageHelper()});
		factory.afterPropertiesSet();
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
		PooledDataSource dataSourceTmp = new PooledDataSource();
		dataSourceTmp.setUrl(TestUtil.getPropertiesValue("jdbc.url"));
		dataSourceTmp.setUsername(TestUtil.getPropertiesValue("jdbc.username"));
		dataSourceTmp.setPassword(TestUtil.getPropertiesValue("jdbc.password"));
		dataSourceTmp.setDriver(TestUtil.getPropertiesValue("jdbc.driver"));
		dataSource = dataSourceTmp;
	}

}