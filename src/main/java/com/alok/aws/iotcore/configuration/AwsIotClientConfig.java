package com.alok.aws.iotcore.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.iot.IotClient;

@Configuration
@Slf4j
public class AwsIotClientConfig {

    //@Value("${aws.region:ap-south-1}")
    //private String awsRegion;

    @Value("${aws.iot-core-accessKey:test}")
    private String awsAccessKey;

    @Value("${aws.iot-core-secret:test}")
    private String awsSecret;

    @Bean
    public IotClient iotClientConfigBean() {
        //return IotClient.create();
        return IotClient.builder()
                //.region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider())
                .build();
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return AwsCredentialsProviderChain.builder()
                .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
                .addCredentialsProvider(DefaultCredentialsProvider.create())
                .addCredentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecret))
                )
                .build();
        //return SystemPropertyCredentialsProvider.create();
    }
}
