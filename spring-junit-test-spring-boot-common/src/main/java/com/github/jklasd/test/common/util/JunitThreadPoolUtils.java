package com.github.jklasd.test.common.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class JunitThreadPoolUtils {
	private JunitThreadPoolUtils() {}
	private static volatile ThreadPoolExecutor IO_EXECUTOR;
	private final static double BLOCK_NUM = 0.9;//阻塞系数越大，线程数越大
	private final static int MAX_COMMON_THREAD_COUNT = (int) (Runtime.getRuntime().availableProcessors()/(1-BLOCK_NUM));
	private static volatile ThreadPoolExecutor CPU_EXECUTOR;
	private final static int CPU_CORE_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	private final static int CPU_MAX_QUEUE_COUNT = 2000;
	
	private final static int ALIVE_TIME = 3;
	public static final void commonRun(Runnable runnable){
		if (IO_EXECUTOR == null) {
			initCommonExecutor();
		}
		IO_EXECUTOR.execute(runnable);
	}
	private static synchronized void initCommonExecutor() {
		if (IO_EXECUTOR == null) {
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
			IO_EXECUTOR = new ThreadPoolExecutor(CPU_CORE_THREAD_COUNT*2,MAX_COMMON_THREAD_COUNT, ALIVE_TIME,TimeUnit.SECONDS,queue);
		}
	}
	public static final void commonRunMdc(Runnable runnable){
		commonRun(runnable);
	}
	public static ThreadPoolExecutor getThreadPool(int type) {
		switch (type) {
		case 1:
			if(CPU_EXECUTOR == null) {
				initPriority();
			}
			return CPU_EXECUTOR;
		case 2:
			if(IO_EXECUTOR == null) {
				initPriority();
			}
			return IO_EXECUTOR;
		default:
			break;
		}
		return null;
	}
	private static synchronized void initPriority() {
		if(CPU_EXECUTOR == null) {
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(CPU_MAX_QUEUE_COUNT);
			CPU_EXECUTOR = new ThreadPoolExecutor(CPU_CORE_THREAD_COUNT,CPU_CORE_THREAD_COUNT*2, ALIVE_TIME,TimeUnit.SECONDS,
					queue,new ThreadPoolExecutor.CallerRunsPolicy());
		}
	}
	public static final void priorityRunMdc(Runnable runnable){
		priorityRun(runnable);
	}
	public static final void priorityRun(Runnable runnable){
		if(CPU_EXECUTOR == null) {
			initPriority();
		}
		CPU_EXECUTOR.execute(runnable);
	}
}
