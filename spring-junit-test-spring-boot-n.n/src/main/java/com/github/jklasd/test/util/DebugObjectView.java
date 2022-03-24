package com.github.jklasd.test.util;

public class DebugObjectView {
	private static ThreadLocal<Boolean> isDebug = new InheritableThreadLocal<Boolean>();
	public static void openView() {
		isDebug.set(true);
	}
	public static void clear() {
		isDebug.remove();
	}
	
	public static void readView(Runnable viewRun) {
		if(isDebug.get()!=null && isDebug.get()) {
			viewRun.run();
		}
	}
}
