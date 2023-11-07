package org.testcontainers.utility;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

class RyukContainer extends GenericContainer<RyukContainer> {

    RyukContainer() {
        super("testcontainers/ryuk:0.3.4");
        
        Integer maxContainerMemory = LazyApplicationContext.getInstance().getEnvironment().getProperty("MAX_CONTAINER_MEMORY", Integer.class,128);//测试使用
        withExposedPorts(8080);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withName("testcontainers-ryuk-" + DockerClientFactory.SESSION_ID);
            cmd.withHostConfig(
                cmd
                    .getHostConfig()
                    .withAutoRemove(true)
                    .withPrivileged(TestcontainersConfiguration.getInstance().isRyukPrivileged())
                    .withBinds(
                        new Bind(
                            DockerClientFactory.instance().getRemoteDockerUnixSocketPath(),
                            new Volume("/var/run/docker.sock")
                        )
                    )
                    .withMemory(maxContainerMemory*1024*1024L)//限制256M
            );
        });

        waitingFor(Wait.forLogMessage(".*Started.*", 1));
        Integer port = LazyApplicationContext.getInstance().getEnvironment().getProperty("RYUK_CONTAINER_PORT", Integer.class);
        if(port!=null) {
        	addFixedExposedPort(port, 8080);
        }
    }
}