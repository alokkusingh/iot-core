package com.alok.aws.iotcore.service;

import com.alok.aws.iotcore.exception.ThingCreationException;
import com.alok.aws.iotcore.model.DeviceRegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;

@Service
@Slf4j
public class ThingService {

    @Autowired
    private IotClient iotClient;

    @Value("${aws.iot.thing.policy.allowed}")
    private String thingAllowedPolicy;

    public void createThingAndRegisterCertificate(DeviceRegistrationRequest deviceRegistrationRequest) {
        log.debug("Thing creation started, thing: {}", thingAllowedPolicy);
        createThing(deviceRegistrationRequest.getDeviceName());
        createCertificateAndAttachToThing(deviceRegistrationRequest);
        log.debug("Thing creation completed, thing: {}", thingAllowedPolicy);

    }

    private void createThing(String thingName) {

        log.debug("Creating thing: {}", thingAllowedPolicy);
        CreateThingResponse createThingResponse = null;

        try {
            createThingResponse = iotClient.createThing(
                    CreateThingRequest.builder()
                            .thingName(thingName)
                            .build()
            );
        } catch (ResourceAlreadyExistsException alreadyExists) {
            // treat this a success
            log.debug("Thing already exists, thing: {}", thingAllowedPolicy);
        } catch (RuntimeException rte) {
            log.error("Thing creation failed, thing: {}", thingName);
            throw new ThingCreationException("Thing Creation Failed!", rte);
        }
    }

    private void createCertificateAndAttachToThing(DeviceRegistrationRequest deviceRegistrationRequest) {
        log.debug("Creating certificate for thing: {}", deviceRegistrationRequest.getDeviceName());
        RegisterCertificateResponse registerCertificateResponse= null;
        try {
            registerCertificateResponse = iotClient.registerCertificate(RegisterCertificateRequest.builder()
                    .caCertificatePem(deviceRegistrationRequest.getCaCertificatePem())
                    .certificatePem(deviceRegistrationRequest.getCertificatePem())
                    .setAsActive(true)
                    .build());
        } catch (RuntimeException rte) {
            log.error("Certificate creation failed, thing: {}", deviceRegistrationRequest.getDeviceName());
            throw new ThingCreationException("Certificate Creation Failed!", rte);
        }

        log.debug("Attaching policy to certificate, thing: {}, certId: {}, policy: {}", deviceRegistrationRequest.getDeviceName(), registerCertificateResponse.certificateId(), thingAllowedPolicy);
        try {
            iotClient.attachPolicy(AttachPolicyRequest.builder()
                    .policyName(thingAllowedPolicy)
                    .target(registerCertificateResponse.certificateArn())
                    .build());
        } catch (RuntimeException rte) {
            log.error("Attaching policy to thing failed, deleting certificate, thing: {}, certId: {}", deviceRegistrationRequest.getDeviceName(), registerCertificateResponse.certificateId());
            deleteCertificate(registerCertificateResponse.certificateId());
            throw new ThingCreationException("Policy attachment Failed!", rte);
        }

        log.debug("Attaching certificate to thing: {}, certId: {}", deviceRegistrationRequest.getDeviceName(), registerCertificateResponse.certificateId());
        try {
            iotClient.attachThingPrincipal(AttachThingPrincipalRequest.builder()
                    .thingName(deviceRegistrationRequest.getDeviceName())
                    .principal(registerCertificateResponse.certificateArn())
                    .build());
        } catch (RuntimeException rte) {
            log.error("Attaching certificate to thing failed, deleting certificate, thing: {}, certId: {}", deviceRegistrationRequest.getDeviceName(), registerCertificateResponse.certificateId());
            deleteCertificate(registerCertificateResponse.certificateId());
            throw new ThingCreationException("Certificate attachment Failed!", rte);
        }
    }

    public void deleteCertificate(String certificateId) {
        try {
             iotClient.updateCertificate(
                UpdateCertificateRequest.builder()
                    .certificateId(certificateId)
                    .newStatus("INACTIVE")
                    .build());

              iotClient.deleteCertificate(
                DeleteCertificateRequest.builder()
                        .certificateId(certificateId)
                        .forceDelete(true)
                        .build()
             );
        } catch (RuntimeException rte) {
            log.error("Certificate deletion failed, certId: {}", certificateId);
            throw new ThingCreationException("Policy attachment Failed!", rte);
        }
    }
}
