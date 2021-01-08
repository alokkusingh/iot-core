package com.alok.aws.iotcore.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Device {
    @Id
    private String deviceName;
    private String awsDeviceCertId;
    private String awsDeviceCertArn;
}
