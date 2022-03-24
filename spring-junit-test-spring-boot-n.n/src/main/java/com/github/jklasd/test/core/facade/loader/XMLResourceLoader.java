package com.github.jklasd.test.core.facade.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.JunitResourceLoader;
import com.github.jklasd.test.lazyplugn.spring.xml.LazyBeanDefinitionDocumentReader;
import com.github.jklasd.test.lazyplugn.spring.xml.LazyXmlListableBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XMLResourceLoader implements JunitResourceLoader{
	private static XMLResourceLoader loader = new XMLResourceLoader();
	private XMLResourceLoader() {}
	public static XMLResourceLoader getInstance() {
		return loader;
	}
	private DocumentLoader documentLoader = new DefaultDocumentLoader();
    private XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(LazyXmlListableBeanFactory.getInstance());
    
	public static Set<String> xmlpathlist = Sets.newConcurrentHashSet();
	protected final Log logger = LogFactory.getLog(getClass());
    private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	@Override
	public void loadResource(InputStream jarFileIs) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(jarFileIs));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] url_handler = line.split("=");
				Class<?> nameSpaceHandlerC = ScanUtil.loadClass(url_handler[1]);
				if (nameSpaceHandlerC != null) {
					XmlBeanUtil.getInstance().putNameSpace(
							url_handler[0].replace("\\", ""), nameSpaceHandlerC);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadXmlPath(String... xmlPath) {
		for (String path : xmlPath) {
			xmlpathlist.add(path);
		}
	}

	@Override
	public void initResource() {
		xmlpathlist.forEach(xml -> readNode(xml));
	}
	public void readNode(String xml) {
        Resource file = TestUtil.getInstance().getApplicationContext().getResource(xml);
        if (file != null) {
            try {
                log.info("load:{}",file.getURL().getPath());
                XmlReaderContext context = xmlReader.createReaderContext(file);
                LazyBeanDefinitionDocumentReader parsor = new LazyBeanDefinitionDocumentReader();
                Document document = documentLoader.loadDocument(new InputSource(file.getInputStream()), getEntityResolver(xmlReader), 
                    errorHandler,(int)JunitInvokeUtil.invokeMethodByParamClass(xmlReader, "getValidationModeForResource",new Class[] {Resource.class}, new Object[] {file}),xmlReader.isNamespaceAware());
                
                parsor.registerBeanDefinitions(document,context);
            } catch (Exception e) {
                log.error("加载xml", e);
                throw new RuntimeException("加载xml【"+file+"】失败");
            }
        }
    }
	private EntityResolver getEntityResolver(XmlBeanDefinitionReader reader) {
        EntityResolver entityResolver = null;
        ResourceLoader resourceLoader = reader.getResourceLoader();
        if (resourceLoader != null) {
            entityResolver = new ResourceEntityResolver(resourceLoader);
        } else {
            entityResolver = new DelegatingEntityResolver(reader.getBeanClassLoader());
        }
        return entityResolver;
    }

	@Override
	public void loadResource(String... sourcePath) {
		// TODO Auto-generated method stub
		
	}
}
