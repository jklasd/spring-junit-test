[toc]

# Spring-Junit-Test



## 1、简介

Spring-junit-test 是一个用于SpringBoot项目单元测试的工具包。

#### 产生背景

JUnit和SpringTest,基本上可以满足绝大多数的单元测试了，但是，由于现在的系统越来越复杂，相互之间的依赖越来越多。特别是微服务化以后的系统，往往一个模块的代码需要依赖几个其他模块的东西。因此，在做单元测试的时候，往往很难构造出需要的依赖。

Spring项目在Junit测试，按照IOC的启动方式，需要预先准备好所有的环境，才去执行junit task。在准备环境的这个过程，非常耗时。如果需要连接的中间件越多，则消耗的时间越多，在我们本地执行一下junit task则非常困难。平均每次启动到开始执行task的时间都要5~10分钟。这也导致很多开发人员对junit task的厌恶，拒绝。



一个单元测试，我们只关心一个小的功能。比如我现在只想执行一个任务，这个任务只做了mysql查询，和数据转换。我没有用到redis，没有用到远程服务，没有其他定时器等等。那我执行这一个junit task就没必要去构建与这个task无关的环境准备工作。



Spring-junit-test 插件，就是为了解决这个问题，而产生的。以下是使用Spring-junit-test插件的方式。

比如：

```java
@Runwith(RunSpringJunitTest.class)
public class HttpTest{

	@Autowired
	private StockHttpService stockHttpService;
	
	@Test
	public void stockTest() {
		stockHttpService.collectData("");
	}
}
```



#### 适用痛点场景

1、远程开发环境，网络环境差

2、分布式项目开发

#### 适用场景

1、业务代码测试

#### 不适用场景

1、架构开发测试

## 2、使用方式

Spring-Junit-Test的使用方式分为Junit4和Junit5

#### Junit4的使用方式

```java
@Runwith(RunSpringJunitTest.class)
public class HttpTest{

	@Autowired
	private StockHttpService stockHttpService;
	
	@Test
	public void stockTest() {
		stockHttpService.collectData("");
	}
}
```

#### Junit5的使用方式

```java
@RunSpringJunitTestFor5
public class HttpTest{

	@Autowired
	private StockHttpService stockHttpService;
	
	@Test
	public void stockTest() {
		stockHttpService.collectData("");
	}
}
```



#### maven 配置

```xml
<dependency>
    <groupId>com.github.jklasd</groupId>
    <artifactId>spring-junit-test-boot-version-controller</artifactId>
    <version>2.4.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.github.jklasd</groupId>
            <artifactId>spring-junit-test-common</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.github.jklasd</groupId>
    <artifactId>spring-junit-test</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

spring-junit-test-boot-version-controller 是用于对springboot的版本兼容性处理的。

当如果你使用的2.x一下的，则请选择

```xml
<dependency>
    <groupId>com.github.jklasd</groupId>
    <artifactId>spring-junit-test-boot-version-controller</artifactId>
    <version>1.5.0</version>
</dependency>
```



#### 对DB的支持方式

spring-junit-test 对db的处理支持通过mysql容器和H2两种方式，实现可重复使用的sql和查询数据，方便java单元测试重复执行。

##### 对mysql容器的支持

* 配置初始化语句

```
表结构初始化
src/test/resources/db-mysql/schema/*.sql 

跨库表结构初始化【针对某些项目需要跨库查询，可初始化其他库的表结构】
src/test/resources/db-mysql/schema/cross-library/*.sql

表数据初始化
src/test/resources/db-mysql/data/*.sql

```

* @JunitMysqlContainerSelected注解使用

```java
/*
* 放在class上面，全部的method都是用虚拟mysql容器数据库
*/
@JunitMysqlContainerSelected
@RunSpringJunitTestFor5
public class MysqlTest {
	@Autowired
	private TestService testService;
	
	@Autowired
	private TestService2 testService2;
	
	@Test
	public void findData() {
		testService2.getData("81800000008", 1l);
	}
	
	@Test
	public void findData2() {
		
		TestData data = testService.getData(2);
		
		System.out.println(data!=null);
		
		System.out.println(data.getTitle());
		
	}
}
```

```java

@RunSpringJunitTestFor5
public class MysqlTest {
	@Autowired
	private TestService testService;
	
	@Autowired
	private TestService2 testService2;
	
	@Test
	public void findData() {
		testService2.getData("81800000008", 1l);
	}
    
    /*
    * 放在method上面，指定某个method都是用虚拟mysql容器数据库
    */
	@JunitMysqlContainerSelected
	@Test
	public void findData2() {
		
		TestData data = testService.getData(2);
		
		System.out.println(data!=null);
		
		System.out.println(data.getTitle());
		
	}
}
```



```java
/*
* 放在class上面，全部的method都是用虚拟mysql容器数据库
*/
@JunitMysqlContainerSelected
@RunSpringJunitTestFor5
public class MysqlTest {
	@Autowired
	private TestService testService;
	
	@Autowired
	private TestService2 testService2;
	
	@Test
	public void findData() {//这个方法使用虚拟mysql容器数据库
		testService2.getData("81800000008", 1l);
	}
    
    
	@JunitMysqlContainerSelected(false)//指定某个method不使用虚拟mysql容器数据库
	@Test
	public void findData2() {//这个方法使用项目配置application.properties/application.yml配置的数据库
		
		TestData data = testService.getData(2);
		
		System.out.println(data!=null);
		
		System.out.println(data.getTitle());
		
	}
}
```





##### 对H2数据库的支持

* 配置初始化语句

```
表结构初始化
src/test/resources/db-h2/schema/*.sql 

跨库表结构初始化【针对某些项目需要跨库查询，可初始化其他库的表结构】
src/test/resources/db-h2/schema/cross-library/*.sql

表数据初始化
src/test/resources/db-h2/data/*.sql
```

* @JunitH2Selected注解使用

```
同@JunitMysqlContainerSelected
```

* @JunitMysqlToH2注解使用【用于替换H2不支持的mysql语句，替换成标准sql】

```java
/*
*由于Mysql 和 H2的兼容性问题
*/
@JunitH2Selected//放在class上面，全部的method都是用H2数据库
@RunSpringJunitTestFor5
public class MysqlTest {
	@Autowired
	private TestService testService;
	
	@Autowired
	private TestService2 testService2;
	
	@Test
	@JunitMysqlToH2(from = {"LAST_DAY(DATE_ADD(CURDATE()-DAY(CURDATE())+1,INTERVAL 1 MONTH))",
    		"DATE_ADD(CURDATE()-DAY(CURDATE())+1,INTERVAL 1 MONTH)"},
    to = {"'2022-09-30'","'2022-09-01'"})//用于mysql的特殊函数替换成简单值或者标准sql，针对历史sql处理问题，不修改业务代码。
	public void findData() {//这个方法使用H2数据库
		testService2.getData("81800000008", 1l);
	}
    
    
	@JunitH2Selected(false)//指定某个method不使用H2数据库
	@Test
	public void findData2() {//这个方法使用项目配置application.properties/application.yml配置的数据库
		
		TestData data = testService.getData(2);
		
		System.out.println(data!=null);
		
		System.out.println(data.getTitle());
		
	}
}
```