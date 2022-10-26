package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;

import com.github.jklasd.test.common.interf.handler.BootHandler;
import com.github.jklasd.test.common.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class H2DatabaseInitialization implements BootHandler{

	String findPath = "db-h2";
	
	@Override
	public void launcher() {
		try {
			
			DatabasePopulatorExt tmp = new DatabasePopulatorExt();
			Resource[] schema = null;
			try {
				schema = ScanUtil.getResources("classpath:/"+findPath+"/schema/*");
				if(schema.length == 0) {
					return;
				}
			} catch (FileNotFoundException e) {
				return;
			}
			tmp.addSchema(schema);
			String databaseName = findDataBaseName(schema);
			findOtherSchema(schema,tmp);
			
			try {
				Resource[] data = ScanUtil.getResources("classpath:/"+findPath+"/data/**");
				tmp.addData(data);
			} catch (FileNotFoundException e) {
			}
			
			
			EmbeddedDatabaseFactory defaultFactory = new EmbeddedDatabaseFactory();
			
			
			defaultFactory.setDatabaseConfigurer(new H2Config());
			defaultFactory.setDatabasePopulator(tmp);
			defaultFactory.setDatabaseName(databaseName);
			
			EmbeddedDatabase source = defaultFactory.getDatabase();
			RoutingDataSourceExt.getInstance().initFunction(source);
			RoutingDataSourceExt.getInstance().setDataSource(source);
			
		} catch (IOException e) {
			if(e instanceof FileNotFoundException) {
				log.warn("load classpath:/db-h2/ =>{}",e);
			}
		} catch (Exception e) {
			log.error("H2DatabaseInitialization",e);
		}
	}
	/**
	 * 如果存在其他库，需要提供跨库查询，则需要建立一个文件夹
	 * @param schema
	 * @param tmp 
	 * @throws IOException 
	 */
	private void findOtherSchema(Resource[] resources, DatabasePopulatorExt tmp) throws IOException {
		for(Resource s : resources) {
			if(!s.isReadable()) {
				Resource[] crossLibrary = ScanUtil.getResources("classpath:/"+findPath+"/schema/**");
				tmp.addSchema(crossLibrary);
				return;
			}
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
