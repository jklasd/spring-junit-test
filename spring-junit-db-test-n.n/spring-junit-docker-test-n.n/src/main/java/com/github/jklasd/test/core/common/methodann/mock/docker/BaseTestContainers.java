package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.sql.Driver;

import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseTestContainers{
	private MySQLContainer mySQLContainer;
	@Getter
	private String url;
	@Getter
	private String userName="root";
	@Getter
	private String password="root";
	@Getter
	private Class<? extends Driver> driverClass;

	public BaseTestContainers(String databaseName) {
		mySQLContainer = new MySQLContainer("mysql:5.7")
		            .withUsername(userName)
		            .withPassword(password)
		            .withDatabaseName(databaseName);
		
		mySQLContainer.start();
		url = mySQLContainer.getJdbcUrl()+"?characterEncoding=UTF-8&useSSL=false";
		log.info("构建Mysql容器,url:{}",url);
		driverClass = mySQLContainer.getJdbcDriverInstance().getClass();
	}

	public void initScript(ResourceDatabasePopulator tmp) {
	}

}