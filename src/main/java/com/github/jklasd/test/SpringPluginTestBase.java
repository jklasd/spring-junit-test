package com.github.jklasd.test;

public abstract class SpringPluginTestBase {
	public SpringPluginTestBase() {
		TestUtil.startTestForNoContainer(this);
	}
}
