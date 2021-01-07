package com.alok.aws.iotcore.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.iot.IotClient;

@Configuration
@Slf4j
public class AwsIotClientConfig {

    @Bean
    public IotClient iotClientConfigBean() {
        return IotClient.create();
    }
}
