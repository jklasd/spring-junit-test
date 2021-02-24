package org.springframework.boot.autoconfigure.domain;

public class EntityScanPackagesConstructor {
	private static EntityScanPackages bean;
	public static synchronized EntityScanPackages getBean() {
		if(bean == null) {
			bean = new EntityScanPackages();
		}
		return bean;
	}
}
