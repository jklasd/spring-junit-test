package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.sql.Driver;

import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.MySQLContainer;

import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseTestContainers{
	static class MySQLContainerExt extends MySQLContainer<MySQLContainerExt>{

		public MySQLContainerExt(String image) {
			super(image);
		}
		
		public void addFixedExposedPort(int hostPort, int containerPort) {
	        addFixedExposedPort(hostPort, containerPort, InternetProtocol.TCP);
	    }
		
	}
	
	private MySQLContainerExt mySQLContainer;
	@Getter
	private String url;
	@Getter
	private String userName="root";
	@Getter
	private String password="root";
	@Getter
	private Class<? extends Driver> driverClass;

	@SuppressWarnings("resource")
	public BaseTestContainers(String databaseName) {
		mySQLContainer = new MySQLContainerExt("mysql:5.7")
		            .withUsername(userName)
		            .withPassword(password)
		            .withDatabaseName(databaseName);
		
		Integer fixedPort = LazyApplicationContext.getInstance().getEnvironment().getProperty("MYSQL_CONTAINER_PORT", Integer.class);
		if(fixedPort!=null) {
			mySQLContainer.addFixedExposedPort(fixedPort, 3306);
		}
		
		mySQLContainer.start();
		url = mySQLContainer.getJdbcUrl()+"?characterEncoding=UTF-8&useSSL=false";
		log.info("构建Mysql容器,url:{}",url);
		driverClass = mySQLContainer.getJdbcDriverInstance().getClass();
	}

	public void initScript(ResourceDatabasePopulator tmp) {
	}

}