package com.github.jklasd.test.common.interf;

import javax.sql.DataSource;

public interface DatabaseInitialization {

	DataSource build(DataSource dataSource);

}
