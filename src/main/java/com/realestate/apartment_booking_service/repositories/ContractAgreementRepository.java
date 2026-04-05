package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.ContractAgreement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractAgreementRepository extends JpaRepository<ContractAgreement, Long> {

    @EntityGraph(attributePaths = { "appointment", "appointment.user", "appointment.agent", "appointment.apartment" })
    Optional<ContractAgreement> findByAppointmentId(Long appointmentId);

    @EntityGraph(attributePaths = { "appointment", "appointment.user", "appointment.agent", "appointment.apartment" })
    Optional<ContractAgreement> findByAppointmentIdAndAppointmentUserId(Long appointmentId, Long userId);

    @EntityGraph(attributePaths = { "appointment", "appointment.user", "appointment.agent", "appointment.apartment" })
    Optional<ContractAgreement> findByAppointmentIdAndAppointmentAgentId(Long appointmentId, Long agentId);

    List<ContractAgreement> findByAppointmentUserIdAndAppointmentIdInAndUserSignedAtIsNotNull(
            Long userId,
            Collection<Long> appointmentIds);

    List<ContractAgreement> findByAppointmentAgentIdAndAppointmentIdInAndUserSignedAtIsNotNull(
            Long agentId,
            Collection<Long> appointmentIds);

    List<ContractAgreement> findByAppointmentAgentIdAndAppointmentIdInAndAgentSignedAtIsNotNull(
            Long agentId,
            Collection<Long> appointmentIds);
}
