# Spring-Junit-Test



## 1、简介

Spring-junit-test 是一个用于SpringBoot项目单元测试的工具包。

##### 产生背景

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