@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestUtil.class,RedisAutoConfiguration.class })
public class SimpleTest extends AbstractJUnit4SpringContextTests {
	public SimpleTest() {
		System.out.println(("================初始化单元用例======================"));
		LazyBean.processAttr(this, this.getClass());
		TestUtil.configBeanFactory(BeanfactoryUtils.class);
		TestUtil.mapperScanPath = "com.xx.xx.mapper";
		TestUtil.dubboXml = "classpath*:/dubbo-context.xml";
//		TestUtil.configStatic(MultiServiceConfiguration.class);
	}
}