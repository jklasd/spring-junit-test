package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据源切换
 * 
 * 使用时，切换到MysqlContainer数据库
 * @author jubin.zhang
 * 
 * 如果是使用远程Docker镜像，请配置DOCKER_HOST,例:tcp://127.0.0.1:2375<br/>
 * 
 * MYSQL_CONTAINER_PORT，配置后固定端口号，如果是公共资源，建议不设置值<br/>
 * RYUK_CONTAINER_PORT，配置后固定端口号，如果是公共资源，建议不设置值<br/>
 * 
 * docker 开启远程配置，参考https://www.php1.cn/detail/dockerdaemon_Yua_afdecdc6.html
 *
 */
@Target({ElementType.METHOD,ElementType.TYPE})//指定位置
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JunitMysqlContainerSelected {
	/**
	 * 是否使用mysql容器数据库
	 * @return
	 */
	boolean value() default true;
	
	/**
	 * 当定义在class上时使用，控制当前表示当前junit class用到的数据源写入。<br/>
	 * 
	 * insertResource,寻址源头是从src/test/resources去找，路径可以自定义
	 * 
	 * 表的创建建议统一写到db-mysql/schema <br/>
	 * db-mysql/data下也可以写一些插入公共数据
	 * @return
	 */
	String[] insertResource() default "";
}
