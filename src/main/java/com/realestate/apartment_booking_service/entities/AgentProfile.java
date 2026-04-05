package com.realestate.apartment_booking_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_profiles")
public class  AgentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private Double responseRate;

    @Column
    private Integer activeListings;

    @Column
    private Integer successDeals;

    @Column(nullable = false)
    private Boolean verifiedStatus;

    @Column(nullable = false)
    private Double averageRating;

    @Column(nullable = false)
    private Integer reviewCount;

    @Column(length = 500)
    private String idCardImage;

    @Column(length = 500)
    private String portraitImage;

    @Column(length = 40)
    private String idCardNumber;

    @Column(length = 180)
    private String idCardName;

    @Column(length = 60)
    private String idCardDob;

    @Column(length = 40)
    private String idCardType;

    @Column(length = 40)
    private String idCardTypeNew;

    @Column(columnDefinition = "TEXT")
    private String idCardAddress;

    @Column
    private LocalDateTime verificationSubmittedAt;

    @Column
    private LocalDateTime verificationReviewedAt;

    @Column(length = 255)
    private String verificationNote;

    @PrePersist
    public void prePersist() {
        if (averageRating == null) {
            averageRating = 0.0;
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }
        if (verifiedStatus == null) {
            verifiedStatus = false;
        }
    }
}
