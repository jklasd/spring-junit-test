package com.github.jklasd.test.common.component;

import com.github.jklasd.test.common.VersionController;

public class VersionControlComponent extends AbstractComponent{

	@Override
	<T> void add(T component) {
		VersionController handler = (VersionController) component;
		handler.register();
	}

}
