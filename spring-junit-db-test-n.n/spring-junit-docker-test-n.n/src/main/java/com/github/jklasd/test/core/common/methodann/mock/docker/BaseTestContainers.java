package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.sql.Driver;

import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseTestContainers{
	
	private MySQLContainer<?> mySQLContainer;
	@Getter
	private String url;
	@Getter
	private String userName="root";
	@Getter
	private String password="root";
	@Getter
	private Class<? extends Driver> driverClass;

	@SuppressWarnings({ "unchecked", "rawtypes", "resource" })
	public BaseTestContainers(String databaseName) {
		mySQLContainer = new MySQLContainer("mysql:5.7")
		            .withUsername(userName)
		            .withPassword(password)
		            .withDatabaseName(databaseName);
		
		Integer fixedPort = LazyApplicationContext.getInstance().getEnvironment().getProperty("MYSQL_CONTAINER_PORT", Integer.class);
		
		Integer maxContainerMemory = LazyApplicationContext.getInstance().getEnvironment().getProperty("MAX_CONTAINER_MEMORY", Integer.class,256);
		if(fixedPort!=null) {
			mySQLContainer.withCreateContainerCmdModifier(cmd ->{
				CreateContainerCmd ccc = cmd;
				HostConfig hostConfig = new HostConfig();
				hostConfig.withPortBindings(new PortBinding(Ports.Binding.bindPort(fixedPort), new ExposedPort(3306)));
				hostConfig.withMemory(maxContainerMemory*1024*1024L);
				ccc.withHostConfig(hostConfig);
			});
		}
		
		mySQLContainer.start();
		url = mySQLContainer.getJdbcUrl()+"?characterEncoding=UTF-8&useSSL=false";
		log.info("构建Mysql容器,url:{}",url);
		driverClass = mySQLContainer.getJdbcDriverInstance().getClass();
	}

	public void initScript(ResourceDatabasePopulator tmp) {
	}

}