@RunWith(SpringRunner.class)

@SpringBootTest(classes = { TestUtil.class})

public class SimpleTest{

	public SimpleTest() {
		System.out.println(("================初始化单元用例======================"));
		LazyBean.processAttr(this, this.getClass());
		TestUtil.mapperScanPath = "com.xx.xx.mapper";
		TestUtil.dubboXml = "classpath*:/dubbo-context.xml";
	}
}
