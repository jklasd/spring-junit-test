package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.component.MockAnnHandlerComponent;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlInterceptor implements ContainerRegister{

	private Class<?> interceptorClass;
	private Class<?> executorClass;
	private Class<?> pluginClass;
//	private Class<?> boundSqlClass;
	private Class<?> sqlSourceClass;
//	private Method wrap;
	
	Map<Class<?>, Set<Method>> signatureMap;
	
	{
		interceptorClass = ScanUtil.loadClass("org.apache.ibatis.plugin.Interceptor");
		executorClass = ScanUtil.loadClass("org.apache.ibatis.executor.Executor");
		pluginClass = ScanUtil.loadClass("org.apache.ibatis.plugin.Plugin");
		sqlSourceClass = ScanUtil.loadClass("org.apache.ibatis.mapping.SqlSource");
//		boundSqlClass = ScanUtil.loadClass("org.apache.ibatis.mapping.BoundSql");
		
		
		signatureMap = Maps.newHashMap();
		signatureMap.put(executorClass, Sets.newHashSet());
		Method[] ms = executorClass.getMethods();
		for (Method m : ms) {
			if (Objects.equal(m.getName(), "query")) {
				signatureMap.get(executorClass).add(m);
			}
		}
	}
	private Object ibatisSqlInterceptor;
	public synchronized Object buildInterceptor() {
		if(ibatisSqlInterceptor==null) {
			ibatisSqlInterceptor = Proxy.newProxyInstance(JunitClassLoader.getInstance(), new Class[] { interceptorClass }, new IbatisSqlInterceptor());
		}
		return ibatisSqlInterceptor;
	}
	
	public class IbatisSqlInterceptor implements InvocationHandler{
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
			case "intercept":
				if(MockAnnHandlerComponent.isMock(JunitH2Selected.class.getName())) {
					resetSql2Invocation(args[0]);
				}
		        return JunitInvokeUtil.invokeMethod(args[0], "proceed");
			case "plugin":
				Object target = args[0];
				if(MockAnnHandlerComponent.isMock(JunitH2Selected.class.getName())) {
					/**
					 * @Intercepts(
				        {
				                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
				                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
				        }
				)
					 * 
					 */
					Class<?> type = target.getClass();
					Class<?>[] interfaces = getAllInterfaces(target.getClass(), signatureMap);
					if (interfaces.length > 0) {
						Constructor<?> c = pluginClass.getDeclaredConstructors()[0];
						if (!c.isAccessible()) {
							c.setAccessible(true);
						}
						return Proxy.newProxyInstance(type.getClassLoader(), interfaces,
								(InvocationHandler) c.newInstance(target, buildInterceptor(), signatureMap));
					}
				}
				return target;
			case "setProperties":
				break;
			default:
				break;
			};
			return null;
		}
	}
	
	@AllArgsConstructor
	public class IbatisSqlSourceInterceptor implements InvocationHandler{
		
		private Object boundSql;
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return boundSql;
		}
	}
	private String getSqlByInvocation(Object invocation) {
        final Object[] args = (Object[]) JunitInvokeUtil.invokeMethod(invocation, "getArgs");
        Object ms = args[0];
        Object parameterObject = args[1];
        Object boundSql = JunitInvokeUtil.invokeMethodSignParam(ms, "getBoundSql",Object.class, parameterObject); // ms.getBoundSql(parameterObject);//(MappedStatement) 
        return JunitInvokeUtil.invokeMethod(boundSql,"getSql").toString();
    }
	/**
	 * 只做关键字替换，不做参数替换
	 * @param invocation
	 * @throws SQLException
	 */
	private void resetSql2Invocation(Object invocation) throws SQLException {
		SqlReplaceMysqlToH2Handler handler = (SqlReplaceMysqlToH2Handler) MockAnnHandlerComponent.getHandler(JunitMysqlToH2.class.getName());
		JunitMysqlToH2 sqlData = handler.getData();//存在父子线程消息传递问题
		
		final Object[] args = (Object[]) JunitInvokeUtil.invokeMethod(invocation, "getArgs");
        Object statement = args[0];
        Object parameterObject = args[1];
        Object boundSql = JunitInvokeUtil.invokeMethodSignParam(statement, "getBoundSql",Object.class,parameterObject);
        
//        Object sqlSource = JunitInvokeUtil.invokeMethod(statement, "getSqlSource");
        
        String sql = JunitInvokeUtil.invokeMethod(boundSql,"getSql").toString();
        if(sqlData != null) {
        	log.info("sql:{}",sql);
        	log.info("存在sql替换:{}",sqlData);
        	String[] replaceSql = handler.getData().from();
        	String[] toSql = handler.getData().to();
        	for(int i=0;i<replaceSql.length;i++) {
        		sql = sql.replace(replaceSql[i], toSql[i]);
        	}
//        	log.debug("sql替换后:{}",sql);
		}
        sql = sql.replace("IF(","IF_(").replace("if(", "IF_(").replace("If(", "IF_(");
        sql = sql.replace("ISNULL(","ISNULL_(");
//        sql = sql.replace("NOW(","NOW_(");
        
        JunitInvokeUtil.invokeWriteField("sql", boundSql, sql);
        //生成新的代理sqlSource 存入里面
        JunitInvokeUtil.invokeWriteField("sqlSource", statement, Proxy.newProxyInstance(JunitClassLoader.getInstance(), new Class[] { sqlSourceClass }, 
        		new IbatisSqlSourceInterceptor(boundSql)));
    }

	@Override
	public void register() {
		ContainerManager.registComponent(this);
	}

	@Override
	public String getBeanKey() {
		return ContainerManager.NameConstants.SqlInterceptor;
	}
	
	private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
	    Set<Class<?>> interfaces = new HashSet<>();
	    while (type != null) {
	      for (Class<?> c : type.getInterfaces()) {
	        if (signatureMap.containsKey(c)) {
	          interfaces.add(c);
	        }
	      }
	      type = type.getSuperclass();
	    }
	    return interfaces.toArray(new Class<?>[0]);
	  }

}
