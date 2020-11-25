@RunWith(SpringRunner.class)
@ImportResource(locations = { "classpath:/properties-source.xml",
		"classpath:/applicationContext-datasource-mybatis.xml" })
@SpringBootTest(classes = { TestUtil.class,RedisAutoConfiguration.class })
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class SimpleTest extends AbstractJUnit4SpringContextTests {
	public SimpleTest() {
		System.out.println(("================初始化单元用例======================"));
		TestUtil.openTest();
		LazyBean.processAttr(this, this.getClass());
		TestUtil.configBeanFactory(SpringContextUtil.class);
		TestUtil.configStatic(MultiServiceConfiguration.class);
	}
}