 package com.github.jklasd.test.lazyplugn.spring.xml;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

import com.github.jklasd.test.lazybean.beanfactory.generics.LazyDubboBean;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate{
    
    private Map<String,BeanDefParser> parser = Maps.newConcurrentMap();
    {
        LazyDubboBean.getInstance().load(parser);
    }
    private XmlReaderContext readerContext;
    public LazyBeanDefinitionParserDelegate(XmlReaderContext readerContext,LazyBeanDefinitionDocumentReader documentReader) {
        super(readerContext);
        this.readerContext = readerContext;
    }
    
    public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {

    	String namespaceUri = getNamespaceURI(ele);
        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null) {
            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
            return null;
        }
        BeanDefinition beanDef =  handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
        if(beanDef != null) {
            if(parser.containsKey(namespaceUri)) {
                parser.get(namespaceUri).handBeanDef(ele,beanDef);
            }
        }
        return beanDef;
    }
}
