@RunWith(SpringRunner.class)
@ImportResource(locations= {"classpath:/properties-source.xml","classpath:/applicationContext-datasource-mybatis.xml"})
@SpringBootTest(classes = {TestUtil.class,RedisAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@Slf4j
public abstract class TestDemo{
    public TestDemo() {
    	LazyBean.processAttr(this, this.getClass());
    	TestUtil.configBeanFactory(BeanfactoryUtils.class);
    	log.info("初始化单元用例");
    }
}