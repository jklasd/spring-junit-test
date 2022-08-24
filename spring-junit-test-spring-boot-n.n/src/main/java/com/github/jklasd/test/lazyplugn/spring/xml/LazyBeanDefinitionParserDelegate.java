 package com.github.jklasd.test.lazyplugn.spring.xml;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboBean;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate{
    
    private Map<String,XmlParser> filter = Maps.newConcurrentMap();
    private XmlParser defaultXmlParser = new XmlParser();
    {
        filter.put("context:annotation-config",defaultXmlParser);
        filter.put("aop:aspectj-autoproxy",defaultXmlParser);
        filter.put("context:component-scan",defaultXmlParser);
        filter.put("reg:zookeeper",defaultXmlParser);
        filter.put("job:simple",defaultXmlParser);
    }
    private Map<String,BeanDefParser> parser = Maps.newConcurrentMap();
    {
        LazyDubboBean.getInstance().load(parser);
    }
    private XmlReaderContext readerContext;
//    private LazyBeanDefinitionDocumentReader documentReader;
    public LazyBeanDefinitionParserDelegate(XmlReaderContext readerContext,LazyBeanDefinitionDocumentReader documentReader) {
        super(readerContext);
        this.readerContext = readerContext;
//        this.documentReader = documentReader;
    }

    public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {
        if(filter.containsKey(ele.getTagName())) {
            return filter.get(ele.getTagName()).parse(ele);
        }
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
            }else {
                Class<?> beanC = ScanUtil.loadClass(beanDef.getBeanClassName());
                String beanName = ele.hasAttribute("id") ?ele.getAttribute("id"):BeanNameUtil.getBeanName(beanC);
                log.info("beanName=>{};beanDef=>{},=>{},pv=>{};",beanName,ele.getTagName(),beanDef.getBeanClassName(),beanDef.getPropertyValues());
                
                if(beanName.equals("serviceBean")) {
                	log.info("");
                }
                
                BeanModel beanModel = new BeanModel();
                beanModel.setXmlBean(true);
                beanModel.setTagClass(beanC);
//                beanModel.setBeanClassName(beanDef.getBeanClassName());
                beanModel.setBeanName(beanName);
                beanModel.setPropValue(beanDef.getPropertyValues());
                beanModel.setConstructorArgs(beanDef.getConstructorArgumentValues());
                LazyBean.getInstance().buildProxy(beanModel);
//                TestUtil.getApplicationContext().registBean(beanName, obj, beanC);
                
//                documentReader.attrs.put(key, beanDef.getPropertyValues());
            }
        }
        return beanDef;
    }
}
