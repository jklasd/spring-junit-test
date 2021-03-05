package com.github.jklasd.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 
 * @author jubin.zhang
 *
 * @param <T> 遍历对象类型
 * @param <CountDownLatch> 自定义CountDownLatch 处理对象
 */
public class TraversingUtils<T,CountDownLatch> {
	public interface TraversingFunction<T,CountDownLatch,V>{

		void accept(T t,CountDownLatch r,V v);
		
	}
	protected List<T> list;
	protected CountDownLatch obj;
	protected TraversingUtils(List<T> listData,CountDownLatch obj) {
		this.list = listData;
		this.obj = obj;
	}
	
	public void forEach(TraversingFunction<? super T, CountDownLatch ,Integer> action) {
		AtomicInteger index = new AtomicInteger();
		for(T tmp : list) {
			index.incrementAndGet();
			action.accept(tmp, obj,index.get());
		}
	}
	public void forEach(BiConsumer<? super T, CountDownLatch> action) {
		for(T tmp : list) {
			action.accept(tmp, obj);
		}
	}
	public void forEach(Consumer<? super T> action) {
		for(T tmp : list) {
			action.accept(tmp);
		}
	}
}