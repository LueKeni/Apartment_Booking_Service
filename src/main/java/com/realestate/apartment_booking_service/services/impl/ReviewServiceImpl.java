package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.dto.CreateReviewRequest;
import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.entities.Review;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import com.realestate.apartment_booking_service.enums.NotificationType;
import com.realestate.apartment_booking_service.repositories.AppointmentRepository;
import com.realestate.apartment_booking_service.repositories.ReviewRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.NotificationService;
import com.realestate.apartment_booking_service.services.interfaces.ReviewService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.realestate.apartment_booking_service.repositories.AgentProfileRepository agentProfileRepository;

    @Override
    public Review createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this appointment");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Review is allowed only for completed appointments");
        }

        if (reviewRepository.existsByAppointmentIdAndUserId(appointment.getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this appointment");
        }

        Review review = Review.builder()
                .user(user)
                .agent(appointment.getAgent())
                .appointment(appointment)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        // Update Agent Profile rating
        com.realestate.apartment_booking_service.entities.AgentProfile profile = agentProfileRepository
                .findByUserId(appointment.getAgent().getId())
                .orElseGet(() -> {
                    com.realestate.apartment_booking_service.entities.AgentProfile newProfile = com.realestate.apartment_booking_service.entities.AgentProfile
                            .builder()
                            .user(appointment.getAgent())
                            .responseRate(0.0)
                            .activeListings(0)
                            .successDeals(0)
                            .verifiedStatus(false)
                            .averageRating(0.0)
                            .reviewCount(0)
                            .build();
                    return agentProfileRepository.save(newProfile);
                });

        double currentTotalRating = profile.getAverageRating() * profile.getReviewCount();
        profile.setReviewCount(profile.getReviewCount() + 1);
        profile.setAverageRating((currentTotalRating + request.getRating()) / profile.getReviewCount());
        agentProfileRepository.save(profile);

        notificationService.createNotification(
                appointment.getAgent().getId(),
                "New review received",
                user.getFullName() + " submitted a " + request.getRating() + "-star review",
                NotificationType.SYSTEM,
                "/agent/profile");

        return saved;
    }

    @Override
    public List<Review> getAgentReviews(Long agentId) {
        return reviewRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    @Override
    public Set<Long> getReviewedAppointmentIds(Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .map(review -> review.getAppointment().getId())
                .collect(java.util.stream.Collectors.toSet());
    }
}
