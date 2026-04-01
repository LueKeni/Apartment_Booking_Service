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
        @EntityGraph(attributePaths = { "agent", "images" })
        Optional<Apartment> findById(Long id);

        @EntityGraph(attributePaths = { "agent", "images" })
        List<Apartment> findByAgentId(Long agentId);

        @EntityGraph(attributePaths = { "agent", "images" })
        List<Apartment> findByStatus(ApartmentStatus status);

        @EntityGraph(attributePaths = { "agent", "images" })
        @Query("""
                            select a from Apartment a
                                     left join a.agent ag
                                     left join ag.agentProfile ap
                            where (:keyword is null
                                   or lower(a.title) like lower(concat('%', :keyword, '%'))
                                   or lower(a.locationAddress) like lower(concat('%', :keyword, '%'))
                                   or lower(coalesce(a.locationDistrict, '')) like lower(concat('%', :keyword, '%')))
                              and (:district is null or lower(a.locationDistrict) = lower(:district))
                              and (:roomType is null or upper(a.roomType) = upper(:roomType))
                              and (:transactionType is null or a.transactionType = :transactionType)
                              and (:status is null or a.status = :status)
                            order by coalesce(a.boostPoints, 0) desc, coalesce(ap.averageRating, 0.0) desc, a.id desc
                        """)
        List<Apartment> search(
                        @Param("keyword") String keyword,
                        @Param("district") String district,
                        @Param("roomType") String roomType,
                        @Param("transactionType") TransactionType transactionType,
                        @Param("status") ApartmentStatus status);

        @EntityGraph(attributePaths = { "images" })
        List<Apartment> findTop6ByRoomTypeIgnoreCaseAndIdNotAndStatusOrderByIdDesc(
                        String roomType, Long apartmentId, ApartmentStatus status);

        @EntityGraph(attributePaths = { "images" })
        List<Apartment> findTop6ByIdNotAndStatusOrderByIdDesc(Long apartmentId, ApartmentStatus status);
}
