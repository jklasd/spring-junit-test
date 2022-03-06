package com.github.jklasd.test.lazyplugn.spring.xml;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

import com.github.jklasd.test.core.facade.loader.XMLResourceLoader;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {
    private BeanDefinitionParserDelegate customdelegate;
    

    protected void doRegisterBeanDefinitions(Element root) {
        BeanDefinitionParserDelegate parent = this.customdelegate;
        /**
         * 自定义解析器
         */
        this.customdelegate = createDelegate(getReaderContext(), root, parent);
        parseBeanDefinitions(root, customdelegate);
        this.customdelegate = parent;
        
//        XmlBeanUtil.getInstance().handAttr(attrs);
    }

    protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root,
        BeanDefinitionParserDelegate parentDelegate) {
        BeanDefinitionParserDelegate delegate = new LazyBeanDefinitionParserDelegate(readerContext,this);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        BeanDefinitionHolder holder = delegate.parseBeanDefinitionElement(ele);
        AbstractBeanDefinition beanDef = (AbstractBeanDefinition)holder.getBeanDefinition();
        String beanName = holder.getBeanName();
        Class<?> beanC = ScanUtil.loadClass(beanDef.getBeanClassName());
        
        log.info("xml build proxy bean=>{}",beanDef.getBeanClassName());
        BeanModel model = new BeanModel();
        model.setThrows(true);
//        model.setBeanClassName(beanDef.getBeanClassName());
        model.setXmlBean(true);
        model.setBeanName(beanName);
        model.setTagClass(beanC);
        model.setPropValue(beanDef.getPropertyValues());
        LazyBean.getInstance().buildProxy(model);
        
    }

    protected void processAliasRegistration(Element ele) {

    }

    protected void importBeanDefinitionResource(Element ele) {
    	XMLResourceLoader.getInstance().readNode(ele.getAttribute(RESOURCE_ATTRIBUTE));
    }

    public void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
        super.parseBeanDefinitions(root, delegate);
    }
}
