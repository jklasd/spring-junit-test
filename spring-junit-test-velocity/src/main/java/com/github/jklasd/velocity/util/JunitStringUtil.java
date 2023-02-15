package com.github.jklasd.velocity.util;

public class JunitStringUtil {
	public static String firstToBig(String str) {
        char[] cs=str.toCharArray();
        if((int)cs[0]<=65) {
        	return str;
        }
        cs[0]-=32;
        return String.valueOf(cs);
    }
	public static String firstToSmall(String str) {
        char[] cs=str.toCharArray();
        if((int)cs[0]>=122) {
        	return str;
        }
        cs[0]+=32;
        return String.valueOf(cs);
    }
	
	/**
	 */
	
}
