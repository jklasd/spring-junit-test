package com.github.jklasd.test.core.common.methodann.mock.docker;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;

import org.springframework.core.env.Environment;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;
import org.testcontainers.dockerclient.TransportConfig.TransportConfigBuilder;

import com.github.dockerjava.transport.SSLConfig;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

import lombok.AllArgsConstructor;

public class RemoteDockerClientProviderStrategy extends DockerClientProviderStrategy{

    private static final String SOCKET_LOCATION = "tcp://127.0.0.1:2375";

    public static final int PRIORITY = 80;
    
    private Environment env = LazyApplicationContext.getInstance().getEnvironment();
    
    @AllArgsConstructor
    public class RemoteDockerSSLConfig implements SSLConfig{

    	Environment env;
    	
		@Override
		public SSLContext getSSLContext()
				throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
			
			
			
			return null;
		}
    	
    }
    
    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
    	String remoteUrl = env.getProperty("DOCKER_HOST",SOCKET_LOCATION);//;
    	TransportConfigBuilder builder = TransportConfig.builder();
    	builder.dockerHost(URI.create(remoteUrl));
//    	builder.sslConfig(new RemoteDockerSSLConfig(env));
        return builder.build();
    }

    @Override
    protected boolean isApplicable() {
        return env.containsProperty("DOCKER_HOST");
    }

    @Override
    public String getDescription() {
        return "RemoteDocker socket (" + getDockerHostIpAddress() + ")";
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

}
