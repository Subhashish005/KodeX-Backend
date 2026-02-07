package com.something.kodex_backend.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {

  @Bean
  public DockerClient dockerClient() {
    DefaultDockerClientConfig config =
      DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost("unix:///var/run/docker.sock")
        .build();

    DockerHttpClient httpClient =
      new ZerodepDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .sslConfig(config.getSSLConfig())
        .build();

    return DockerClientBuilder.getInstance(config)
      .withDockerHttpClient(httpClient)
      .build();
  }

}