# spring-juint-test
spring-juint-test 是一个用于Spring项目单元测试对工具包。
由于我们通常情况下使用@RunWIth(SpringJunit4ClassRunner.class)会启动整个项目，导致执行一个JunitTest 任务会很慢。
如果用使用spring-juint-test工具包，则可以帮助你在执行JuintTest 任务时不需要启动整个项目。
只构建和实例化你要执行对Juint任务中使用到的@Service/@Component/@Configruation 等等
