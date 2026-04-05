package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.entities.ContractAgreement;
import com.realestate.apartment_booking_service.enums.ContractAgreementStatus;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import com.realestate.apartment_booking_service.repositories.AppointmentRepository;
import com.realestate.apartment_booking_service.repositories.ContractAgreementRepository;
import com.realestate.apartment_booking_service.services.interfaces.ContractAgreementService;
import com.realestate.apartment_booking_service.services.interfaces.ContractPdfService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractAgreementServiceImpl implements ContractAgreementService {

    private static final DateTimeFormatter CONTRACT_CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter VIEWING_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final ContractAgreementRepository contractAgreementRepository;
    private final ContractPdfService contractPdfService;

    @Override
    public ContractAgreement getOrCreateForUser(Long userId, Long appointmentId) {
        Appointment appointment = resolveOwnedCompletedAppointment(userId, appointmentId);
        ContractAgreement contractAgreement = contractAgreementRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> {
                    contractAgreementRepository.save(buildDraftContract(appointment));
                    return contractAgreementRepository.findByAppointmentId(appointmentId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Cannot initialize contract"));
                });
        return ensureFinalPdfIfNeeded(contractAgreementRepository.save(syncLegacyContract(contractAgreement, appointment)));
    }

    @Override
    public ContractAgreement signForUser(Long userId, Long appointmentId, String signerName, String signatureDataUrl) {
        String normalizedSigner = signerName == null ? "" : signerName.trim();
        String normalizedSignature = signatureDataUrl == null ? "" : signatureDataUrl.trim();

        if (normalizedSigner.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter signer name");
        }
        if (normalizedSignature.isBlank() || !normalizedSignature.startsWith("data:image/png;base64,")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please provide a valid signature");
        }

        ContractAgreement contractAgreement = getOrCreateForUser(userId, appointmentId);
        contractAgreement.setUserSignerName(normalizedSigner);
        contractAgreement.setUserSignatureDataUrl(normalizedSignature);
        contractAgreement.setUserSignedAt(LocalDateTime.now());
        contractAgreement.setStatus(ContractAgreementStatus.USER_SIGNED);
        return contractAgreementRepository.save(contractAgreement);
    }

    @Override
    public Set<Long> getSignedAppointmentIds(Long userId, Collection<Long> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            return Set.of();
        }
        return contractAgreementRepository
                .findByAppointmentUserIdAndAppointmentIdInAndUserSignedAtIsNotNull(userId, appointmentIds)
                .stream()
                .map(contractAgreement -> contractAgreement.getAppointment().getId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    @Override
    public ContractAgreement getOrCreateForAgent(Long agentId, Long appointmentId) {
        Appointment appointment = resolveOwnedCompletedAppointmentForAgent(agentId, appointmentId);
        ContractAgreement contractAgreement = contractAgreementRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> {
                    contractAgreementRepository.save(buildDraftContract(appointment));
                    return contractAgreementRepository.findByAppointmentId(appointmentId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Cannot initialize contract"));
                });
        return ensureFinalPdfIfNeeded(contractAgreementRepository.save(syncLegacyContract(contractAgreement, appointment)));
    }

    @Override
    public ContractAgreement signForAgent(Long agentId, Long appointmentId, String signerName, String signatureDataUrl) {
        String normalizedSigner = signerName == null ? "" : signerName.trim();
        String normalizedSignature = signatureDataUrl == null ? "" : signatureDataUrl.trim();

        if (normalizedSigner.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter signer name");
        }
        if (normalizedSignature.isBlank() || !normalizedSignature.startsWith("data:image/png;base64,")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please provide a valid signature");
        }

        ContractAgreement contractAgreement = getOrCreateForAgent(agentId, appointmentId);
        if (contractAgreement.getUserSignedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must sign first");
        }

        contractAgreement.setAgentSignerName(normalizedSigner);
        contractAgreement.setAgentSignatureDataUrl(normalizedSignature);
        contractAgreement.setAgentSignedAt(LocalDateTime.now());
        contractAgreement.setStatus(ContractAgreementStatus.FULLY_SIGNED);

        ContractPdfService.GeneratedContractPdf generatedPdf = contractPdfService.generateFinalPdf(contractAgreement);
        contractAgreement.setPdfFilePath(generatedPdf.filePath());
        contractAgreement.setPdfPublicUrl(generatedPdf.publicUrl());
        contractAgreement.setPdfChecksum(generatedPdf.sha256Checksum());
        contractAgreement.setPdfGeneratedAt(LocalDateTime.now());
        return contractAgreementRepository.save(contractAgreement);
    }

    @Override
    public Set<Long> getUserSignedAppointmentIdsForAgent(Long agentId, Collection<Long> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            return Set.of();
        }
        return contractAgreementRepository
                .findByAppointmentAgentIdAndAppointmentIdInAndUserSignedAtIsNotNull(agentId, appointmentIds)
                .stream()
                .map(contractAgreement -> contractAgreement.getAppointment().getId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    @Override
    public Set<Long> getAgentSignedAppointmentIds(Long agentId, Collection<Long> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            return Set.of();
        }
        return contractAgreementRepository
                .findByAppointmentAgentIdAndAppointmentIdInAndAgentSignedAtIsNotNull(agentId, appointmentIds)
                .stream()
                .map(contractAgreement -> contractAgreement.getAppointment().getId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private Appointment resolveOwnedCompletedAppointment(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this appointment");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contract can only be signed after appointment is completed");
        }
        return appointment;
    }

    private Appointment resolveOwnedCompletedAppointmentForAgent(Long agentId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getAgent().getId().equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this appointment");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contract can only be signed after appointment is completed");
        }
        return appointment;
    }

    private ContractAgreement buildDraftContract(Appointment appointment) {
        Apartment apartment = appointment.getApartment();
        return ContractAgreement.builder()
                .appointment(appointment)
                .contractCode(buildStandardContractCode(appointment))
                .apartmentTitleSnapshot(apartment.getTitle())
                .apartmentAddressSnapshot(apartment.getFullAddressLabel() != null
                        ? apartment.getFullAddressLabel()
                        : apartment.getLocationAddress())
                .transactionTypeSnapshot(apartment.getTransactionType() == null
                        ? null
                        : apartment.getTransactionType().name())
                .priceSnapshot(apartment.getPrice() == null ? null : apartment.getPrice().toPlainString())
                .userPartyNameSnapshot(appointment.getUser().getFullName())
                .agentPartyNameSnapshot(appointment.getAgent().getFullName())
                .viewingDateSnapshot(VIEWING_DATE_FORMAT.format(
                        LocalDateTime.of(appointment.getScheduledDate(), appointment.getScheduledTime())))
                .status(ContractAgreementStatus.DRAFT)
                .build();
    }

    private ContractAgreement syncLegacyContract(ContractAgreement contractAgreement, Appointment appointment) {
        Apartment apartment = appointment.getApartment();
        if (contractAgreement.getContractCode() == null || contractAgreement.getContractCode().isBlank()
                || contractAgreement.getContractCode().startsWith("HD-")) {
            contractAgreement.setContractCode(buildStandardContractCode(appointment));
        }
        if (contractAgreement.getApartmentTitleSnapshot() == null || contractAgreement.getApartmentTitleSnapshot().isBlank()) {
            contractAgreement.setApartmentTitleSnapshot(apartment.getTitle());
        }
        if (contractAgreement.getApartmentAddressSnapshot() == null || contractAgreement.getApartmentAddressSnapshot().isBlank()) {
            contractAgreement.setApartmentAddressSnapshot(
                    apartment.getFullAddressLabel() != null ? apartment.getFullAddressLabel() : apartment.getLocationAddress());
        }
        if (contractAgreement.getTransactionTypeSnapshot() == null || contractAgreement.getTransactionTypeSnapshot().isBlank()) {
            contractAgreement.setTransactionTypeSnapshot(
                    apartment.getTransactionType() == null ? null : apartment.getTransactionType().name());
        }
        if (contractAgreement.getPriceSnapshot() == null || contractAgreement.getPriceSnapshot().isBlank()) {
            contractAgreement.setPriceSnapshot(apartment.getPrice() == null ? null : apartment.getPrice().toPlainString());
        }
        if (contractAgreement.getUserPartyNameSnapshot() == null || contractAgreement.getUserPartyNameSnapshot().isBlank()) {
            contractAgreement.setUserPartyNameSnapshot(appointment.getUser().getFullName());
        }
        if (contractAgreement.getAgentPartyNameSnapshot() == null || contractAgreement.getAgentPartyNameSnapshot().isBlank()) {
            contractAgreement.setAgentPartyNameSnapshot(appointment.getAgent().getFullName());
        }
        if (contractAgreement.getViewingDateSnapshot() == null || contractAgreement.getViewingDateSnapshot().isBlank()) {
            contractAgreement.setViewingDateSnapshot(VIEWING_DATE_FORMAT.format(
                    LocalDateTime.of(appointment.getScheduledDate(), appointment.getScheduledTime())));
        }
        if (contractAgreement.getStatus() == null) {
            if (contractAgreement.getAgentSignedAt() != null) {
                contractAgreement.setStatus(ContractAgreementStatus.FULLY_SIGNED);
            } else if (contractAgreement.getUserSignedAt() != null) {
                contractAgreement.setStatus(ContractAgreementStatus.USER_SIGNED);
            } else {
                contractAgreement.setStatus(ContractAgreementStatus.DRAFT);
            }
        }
        return contractAgreement;
    }

    private String buildStandardContractCode(Appointment appointment) {
        String datePart = CONTRACT_CODE_DATE_FORMAT.format(
                appointment.getCreatedAt() == null ? LocalDateTime.now() : appointment.getCreatedAt());
        return "ABS-" + datePart + "-" + String.format("%06d", appointment.getId());
    }

    private ContractAgreement ensureFinalPdfIfNeeded(ContractAgreement contractAgreement) {
        if (contractAgreement.getStatus() != ContractAgreementStatus.FULLY_SIGNED) {
            return contractAgreement;
        }
        if (contractAgreement.getPdfPublicUrl() != null && !contractAgreement.getPdfPublicUrl().isBlank()) {
            return contractAgreement;
        }
        ContractPdfService.GeneratedContractPdf generatedPdf = contractPdfService.generateFinalPdf(contractAgreement);
        contractAgreement.setPdfFilePath(generatedPdf.filePath());
        contractAgreement.setPdfPublicUrl(generatedPdf.publicUrl());
        contractAgreement.setPdfChecksum(generatedPdf.sha256Checksum());
        if (contractAgreement.getPdfGeneratedAt() == null) {
            contractAgreement.setPdfGeneratedAt(LocalDateTime.now());
        }
        return contractAgreementRepository.save(contractAgreement);
    }
}
