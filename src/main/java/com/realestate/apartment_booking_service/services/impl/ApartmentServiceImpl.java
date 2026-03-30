package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.dto.ApartmentFilterRequest;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.repositories.ApartmentRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ApartmentServiceImpl implements ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    @Override
    public Apartment createApartment(Apartment apartment, Long agentId) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        apartment.setId(null);
        apartment.setAgent(agent);
        if (apartment.getStatus() == null) {
            apartment.setStatus(ApartmentStatus.AVAILABLE);
        }

        return apartmentRepository.save(apartment);
    }

    @Override
    public Apartment updateApartment(Long apartmentId, Apartment payload, Long agentId) {
        Apartment apartment = findById(apartmentId);
        if (!apartment.getAgent().getId().equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this apartment");
        }

        apartment.setTitle(payload.getTitle());
        apartment.setTransactionType(payload.getTransactionType());
        apartment.setPrice(payload.getPrice());
        apartment.setPricePerM2(payload.getPricePerM2());
        apartment.setArea(payload.getArea());
        apartment.setLocationAddress(payload.getLocationAddress());
        apartment.setLocationDistrict(payload.getLocationDistrict());
        apartment.setLegalStatus(payload.getLegalStatus());
        apartment.setBedrooms(payload.getBedrooms());
        apartment.setBathrooms(payload.getBathrooms());
        apartment.setFurnitureStatus(payload.getFurnitureStatus());
        apartment.setDoorDirection(payload.getDoorDirection());
        apartment.setBalconyDirection(payload.getBalconyDirection());
        apartment.setFloorNumber(payload.getFloorNumber());
        apartment.setBuildingBlock(payload.getBuildingBlock());
        apartment.setDescription(payload.getDescription());
        apartment.setImages(payload.getImages());

        return apartmentRepository.save(apartment);
    }

    @Override
    public Apartment findById(Long apartmentId) {
        return apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"));
    }

    @Override
    public List<Apartment> search(ApartmentFilterRequest filterRequest) {
        return apartmentRepository.search(
                filterRequest.getDistrict(),
                filterRequest.getTransactionType(),
                filterRequest.getStatus());
    }

    @Override
    public List<Apartment> findByAgent(Long agentId) {
        return apartmentRepository.findByAgentId(agentId);
    }

    @Override
    public Apartment updateStatus(Long apartmentId, String status) {
        Apartment apartment = findById(apartmentId);
        apartment.setStatus(ApartmentStatus.valueOf(status.toUpperCase()));
        return apartmentRepository.save(apartment);
    }
}
