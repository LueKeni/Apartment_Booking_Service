package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.ApartmentLike;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentLikeRepository extends JpaRepository<ApartmentLike, Long> {

    boolean existsByUserIdAndApartmentId(Long userId, Long apartmentId);

    long countByApartmentId(Long apartmentId);

    void deleteByUserIdAndApartmentId(Long userId, Long apartmentId);

    long deleteByApartmentId(Long apartmentId);

    List<ApartmentLike> findByUserId(Long userId);
}
