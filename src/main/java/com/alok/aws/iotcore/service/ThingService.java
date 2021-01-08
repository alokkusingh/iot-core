package com.alok.aws.iotcore.service;

import com.alok.aws.iotcore.entity.Device;
import com.alok.aws.iotcore.exception.ThingCreationException;
import com.alok.aws.iotcore.model.DeviceRegistrationRequest;
import com.alok.aws.iotcore.repository.DeviceRepository;
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

    @Autowired
    private DeviceRepository deviceRepository;

    @Value("${aws.iot.thing.policy.allowed}")
    private String thingAllowedPolicy;

    public void createThingAndRegisterCertificate(DeviceRegistrationRequest deviceRegistrationRequest) {
        log.debug("Thing creation started, thing: {}", deviceRegistrationRequest.getDeviceName());
        createThing(deviceRegistrationRequest.getDeviceName());
        RegisterCertificateResponse registerCertificateResponse = registerCertificate(deviceRegistrationRequest);
        attachPolicyToCertificate(deviceRegistrationRequest.getDeviceName(), registerCertificateResponse);
        attachThingCertificate(deviceRegistrationRequest.getDeviceName(), registerCertificateResponse);
        log.debug("Thing creation completed, thing: {}", deviceRegistrationRequest.getDeviceName());
    }

    private void createThing(String thingName) {

        log.debug("Creating thing: {}", thingAllowedPolicy);
        try {
            iotClient.createThing(
                    CreateThingRequest.builder()
                            .thingName(thingName)
                            .build()
            );
        } catch (ResourceAlreadyExistsException alreadyExists) {
            // treat this as success
            log.debug("Thing already exists, thing: {}", thingAllowedPolicy);
        } catch (RuntimeException rte) {
            log.error("Thing creation failed, thing: {}", thingName);
            throw new ThingCreationException("Thing Creation Failed!", rte);
        }
    }

    private RegisterCertificateResponse registerCertificate(DeviceRegistrationRequest deviceRegistrationRequest) {
        log.debug("Creating certificate for thing: {}", deviceRegistrationRequest.getDeviceName());
        RegisterCertificateResponse registerCertificateResponse = null;
        try {
            registerCertificateResponse = iotClient.registerCertificate(RegisterCertificateRequest.builder()
                    .caCertificatePem(deviceRegistrationRequest.getCaCertificatePem())
                    .certificatePem(deviceRegistrationRequest.getCertificatePem())
                    .setAsActive(true)
                    .build());

            deviceRepository.save(
                    Device.builder()
                            .deviceName(deviceRegistrationRequest.getDeviceName())
                            .awsDeviceCertId(registerCertificateResponse.certificateId())
                            .awsDeviceCertArn(registerCertificateResponse.certificateArn())
                            .build()
            );

        } catch (ResourceAlreadyExistsException alreadyExists) {
            // treat this as success if DB has cert id
            log.debug("Certificate already exists, thing: {}", thingAllowedPolicy);
            Device device = deviceRepository.findOneByDeviceName(deviceRegistrationRequest.getDeviceName());
            if (device != null)
                registerCertificateResponse = RegisterCertificateResponse.builder()
                        .certificateId(device.getAwsDeviceCertId())
                        .certificateArn(device.getAwsDeviceCertArn())
                        .build();
            else
                throw new ThingCreationException("Certificate Creation Failed!", alreadyExists);
        } catch (RuntimeException rte) {
            log.error("Certificate creation failed, thing: {}", deviceRegistrationRequest.getDeviceName());
            throw new ThingCreationException("Certificate Creation Failed!", rte);
        }

        return registerCertificateResponse;
    }

    private void attachPolicyToCertificate(String deviceName, RegisterCertificateResponse registerCertificateResponse) {
        log.debug("Attaching policy to certificate, thing: {}, certId: {}, policy: {}", deviceName, registerCertificateResponse.certificateId(), thingAllowedPolicy);
        try {
            iotClient.attachPolicy(AttachPolicyRequest.builder()
                    .policyName(thingAllowedPolicy)
                    .target(registerCertificateResponse.certificateArn())
                    .build());
        } catch (RuntimeException rte) {
            log.error("Attaching policy to thing failed, deleting certificate, thing: {}, certId: {}", deviceName, registerCertificateResponse.certificateId());
            deleteCertificate(registerCertificateResponse.certificateId());
            throw new ThingCreationException("Policy attachment Failed!", rte);
        }
    }

    private void attachThingCertificate(String deviceName, RegisterCertificateResponse registerCertificateResponse) {
        log.debug("Attaching certificate to thing: {}, certId: {}", deviceName, registerCertificateResponse.certificateId());
        try {
            iotClient.attachThingPrincipal(AttachThingPrincipalRequest.builder()
                    .thingName(deviceName)
                    .principal(registerCertificateResponse.certificateArn())
                    .build());
        } catch (RuntimeException rte) {
            log.error("Attaching certificate to thing failed, deleting certificate, thing: {}, certId: {}", deviceName, registerCertificateResponse.certificateId());
            deleteCertificate(registerCertificateResponse.certificateId());
            throw new ThingCreationException("Certificate attachment Failed!", rte);
        }
    }

    private void deleteCertificate(String certificateId) {
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
