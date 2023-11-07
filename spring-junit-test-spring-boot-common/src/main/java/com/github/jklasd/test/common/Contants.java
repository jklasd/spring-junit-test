package com.github.jklasd.test.common;

public class Contants {
	public static boolean prepareStatic = false;//启动时处理静态bean，false则运行时处理静态bean为true
	public static boolean runPrepareStatic = !prepareStatic;//运行时处理
}
