package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.enums.TransactionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    @Override
    @EntityGraph(attributePaths = {"agent", "images"})
    Optional<Apartment> findById(Long id);

    @EntityGraph(attributePaths = {"agent", "images"})
    List<Apartment> findByAgentId(Long agentId);

    @EntityGraph(attributePaths = {"agent", "images"})
    List<Apartment> findByStatus(ApartmentStatus status);

    @EntityGraph(attributePaths = {"agent", "images"})
    @Query("""
                select a from Apartment a
                where (:district is null or lower(a.locationDistrict) = lower(:district))
                  and (:transactionType is null or a.transactionType = :transactionType)
                  and (:status is null or a.status = :status)
            """)
    List<Apartment> search(
            @Param("district") String district,
            @Param("transactionType") TransactionType transactionType,
            @Param("status") ApartmentStatus status);
}
