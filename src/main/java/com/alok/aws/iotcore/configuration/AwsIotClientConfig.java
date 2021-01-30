package com.alok.aws.iotcore.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotClient;

@Configuration
@Slf4j
public class AwsIotClientConfig {

    @Bean
    public IotClient currentRegionIotClient() {
        System.out.println("Current Region: " + System.getenv("AWS_REGION"));
        return IotClient.builder()
                .build();
    }

    @Bean
    public IotClient otherRegionIotClient() {
        System.out.println("Current Region: " + System.getenv("AWS_REGION"));
        return IotClient.builder()
                .region(Region.of(getOtherRegion()))
                .build();
    }

    private String getOtherRegion() {
       if ("ap-south-1".equals(System.getenv("AWS_REGION")))
           return "ap-southeast-1";

       return "ap-south-1";
    }

    /*@Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return AwsCredentialsProviderChain.builder()
                .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
                .addCredentialsProvider(DefaultCredentialsProvider.create())
                .addCredentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecret))
                )
                .build();
        //return SystemPropertyCredentialsProvider.create();
    }*/
}
