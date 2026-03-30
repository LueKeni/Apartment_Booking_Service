package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndIsReadFalse(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);
}
