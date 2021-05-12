package com.github.jklasd.test.spring.xml;

import java.util.List;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ManagedArray;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.beanfactory.LazyBean;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
        
        /**
         * 处理bean attr
         */
        attrs.keySet().forEach(key->{
            Map<String, Object> attrParam = Maps.newHashMap();
            Class<?> tagC = ScanUtil.loadClass(key.split("-")[0]);
            attrs.get(key).getPropertyValueList().forEach(prov->{
                Object value = null;
                if(prov.getValue() instanceof RuntimeBeanReference) {
//                    log.info("RuntimeBeanReference=>{}",prov.getValue());
                    RuntimeBeanReference tmp = (RuntimeBeanReference)prov.getValue();
                    value = TestUtil.getApplicationContext().getBean(tmp.getBeanName());
                }else if(prov.getValue() instanceof ManagedArray) {
                    ManagedArray tmp = (ManagedArray)prov.getValue();
                    List<Object> list = Lists.newArrayList();
                    tmp.stream().forEach(item ->{
                        if(item instanceof BeanDefinitionHolder) {
                            BeanDefinitionHolder tmpBdh = (BeanDefinitionHolder)item;
                            BeanDefinition tmpBd = tmpBdh.getBeanDefinition();
                            Class<?> tmpC= ScanUtil.loadClass(tmpBd.getBeanClassName());
                            XmlBeanUtil.getInstance().addClass(tmpC);
                            list.add(LazyBean.buildProxy(tmpC));
                        }else {
                            log.info("ManagedArray=>{}",item);
                        }
                    });
                    value = list;
                }else if(prov.getValue() instanceof TypedStringValue) {
                    TypedStringValue tmp = (TypedStringValue)prov.getValue();
                    value = tmp.getValue();
                }else {
                    log.info("value other=>{}",prov.getValue());
                }
                attrParam.put(prov.getName(), value);
            });
            XmlBeanUtil.getInstance().processValue(attrParam, tagC);
            XmlBeanUtil.getInstance().getProcess(key).getProcess().init(attrParam);
        });
    }

    protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root,
        BeanDefinitionParserDelegate parentDelegate) {
        BeanDefinitionParserDelegate delegate = new LazyBeanDefinitionParserDelegate(readerContext,this);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    Map<String,MutablePropertyValues> attrs = Maps.newConcurrentMap();
    
    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        BeanDefinitionHolder holder = delegate.parseBeanDefinitionElement(ele);
//        log.info("id=>{}",holder.getBeanName());
        AbstractBeanDefinition beanDef = (AbstractBeanDefinition)holder.getBeanDefinition();
        String beanName = holder.getBeanName();
        Class<?> beanC = ScanUtil.loadClass(beanDef.getBeanClassName());
        XmlBeanUtil.getInstance().addClass(beanC);
        
        String key = beanDef.getBeanClassName() +"-" + beanName;
        XmlBeanUtil.getInstance().loadAttrMapProcess(key);
        Object obj = LazyBean.buildProxy(beanC, beanName, XmlBeanUtil.getInstance().getProcess(key));
        TestUtil.getApplicationContext().registBean(beanName, obj, beanC);
        attrs.put(key, beanDef.getPropertyValues());
    }

    protected void processAliasRegistration(Element ele) {

    }

    protected void importBeanDefinitionResource(Element ele) {
        XmlBeanUtil.getInstance().readNode(ele.getAttribute(RESOURCE_ATTRIBUTE));
    }

    public void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
        super.parseBeanDefinitions(root, delegate);
    }
}
