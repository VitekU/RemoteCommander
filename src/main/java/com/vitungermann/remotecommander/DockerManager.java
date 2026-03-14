package com.vitungermann.remotecommander;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.stereotype.Service;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;

@Service
class DockerManager {
    public final DockerClient dockerClient;

    public DockerManager(JobManager jobManager) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("tcp://192.168.0.40").build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).build();

        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }


}
