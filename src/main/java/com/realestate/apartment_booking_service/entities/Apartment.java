package com.realestate.apartment_booking_service.entities;

import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.enums.TransactionType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "apartments")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(precision = 18, scale = 2)
    private BigDecimal pricePerM2;

    @Column(nullable = false)
    private Double area;

    @Column(nullable = false, length = 255)
    private String locationAddress;

    @Column(length = 120)
    private String locationDistrict;

    @Column(length = 20)
    private String locationProvinceCode;

    @Column(length = 120)
    private String locationProvince;

    @Column(length = 20)
    private String locationDistrictCode;

    @Column(length = 20)
    private String locationWardCode;

    @Column(length = 120)
    private String locationWard;

    @Column(length = 120)
    private String legalStatus;

    @Column(length = 20)
    private String roomType;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 120)
    private String furnitureStatus;

    @Column(length = 80)
    private String doorDirection;

    @Column(length = 80)
    private String balconyDirection;

    @Column
    private Integer floorNumber;

    @Column(length = 80)
    private String buildingBlock;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApartmentStatus status;

    @Column
    @Default
    private Long boostPoints = 0L;

    @Column
    @Default
    private Long likesCount = 0L;

    @ElementCollection
    @CollectionTable(name = "apartment_images", joinColumns = @JoinColumn(name = "apartment_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", nullable = false, length = 500)
    @Default
    private List<String> images = new ArrayList<>();

    public String getPrimaryImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        String firstImage = images.get(0);
        return hasText(firstImage) ? firstImage.trim() : null;
    }

    public String getLocationAreaLabel() {
        List<String> areaParts = new ArrayList<>();
        if (hasText(locationWard)) {
            areaParts.add(locationWard.trim());
        }
        if (hasText(locationDistrict)) {
            areaParts.add(locationDistrict.trim());
        }
        if (hasText(locationProvince)) {
            areaParts.add(locationProvince.trim());
        }
        return areaParts.isEmpty() ? null : String.join(", ", areaParts);
    }

    public String getFullAddressLabel() {
        List<String> addressParts = new ArrayList<>();
        if (hasText(locationAddress)) {
            addressParts.add(locationAddress.trim());
        }
        String areaLabel = getLocationAreaLabel();
        if (hasText(areaLabel)) {
            addressParts.add(areaLabel);
        }
        return addressParts.isEmpty() ? null : String.join(", ", addressParts);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
