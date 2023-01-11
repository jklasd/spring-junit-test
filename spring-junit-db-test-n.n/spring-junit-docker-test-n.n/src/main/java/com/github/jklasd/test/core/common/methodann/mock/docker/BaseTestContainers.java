package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer;
import org.springframework.util.ClassUtils;
import org.testcontainers.containers.MySQLContainer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseTestContainers implements EmbeddedDatabaseConfigurer{
	public MySQLContainer mySQLContainer;
	private String url;
	private String userName="junit";
	private String password="junit";
	private String className;

	public BaseTestContainers(String databaseName) {
		mySQLContainer = new MySQLContainer("mysql:5.7")
		            .withUsername(userName)
		            .withPassword(password)
		            .withDatabaseName(databaseName);
		
		url = mySQLContainer.getJdbcUrl();
		className = mySQLContainer.getDriverClassName();
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		try {
			properties.setDriverClass((Class<? extends Driver>)ClassUtils.forName(className, BaseTestContainers.class.getClassLoader()));
		} catch (ClassNotFoundException | LinkageError e) {
			e.printStackTrace();
		}
		properties.setUrl(url);
		properties.setUsername(userName);
		properties.setPassword(password);
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.createStatement().execute("SHUTDOWN");
		}
		catch (SQLException ex) {
			log.warn("Could not shut down embedded database", ex);
		}
		finally {
			if (con != null) {
				try {
					con.close();
				}
				catch (Throwable ex) {
					log.debug("Could not close JDBC Connection on shutdown", ex);
				}
			}
		}
	}

}