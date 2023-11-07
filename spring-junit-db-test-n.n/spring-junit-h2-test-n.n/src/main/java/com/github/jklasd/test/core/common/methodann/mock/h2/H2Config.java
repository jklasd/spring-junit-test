package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer;
import org.springframework.util.ClassUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class H2Config implements EmbeddedDatabaseConfigurer{

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		try {
			properties.setDriverClass((Class<? extends Driver>)
					ClassUtils.forName("org.h2.Driver", H2Config.class.getClassLoader()));
		} catch (ClassNotFoundException | LinkageError e) {
			e.printStackTrace();
		}
		properties.setUrl(String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL;DB_CLOSE_ON_EXIT=false", databaseName));
		properties.setUsername("sa");
		properties.setPassword("");
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
