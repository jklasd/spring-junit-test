package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.github.jklasd.test.common.interf.handler.BootHandler;
import com.github.jklasd.test.common.util.LazyDataSourceUtil;
import com.github.jklasd.test.common.util.LazyDataSourceUtil.DataSourceBuilder;
import com.github.jklasd.test.common.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MysqlContainersInitialization implements BootHandler{

	String findPath = "db-mysql";
	
	@Override
	public void launcher() {
			DataSource dataSource = LazyDataSourceUtil.buildLazyDataSource(new DataSourceBuilder() {
				@Override
				public DataSource build() {
					try {
						ResourceDatabasePopulator tmp = new ResourceDatabasePopulator();
						String schemaPath = "classpath:/"+findPath+"/schema/*";
						addscript(schemaPath, tmp);
						addscript(schemaPath+"*", tmp);
						String databaseName = findDataBaseName(schemaPath);
						addscript("classpath:/"+findPath+"/data/**", tmp);
						
						BaseTestContainers baseContainer = new BaseTestContainers(databaseName);
						
						SimpleDriverDataSource source = new SimpleDriverDataSource();
						source.setUrl(baseContainer.getUrl());
						source.setDriverClass(baseContainer.getDriverClass());
						source.setUsername(baseContainer.getUserName());
						source.setPassword(baseContainer.getPassword());
						
						tmp.execute(source);
						//5.7 group问题
						source.getConnection().createStatement().execute("set @@GLOBAL.sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';");
						
						return source;
					} catch (IOException e) {
						if(e instanceof FileNotFoundException) {
							log.warn("load classpath:/db-mysql/ =>{}",e);
						}
					} catch (IllegalStateException e) {
						if(e.getMessage().contains(" a valid Docker")
								|| e.getMessage().contains("Docker environment failed")) {
							log.warn(e.getMessage());
						}else {
							log.error("构建mysql 容器异常",e);
						}
					} catch (Exception e) {
						log.error("MysqlContainerDatabaseInitialization",e);
					}
					return null;
				}
			});
			
			
			RoutingDataSourceExt sourceRount = RoutingDataSourceExt.getInstance();
			sourceRount.setDataSource(dataSource);
			
	}

	private void addscript(String path, ResourceDatabasePopulator tmp) throws IOException {
		try {
			Resource[] rs = ScanUtil.getResources(path);
			if(rs.length == 0) {
				return;
			}
			for(Resource r : rs) {
				if(r.isReadable() && !r.getFile().isDirectory()) {
					tmp.addScript(r);
				}
			}
		} catch (FileNotFoundException e) {
			return;
		}
	}

	protected String findDataBaseName(String path) throws IOException {
		Resource[] schemaes = ScanUtil.getResources(path);
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
