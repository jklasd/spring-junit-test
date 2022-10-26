package com.github.jklasd.test.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.LockSupport;

import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JunitThreadUtil{
	public static void wait(Class<?> executorOf ,String fieldName ) {
		Object bean = LazyApplicationContext.getInstance().getProxyBeanByClass(executorOf);
		ThreadPoolExecutor executor = (ThreadPoolExecutor) JunitInvokeUtil.invokeReadField(fieldName,
				LazyProxyManager.isProxy(bean)?LazyProxyManager.getProxyTagObj(bean):bean);
		wait(executor);
	}
	public static void wait(String fieldName ,String beanName) {
		Object bean = LazyApplicationContext.getInstance().getBean(beanName);
		ThreadPoolExecutor executor = (ThreadPoolExecutor) JunitInvokeUtil.invokeReadField(fieldName,
				LazyProxyManager.isProxy(bean)?LazyProxyManager.getProxyTagObj(bean):bean);
		wait(executor);
	}
	public static void wait(ThreadPoolExecutor executor) {
		JunitThreadFactory jtf = new JunitThreadFactory();
		executor.setThreadFactory(jtf);
		
		while(executor.getActiveCount()>0) {
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			log.info("线程池还有数量:{}",executor.getActiveCount());
		}
		
		jtf.waitting();
	}
	
	static class JunitThreadFactory implements ThreadFactory{
		volatile Thread catchThread;
		@Override
		public Thread newThread(Runnable r) {
			if(r == null) {
				log.info("==========******************============线程池执行完毕,可以释放主线程==========******************=======");
				LockSupport.unpark(catchThread);
			}
			waitting = false;
			return new Thread(r);
		}
		
		private volatile boolean waitting;

		public void waitting() {
			catchThread = Thread.currentThread();
			waitting = true;
			new Thread(()->{
					if(waitting) {
						try {
							Thread.sleep(5000l);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if(waitting) {//5s之后还没有线程构造，则唤醒主线程
						LockSupport.unpark(catchThread);
					}
			}).start();
			LockSupport.park();
		}
	}
}
