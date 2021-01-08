package com.alok.aws.iotcore.repository;

import com.alok.aws.iotcore.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String> {

    Device findOneByDeviceName(String deviceName);
}
