package com.github.jklasd.test.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
final class ThreadPoolUtils {
	private ThreadPoolUtils() {}
	private static ThreadPoolExecutor executorServer;
	/** 排队任务的最大数目，
	 * 如果超过则创建新线程(不超过MAX_THREAD_COUNT否则任务丢弃)，
	 * 此参数为0则MAX_THREAD_COUNT无效线程池最多有CORE_THREAD_COUNT数目的线程*/
	private static final int MAX_QUEUE_COUNT=1000;
	/** 核心线程数目也即最小的数目，小于此数目的任务直接运行而无需排队*/
	private static final int COMMON_CORE_THREAD_COUNT=8;
	/** 最大线程数目*/
//	private static final int COMMON_MAX_THREAD_COUNT = 64;
	/** 核心线程数目也即最小的数目，小于此数目的任务直接运行而无需排队*/
	private static final int PRIORITY_CORE_THREAD_COUNT=4;
	/** 最大线程数目*/
	private static final int PRIORITY_MAX_THREAD_COUNT = 12;
	private static final long ALIVE_TIME = 600;
	
	private static ThreadPoolExecutor priorityExecutor;
	
	public static final void commonRun(Runnable runnable){
		if (executorServer == null) {
			initCommonPool();
		}
		executorServer.execute(runnable);
	}
	private static synchronized void initCommonPool() {
		if (executorServer == null) {
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
			executorServer = new ThreadPoolExecutor(COMMON_CORE_THREAD_COUNT,COMMON_CORE_THREAD_COUNT, ALIVE_TIME,TimeUnit.SECONDS,queue);
		}
	}
	private static synchronized void initPriority() {
		if(priorityExecutor == null) {
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_QUEUE_COUNT);
			priorityExecutor = new ThreadPoolExecutor(PRIORITY_CORE_THREAD_COUNT,PRIORITY_MAX_THREAD_COUNT, ALIVE_TIME,TimeUnit.SECONDS,
					queue,new ThreadPoolExecutor.CallerRunsPolicy());
		}
	}
	public static final void priorityRun(Runnable runnable){
		initPriority();
		priorityExecutor.execute(runnable);
	}
}
