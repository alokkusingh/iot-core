package com.alok.aws.iotcore.controller;

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
            thingService.createThingAndRegisterCertificate(deviceRegistrationRequest);
        } catch (RuntimeException rte) {

            log.error("Thing creation failed, error: {}, " + rte.getMessage(), rte.getStackTrace());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping(value = "/test")
    public ResponseEntity<Void> test() {
       return ResponseEntity.ok().build();
    }
}
