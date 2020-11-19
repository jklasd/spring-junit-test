package com.junit.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author jubin.zhang
 *
 * @param <T>
 * @param <R>
 */
public class TraversingUtils<T,R> {
	public interface TraversingFunction<T,R,V>{

		void accept(T t,R r,V v);
		
	}
	protected List<T> list;
	protected R obj;
	protected TraversingUtils(List<T> listData,R obj) {
		this.list = listData;
		this.obj = obj;
	}
	
	public void forEach(TraversingFunction<? super T, ? super R,Integer> action) {
		AtomicInteger index = new AtomicInteger();
		for(T tmp : list) {
			index.incrementAndGet();
			action.accept(tmp, obj,index.get());
		}
	}
	public void forEach(BiConsumer<? super T, ? super R> action) {
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