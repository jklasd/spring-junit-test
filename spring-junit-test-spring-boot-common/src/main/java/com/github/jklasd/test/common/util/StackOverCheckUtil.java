package com.github.jklasd.test.common.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.jklasd.test.common.exception.JunitException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StackOverCheckUtil {
	
	public interface StackOverCheckI{
		public Object run();	
	}
	public interface StackOverCheckThrowExcepitonI{
		public Object run()  throws Throwable;	
	}
	
	private static ThreadLocal<AtomicInteger> stackOverCheck = new InheritableThreadLocal<AtomicInteger>();
	
	public static Object observeThrowException(StackOverCheckThrowExcepitonI execTrack) throws Throwable{
		LogbackUtil.setTraceId();
		Object result = execTrack.run();
		LogbackUtil.clearTraceId();
        return result;
	}
	
	public static Object observeIgnoreException(StackOverCheckI execTrack){
		LogbackUtil.setTraceId();
		Object result = execTrack.run();
		LogbackUtil.clearTraceId();
        return result;
	}
	
	public static Object observe(StackOverCheckI execTrack,Object... args) {
		if(stackOverCheck.get()!=null && stackOverCheck.get().get()>1) {
			log.info("observe=====args=====>{}",args);
		}
		Object result = execTrack.run();
		if(stackOverCheck.get()!=null && stackOverCheck.get().get()>1) {
			log.info("observe=====return=====>{}",result);
		}
        return result;
	}
	
	public static Object exec(StackOverCheckI execTrack,Object... args) {
		if(stackOverCheck.get()==null) {
        	stackOverCheck.set(new AtomicInteger());
        }
        stackOverCheck.get().incrementAndGet();
        if(stackOverCheck.get().get()>5) {
        	log.error("------------异常查找-------------");
        	if(args.length>0) {
        		log.error("------------异常查找--args[{}]-----------",args);
        	}
        	stackOverCheck.remove();
        	throw new JunitException("*************递归查找问题*************");
        }
        Object result = execTrack.run();
        
        stackOverCheck.remove();
        return result;
	}
}
