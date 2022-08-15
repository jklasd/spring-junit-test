package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.interf.handler.BootHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class H2DatabaseInitialization implements BootHandler{

	private EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
	
	String findPath = "db-h2";
	
	DataSource dataSource;
	
	@Override
	public void launcher() {
		try {
			Resource[] schema = TestUtil.getInstance().getApplicationContext().getResources("classpath:/"+findPath+"/schema/*");
			for(Resource res : schema) {
				String path = res.getURI().getPath();
				path = path.substring(path.indexOf(findPath));
				embeddedDatabaseBuilder.addScript("classpath:/"+path);
			}
			Resource[] data = TestUtil.getInstance().getApplicationContext().getResources("classpath:/"+findPath+"/data/*");
			
			for(Resource res : data) {
				String path = res.getURI().getPath();
				path = path.substring(path.indexOf(findPath));
				embeddedDatabaseBuilder.addScript("classpath:/"+path);
			}
			dataSource = embeddedDatabaseBuilder.setType(EmbeddedDatabaseType.H2).build();
			RoutingDataSourceExt.getInstance().setDataSource(dataSource);
			
		} catch (IOException e) {
			if(e instanceof FileNotFoundException) {
				log.warn("load classpath:/db-h2/ =>{}",e.getMessage());
			}
		} catch (Exception e) {
			log.error("H2DatabaseInitialization",e);
		}
	}
}
