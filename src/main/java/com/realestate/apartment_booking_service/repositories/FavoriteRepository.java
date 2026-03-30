package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Favorite;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @EntityGraph(attributePaths = { "apartment" })
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndApartmentId(Long userId, Long apartmentId);

    void deleteByUserIdAndApartmentId(Long userId, Long apartmentId);
}
