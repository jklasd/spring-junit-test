 package com.github.jklasd.test.lazyplugn.spring.xml;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

import com.github.jklasd.test.common.component.BeanDefinitionParserDelegateComponent;
import com.github.jklasd.test.common.interf.handler.BeanDefParser;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate{
    
    private Map<String,BeanDefParser> parser = Maps.newConcurrentMap();
    {
        BeanDefinitionParserDelegateComponent.loadBeanDefParser(parser);
    }
    private XmlReaderContext readerContext;
    public LazyBeanDefinitionParserDelegate(XmlReaderContext readerContext,LazyBeanDefinitionDocumentReader documentReader) {
        super(readerContext);
        /**
         * 需要修正env
         */
//        readerContext.getReader().setEnvironment(LazyApplicationContext.getInstance().getEnvironment()); //创建时注入
        this.readerContext = readerContext;
    }
    
//    private boolean inited_scan;
    public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {
    	String namespaceUri = getNamespaceURI(ele);
        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null) {
            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
            return null;
        }
//        if(ele.getTagName().equals("context:component-scan") && !inited_scan) {
//        	JunitInvokeUtil.invokeMethod(handler, NamespaceHandlerSupport.class, "registerBeanDefinitionParser", new Object[] {"component-scan",new LazyComponentScanBeanDefinitionParser()});
//        	inited_scan = true;
//        }
        BeanDefinition beanDef =  handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
        if(beanDef != null) {
            if(parser.containsKey(namespaceUri)) {
                parser.get(namespaceUri).handBeanDef(ele,beanDef);
            }
        }
        return beanDef;
    }
}
