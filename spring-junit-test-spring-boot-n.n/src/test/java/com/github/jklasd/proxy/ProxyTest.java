package com.github.jklasd.proxy;

public class ProxyTest {
	public static class Test1{
		private Test1() {}
		public void exec() {System.out.println("test1");}
	}
	public static class Test2{
		private String txt;
		private Test2(String txt) {this.txt = txt;}
		public void exec() {System.out.println(txt);}
	}
}
