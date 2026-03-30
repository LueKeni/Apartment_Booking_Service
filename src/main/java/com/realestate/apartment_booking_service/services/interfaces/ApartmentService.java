package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.ApartmentFilterRequest;
import com.realestate.apartment_booking_service.entities.Apartment;
import java.util.List;

public interface ApartmentService {

    Apartment createApartment(Apartment apartment, Long agentId);

    Apartment updateApartment(Long apartmentId, Apartment payload, Long agentId);

    Apartment findById(Long apartmentId);

    List<Apartment> search(ApartmentFilterRequest filterRequest);

    List<Apartment> findByAgent(Long agentId);

    Apartment updateStatus(Long apartmentId, String status);
}
