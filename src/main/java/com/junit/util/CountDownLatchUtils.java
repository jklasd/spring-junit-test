package com.junit.util;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * 多线程控制器
 * @author jubin.zhang
 *
 * @param <T>
 */
@Slf4j
public class CountDownLatchUtils<T> extends TraversingUtils<T, CountDownLatch>{
	private long timeOut;
	
	private CountDownLatchUtils(List<T> listData) {
		super(listData, new CountDownLatch(listData.size()));
	}
	
	public static <T> CountDownLatchUtils<T> buildCountDownLatch(List<T> listData) {
		return new CountDownLatchUtils<>(listData);
	}
	
	public static <T> CountDownLatchUtils<T> buildCountDownLatch(List<T> listData,long timeOut) {
		CountDownLatchUtils<T> cdlu = new CountDownLatchUtils<>(listData);
		cdlu.timeOut = timeOut;
		return cdlu;
	}
	/**
	 * item
	 * @param action
	 * @return
	 */
	public boolean runAndWait(Consumer<? super T> action) {
		boolean await = false;
		for(T tmp : list) {
			ThreadPoolUtils.commonRun(()->{
				try {
					action.accept(tmp);
				} finally {
					obj.countDown();
				}
			});
		}
		try {
			if(timeOut>0) {
				await = obj.await(timeOut,TimeUnit.SECONDS);
			}else {
				obj.await();
			}
		} catch (Exception e) {
			log.error("CountDownLatchUtils#runAndWait多线程控制",e);
			return await;
		}
		return await;
	}
	/**
	 * item ,countDownLatch ,index
	 * @param action
	 */
	public void runAndWait(TraversingFunction<? super T,? super CountDownLatch,Integer> action) {
		AtomicInteger index = new AtomicInteger();
		for(T tmp : list) {
			index.incrementAndGet();
			ThreadPoolUtils.commonRun(()->{
				try {
					action.accept(tmp,obj,index.get());
				} finally {
					obj.countDown();
				}
			});
		}
		try {
			if(timeOut>0) {
				obj.await(timeOut,TimeUnit.SECONDS);
			}else {
				obj.await();
			}
		} catch (Exception e) {
			log.error("CountDownLatchUtils#runAndWait多线程控制",e);
		}
	}
	/**
	 * item ,index
	 * @param action
	 */
	public void runAndWaitForIndex(BiConsumer<? super T,Integer> action) {
		AtomicInteger index = new AtomicInteger();
		for(T tmp : list) {
			index.incrementAndGet();
			ThreadPoolUtils.commonRun(()->{
				try {
					action.accept(tmp,index.get());
				} finally {
					obj.countDown();
				}
			});
		}
		try {
			if(timeOut>0) {
				obj.await(timeOut,TimeUnit.SECONDS);
			}else {
				obj.await();
			}
		} catch (Exception e) {
			log.error("CountDownLatchUtils#runAndWaitForIndex多线程控制",e);
		}
	}
	/**
	 * item , countDownLatch
	 * @param action
	 */
	public void runAndWait(BiConsumer<? super T, ? super CountDownLatch> action) {
		for(T tmp : list) {
			ThreadPoolUtils.commonRun(()->{
				try {
					action.accept(tmp,obj);
				} finally {
					obj.countDown();
				}
			});
		}
		try {
			if(timeOut>0) {
				obj.await(timeOut,TimeUnit.SECONDS);
			}else {
				obj.await();
			}
		} catch (Exception e) {
			log.error("CountDownLatchUtils#runAndWaitForIndex多线程控制",e);
		}
	}
}