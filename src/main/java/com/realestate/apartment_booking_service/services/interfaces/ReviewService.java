package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.CreateReviewRequest;
import com.realestate.apartment_booking_service.entities.Review;
import java.util.List;
import java.util.Set;

public interface ReviewService {

    Review createReview(Long userId, CreateReviewRequest request);

    List<Review> getAgentReviews(Long agentId);

    Set<Long> getReviewedAppointmentIds(Long userId);
}
