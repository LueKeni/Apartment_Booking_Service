package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.PointUsage;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointUsageRepository extends JpaRepository<PointUsage, Long> {

    @EntityGraph(attributePaths = {"apartment"})
    List<PointUsage> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
