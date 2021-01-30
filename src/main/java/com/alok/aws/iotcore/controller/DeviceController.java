package com.alok.aws.iotcore.controller;

import com.alok.aws.iotcore.exception.CertificateDoesntExistException;
import com.alok.aws.iotcore.exception.ThingDoesntExistException;
import com.alok.aws.iotcore.model.DeviceRegistrationRequest;
import com.alok.aws.iotcore.service.ThingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device")
@Slf4j
public class DeviceController {

    @Autowired
    private ThingService thingService;

    @PostMapping(value = "/register")
    public ResponseEntity<String> registerDevice(@RequestBody DeviceRegistrationRequest deviceRegistrationRequest) {

        try {
            thingService.createThingAndRegisterCertificateCurrentRegion(deviceRegistrationRequest);
            thingService.createThingAndRegisterCertificateOtherRegion(deviceRegistrationRequest);
        } catch (RuntimeException rte) {
            log.error("Thing creation failed, error: {}, cause: {}", rte.getMessage(), rte.getCause());
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY)
                    .build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @PutMapping(value = "/{thingName}")
    public ResponseEntity<Void> updateDeviceCertState(@PathVariable("thingName") String thingName, @RequestParam("newStatus") String newStatus) {

        try {
            thingService.updateThingCertStatusCurrentRegion(thingName, newStatus);
            thingService.updateThingCertStatusOtherRegion(thingName, newStatus);
        } catch (ThingDoesntExistException rte) {
            log.error("Thing creation failed, error: {}, cause: {}", rte.getMessage(), rte.getCause());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .build();
        } catch (CertificateDoesntExistException rte) {
            log.error("Thing creation failed, error: {}, cause: {}", rte.getMessage(), rte.getCause());
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .build();
        } catch (RuntimeException rte) {
            log.error("Thing creation failed, error: {}, cause: {}", rte.getMessage(), rte.getCause());
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY)
                    .build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

    @GetMapping(value = "/test")
    public ResponseEntity<Void> test() {
       return ResponseEntity.ok().build();
    }
}
