@ImportResource(locations= {"classpath:/properties-source.xml","classpath:/applicationContext-datasource-mybatis.xml"})
@SpringBootTest(classes = {TestUtil.class,RedisAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@RunWith(SpringRunner.class)
public class TestDemo{

    public TestDemo() {
    	LazyBean.processAttr(this, getClass());
    }
}