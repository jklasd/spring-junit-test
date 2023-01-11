package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.github.jklasd.test.common.interf.handler.BootHandler;
import com.github.jklasd.test.common.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MysqlContainersInitialization implements BootHandler{

	String findPath = "db-mysql";
	
	@Override
	public void launcher() {
		try {
			
			ResourceDatabasePopulator tmp = new ResourceDatabasePopulator();
			Resource[] schema = null;
			try {
				schema = ScanUtil.getResources("classpath:/"+findPath+"/schema/*");
				if(schema.length == 0) {
					return;
				}
				tmp.addScripts(schema);
			} catch (FileNotFoundException e) {
				return;
			}
			String databaseName = findDataBaseName(schema);
			try {
				Resource[] data = ScanUtil.getResources("classpath:/"+findPath+"/data/**");
				tmp.addScripts(data);
			} catch (FileNotFoundException e) {
			}
			
			
			EmbeddedDatabaseFactory defaultFactory = new EmbeddedDatabaseFactory();
			
			
			defaultFactory.setDatabaseConfigurer(new BaseTestContainers(databaseName));
			defaultFactory.setDatabasePopulator(tmp);
			defaultFactory.setDatabaseName(databaseName);
			
			EmbeddedDatabase source = defaultFactory.getDatabase();
			RoutingDataSourceExt.getInstance().initFunction(source);
			RoutingDataSourceExt.getInstance().setDataSource(source);
			
		} catch (IOException e) {
			if(e instanceof FileNotFoundException) {
				log.warn("load classpath:/db-mysql/ =>{}",e);
			}
		} catch (Exception e) {
			log.error("H2DatabaseInitialization",e);
		}
	}

	protected String findDataBaseName(Resource[] schemaes) throws IOException {
		if(schemaes.length == 1) {
			return nameConvert(schemaes[0].getFilename());
		}else {
			for(Resource res : schemaes) {
				if(res.isReadable()) {
					return nameConvert(res.getFilename());
				}
			}
			return nameConvert(schemaes[0].getFilename());
		}
	}

	protected String nameConvert(String fileName) {
		return fileName.endsWith(".sql")?fileName.replace(".sql", ""):fileName;
	}
}
