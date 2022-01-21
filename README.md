# spring-juint-test
spring-juint-test 是一个用于SpringBoot项目单元测试的工具包。



JUnit和SpringTest,基本上可以满足绝大多数的单元测试了，但是，由于现在的系统越来越复杂，相互之间的依赖越来越多。特别是微服务化以后的系统，往往一个模块的代码需要依赖几个其他模块的东西。因此，在做单元测试的时候，往往很难构造出需要的依赖。一个单元测试，我们只关心一个小的功能，但是为了这个小的功能能跑起来，可能需要依赖一堆其他的东西，这就导致了单元测试无法进行。

这时候，就有了Mock框架。

但是Mock测试的时候，这里还是需要一些准备工作的。

我希望测试类可以足够简洁，可以像正常Service调用一样，去测试。

比如：

```java
public class HttpTest extends SpringPluginTestBase{

	@Autowired
	private StockHttpService stockHttpService;
	
	@Test
	public void stockTest() {
		stockHttpService.collectData("");
	}
}
```