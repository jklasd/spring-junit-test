package com.github.jklasd.util;

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
 * @param <T> 遍历对象类型
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
	private BiConsumer<? super T,? super Exception> exception;
	private BiConsumer<? super T,? super Error> error;
	public CountDownLatchUtils<T> setError(BiConsumer<? super T,? super Error> error) {
		this.error = error;
		return this;
	}
	public CountDownLatchUtils<T> setException(BiConsumer<? super T,? super Exception> exception) {
		this.exception = exception;
		return this;
	}
	/**
	 * item
	 * @param action 处理方法体，方法体中可以获取遍历对象
	 * @return true 表示线程执行完成，false表示执行超时
	 */
	public boolean runAndWait(Consumer<? super T> action) {
		boolean await = false;
		for(T tmp : list) {
			ThreadPoolUtils.commonRun(()->{
				try {
					action.accept(tmp);
				}catch(Exception e) {
					if(exception!=null) {
						exception.accept(tmp,e);
					}else {
						throw e;
					}
				}catch(Error err) {
					if(error!=null) {
						error.accept(tmp,err);
					}else {
						throw err;
					}
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
	 * @param  action 处理方法体，方法体中可以获取遍历对象
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
	 * @param  action 处理方法体，方法体中可以获取遍历对象
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
	 * @param  action 处理方法体，方法体中可以获取遍历对象
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