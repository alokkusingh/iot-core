package com.alok.aws.iotcore.service;

import com.alok.aws.iotcore.entity.Device;
import com.alok.aws.iotcore.exception.CertificateDoesntExistException;
import com.alok.aws.iotcore.exception.ThingCreationException;
import com.alok.aws.iotcore.exception.ThingDoesntExistException;
import com.alok.aws.iotcore.exception.ThingUpdateException;
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
    private IotClient currentRegionIotClient;

    @Autowired
    private IotClient otherRegionIotClient;

    @Autowired
    private DeviceRepository deviceRepository;

    @Value("${aws.iot.thing.policy.allowed}")
    private String thingAllowedPolicy;

    public void createThingAndRegisterCertificateCurrentRegion(DeviceRegistrationRequest deviceRegistrationRequest) {
        createThingAndRegisterCertificate(deviceRegistrationRequest, currentRegionIotClient);
    }

    public void createThingAndRegisterCertificateOtherRegion(DeviceRegistrationRequest deviceRegistrationRequest) {
        createThingAndRegisterCertificate(deviceRegistrationRequest, otherRegionIotClient);
    }

    private void createThingAndRegisterCertificate(DeviceRegistrationRequest deviceRegistrationRequest, IotClient iotClient) {

        // requires AWSIoTConfigAccess policy to ecsTaskExecutionRole

        if (deviceRegistrationRequest == null
                || deviceRegistrationRequest.getDeviceName() == null
                || deviceRegistrationRequest.getCertificatePem() == null
                || deviceRegistrationRequest.getCaCertificatePem() == null
        )
            throw new ThingCreationException("Request validation failed!");

        log.info("Thing creation started, thing: {}", deviceRegistrationRequest.getDeviceName());
        createThing(deviceRegistrationRequest.getDeviceName(), iotClient);
        RegisterCertificateResponse registerCertificateResponse = registerCertificate(deviceRegistrationRequest, iotClient);
        attachPolicyToCertificate(deviceRegistrationRequest.getDeviceName(), registerCertificateResponse, iotClient);
        attachThingCertificate(deviceRegistrationRequest.getDeviceName(), registerCertificateResponse, iotClient);
        log.info("Thing creation completed, thing: {}, certId: {}", deviceRegistrationRequest.getDeviceName(), registerCertificateResponse.certificateId(), iotClient);
    }

    private void createThing(String thingName, IotClient iotClient) {

        log.debug("Creating thing: {}", thingName);
        try {
            iotClient.createThing(
                    CreateThingRequest.builder()
                            .thingName(thingName)
                            .build()
            );
        } catch (ResourceAlreadyExistsException alreadyExists) {
            // treat this as success
            log.debug("Thing already exists, thing: {}", thingName);
        } catch (RuntimeException rte) {
            log.error("Thing creation failed, thing: {}, error: {}, cause: {}", thingName, rte.getMessage(), rte.getCause());
            throw new ThingCreationException("Thing Creation Failed!", rte);
        }
    }

    private RegisterCertificateResponse registerCertificate(DeviceRegistrationRequest deviceRegistrationRequest, IotClient iotClient) {
        log.debug("Creating certificate for thing: {}", deviceRegistrationRequest.getDeviceName());
        RegisterCertificateResponse registerCertificateResponse = null;
        try {
            registerCertificateResponse = iotClient.registerCertificate(RegisterCertificateRequest.builder()
                    .caCertificatePem(deviceRegistrationRequest.getCaCertificatePem())
                    .certificatePem(deviceRegistrationRequest.getCertificatePem())
                    .setAsActive(true)
                    .build());

            deviceRepository.save( Device.builder()
                    .deviceName(deviceRegistrationRequest.getDeviceName())
                    .awsDeviceCertId(registerCertificateResponse.certificateId())
                    .awsDeviceCertArn(registerCertificateResponse.certificateArn())
                    .build());
        } catch (ResourceAlreadyExistsException alreadyExists) {
            // treat this as success if DB has cert id
            log.debug("Certificate already exists, thing: {}", deviceRegistrationRequest.getDeviceName());
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

    private void attachPolicyToCertificate(String deviceName, RegisterCertificateResponse registerCertificateResponse, IotClient iotClient) {
        log.debug("Attaching policy to certificate, thing: {}, certId: {}, policy: {}", deviceName, registerCertificateResponse.certificateId(), thingAllowedPolicy);
        try {
            iotClient.attachPolicy(AttachPolicyRequest.builder()
                    .policyName(thingAllowedPolicy)
                    .target(registerCertificateResponse.certificateArn())
                    .build());
        } catch (RuntimeException rte) {
            log.error("Attaching policy to thing failed, deleting certificate, thing: {}, certId: {}", deviceName, registerCertificateResponse.certificateId());
            deleteCertificate(registerCertificateResponse.certificateId(), iotClient);
            throw new ThingCreationException("Policy attachment Failed!", rte);
        }
    }

    private void attachThingCertificate(String deviceName, RegisterCertificateResponse registerCertificateResponse, IotClient iotClient) {
        log.debug("Attaching certificate to thing: {}, certId: {}", deviceName, registerCertificateResponse.certificateId());
        try {
            iotClient.attachThingPrincipal(AttachThingPrincipalRequest.builder()
                    .thingName(deviceName)
                    .principal(registerCertificateResponse.certificateArn())
                    .build());
        } catch (RuntimeException rte) {
            log.error("Attaching certificate to thing failed, deleting certificate, thing: {}, certId: {}", deviceName, registerCertificateResponse.certificateId());
            deleteCertificate(registerCertificateResponse.certificateId(), iotClient);
            throw new ThingCreationException("Certificate attachment Failed!", rte);
        }
    }

    private void detachThingCertificate(String deviceName, String certificateArn, IotClient iotClient) {
        log.debug("Detaching certificate from thing: {}, certArn: {}", deviceName, certificateArn);
        try {
            iotClient.detachThingPrincipal(DetachThingPrincipalRequest.builder()
                    .thingName(deviceName)
                    .principal(certificateArn)
                    .build());
        } catch (RuntimeException rte) {
            log.error("Detaching certificate from thing failed, deleting certificate, thing: {}, certArn: {}", deviceName, certificateArn);
            throw new ThingCreationException("Detaching certificate from thing Failed!", rte);
        }
    }

    private void deleteCertificate(String certificateId, IotClient iotClient) {
        updateCertificateStatus(certificateId, "INACTIVE", iotClient);
        log.debug("Deleting certificate, certId: {}", certificateId);
        try {
              iotClient.deleteCertificate(
                DeleteCertificateRequest.builder()
                        .certificateId(certificateId)
                        .forceDelete(true)
                        .build());
        } catch (RuntimeException rte) {
            log.error("Certificate deletion failed, certId: {}", certificateId);
            throw new ThingCreationException("Certificate deletion failed!", rte);
        }
    }

    private void updateCertificateStatus(String certificateId, String status, IotClient iotClient) {
        log.debug("Updating certificate status, certId: {}, status: {}", certificateId, status);
        try {
            iotClient.updateCertificate( UpdateCertificateRequest.builder()
                    .certificateId(certificateId)
                    .newStatus(status)
                    .build());
        } catch (RuntimeException rte) {
            log.error("Updating Certificate status failed, certId: {}, status: {}", certificateId, status);
            throw new ThingCreationException("Updating certificate status failed!", rte);
        }
    }

    public void updateThingCertStatusCurrentRegion(String thingName, String newStatus) {
        updateThingCertStatus(thingName, newStatus, currentRegionIotClient);
    }

    public void updateThingCertStatusOtherRegion(String thingName, String newStatus) {
        updateThingCertStatus(thingName, newStatus, otherRegionIotClient);
    }

    private void updateThingCertStatus(String thingName, String newStatus, IotClient iotClient) {
        String certificateArn = getCertificateArn(thingName, iotClient);
        updateCertificateStatus(
                extractCertIdFromArn(certificateArn),
                newStatus,
                iotClient
        );

        if ("REVOKED".equals(newStatus))
            detachThingCertificate(thingName, certificateArn, iotClient);
    }

    private String getCertificateArn(String thingName, IotClient iotClient) {
        log.debug("Get certificate id for thing: {}", thingName);
        try {
            ListThingPrincipalsResponse thingPrincipals = iotClient.listThingPrincipals(ListThingPrincipalsRequest.builder()
                    .thingName(thingName)
                    .build());
            if (!thingPrincipals.hasPrincipals()) {
                log.error("Thing doesn't have principal, thing: {}", thingName);
                throw new CertificateDoesntExistException("hing doesn't have principal!");
            }

            // Assuming thing has only one principal any time - during revoke the principal was detached
            return thingPrincipals.principals().get(0);

        } catch (ResourceNotFoundException rnf) {
            log.error("Thing doesn't exist!, thing: {}", thingName);
            throw new ThingDoesntExistException("Thing doesn't exist!", rnf);
        } catch (RuntimeException rte) {
            log.error("Listing thing principals failed, thing: {}", thingName);
            throw new ThingUpdateException("List Thing principals failed!", rte);
        }
    }

    private String extractCertIdFromArn(String certArn) {
        return certArn.split("/")[1];
    }
}