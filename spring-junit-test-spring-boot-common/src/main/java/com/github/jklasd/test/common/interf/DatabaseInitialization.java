package com.github.jklasd.test.common.interf;

import javax.sql.DataSource;

public interface DatabaseInitialization extends ContainerRegister{

	boolean isInitDataSource();
	DataSource build(DataSource dataSource);

}
