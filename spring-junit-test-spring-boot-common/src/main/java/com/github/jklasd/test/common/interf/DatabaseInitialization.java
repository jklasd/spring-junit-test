package com.github.jklasd.test.common.interf;

import javax.sql.DataSource;

public interface DatabaseInitialization {

	boolean isInitDataSource();
	DataSource build(DataSource dataSource);

}
