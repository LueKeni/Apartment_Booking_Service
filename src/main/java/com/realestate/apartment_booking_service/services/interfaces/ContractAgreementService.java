package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.entities.ContractAgreement;
import java.util.Collection;
import java.util.Set;

public interface ContractAgreementService {

    ContractAgreement getOrCreateForUser(Long userId, Long appointmentId);

    ContractAgreement signForUser(Long userId, Long appointmentId, String signerName, String signatureDataUrl);

    ContractAgreement getOrCreateForAgent(Long agentId, Long appointmentId);

    ContractAgreement signForAgent(Long agentId, Long appointmentId, String signerName, String signatureDataUrl);

    Set<Long> getSignedAppointmentIds(Long userId, Collection<Long> appointmentIds);

    Set<Long> getUserSignedAppointmentIdsForAgent(Long agentId, Collection<Long> appointmentIds);

    Set<Long> getAgentSignedAppointmentIds(Long agentId, Collection<Long> appointmentIds);
}
