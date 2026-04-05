package com.realestate.apartment_booking_service.entities;

import com.realestate.apartment_booking_service.enums.ContractAgreementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "contract_agreements")
public class ContractAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false, length = 50, unique = true)
    private String contractCode;

    @Column(nullable = false, length = 255)
    private String apartmentTitleSnapshot;

    @Column(length = 500)
    private String apartmentAddressSnapshot;

    @Column(length = 50)
    private String transactionTypeSnapshot;

    @Column(length = 100)
    private String priceSnapshot;

    @Column(length = 150)
    private String userPartyNameSnapshot;

    @Column(length = 150)
    private String agentPartyNameSnapshot;

    @Column(length = 50)
    private String viewingDateSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractAgreementStatus status;

    @Column(length = 150)
    private String userSignerName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String userSignatureDataUrl;

    private LocalDateTime userSignedAt;

    @Column(length = 150)
    private String agentSignerName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String agentSignatureDataUrl;

    private LocalDateTime agentSignedAt;

    @Column(length = 500)
    private String pdfFilePath;

    @Column(length = 500)
    private String pdfPublicUrl;

    @Column(length = 64)
    private String pdfChecksum;

    private LocalDateTime pdfGeneratedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = ContractAgreementStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
