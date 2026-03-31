package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.PointTopUp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTopUpRepository extends JpaRepository<PointTopUp, Long> {

    Optional<PointTopUp> findByOrderId(String orderId);
}
