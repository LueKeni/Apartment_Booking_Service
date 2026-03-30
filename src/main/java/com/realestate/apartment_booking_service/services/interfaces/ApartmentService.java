package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.ApartmentFilterRequest;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ApartmentService {

    Apartment createApartment(Apartment apartment, Long agentId, List<MultipartFile> imageFiles);

    Apartment updateApartment(Long apartmentId, Apartment payload, Long agentId, List<MultipartFile> imageFiles);

    Apartment findById(Long apartmentId);

    List<Apartment> search(ApartmentFilterRequest filterRequest);

    List<Apartment> findByAgent(Long agentId);

    Apartment updateStatus(Long apartmentId, String status);

    Apartment updateStatusForAgent(Long apartmentId, ApartmentStatus status, Long agentId);

    void deleteApartment(Long apartmentId, Long agentId);
}
