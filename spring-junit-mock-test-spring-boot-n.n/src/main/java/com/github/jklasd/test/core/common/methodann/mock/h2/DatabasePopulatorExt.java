package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * 数据库初始化
 * @author jubin.zhang
 *
 */
public class DatabasePopulatorExt implements DatabasePopulator {
	
	List<Resource> schemaScripts = Lists.newArrayList();
	Set<String> existsPath = Sets.newHashSet();
	public void addSchema(Resource... resources) throws IOException {
		for(Resource res:resources) {
			String path = res.getURI().getPath();
			if(res.isReadable() && !existsPath.contains(path)) {
				existsPath.add(path);
				schemaScripts.add(res);
			}
		}
	}
	List<Resource> dataScripts = Lists.newArrayList();
	public void addData(Resource... resources) {
		for(Resource res:resources) {
			if(res.isReadable()) {
				dataScripts.add(res);
			}
		}
	}
	
	public void populate(Connection connection) throws ScriptException {
		Assert.notNull(connection, "Connection must not be null");
		
		schemaScripts.forEach(item->{
			EncodedResource encodedScript = new EncodedResource(item);
			ScriptUtilsExt.executeSqlScript(connection, encodedScript);
		});
		
		dataScripts.forEach(item->{
			EncodedResource encodedScript = new EncodedResource(item);
			ScriptUtils.executeSqlScript(connection, encodedScript);
		});
		
		existsPath.clear();
	}

}
