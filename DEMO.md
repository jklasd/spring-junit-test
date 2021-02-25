# 使用demo

```java
public class SimpleTest{

	public SimpleTest() {
		System.out.println(("================初始化单元用例======================"));
		TestUtil.startTestForNoContainer(this);
	}
}
```

```java
public class Demo extends SimpleTest {

    @Autowired
    private IXXX demo;
    @Resource
    private IXXX2 demo2;
    
    @Test
    public void test(){
    	//执行业务
    	demo.testProcess();
    }
}
```

测试用例继承SimpleTest类，然后执行@Test方法。