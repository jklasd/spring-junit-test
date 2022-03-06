package com.github.jklasd.test.lazyplugn.spring.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.util.ScanUtil;

public class XmlParser {

    public BeanDefinition parse(Element ele) {
        if (ele.getTagName().equals("context:component-scan")) {
            TestUtil.getInstance().loadScanPath(ele.getAttribute("base-package"));
            ScanUtil.loadContextPathClass();
        }
        return null;
    }

}
