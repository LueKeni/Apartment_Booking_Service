package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.dto.ApartmentFilterRequest;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.ApartmentLike;
import com.realestate.apartment_booking_service.entities.Favorite;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.repositories.ApartmentLikeRepository;
import com.realestate.apartment_booking_service.repositories.AppointmentRepository;
import com.realestate.apartment_booking_service.repositories.ApartmentRepository;
import com.realestate.apartment_booking_service.repositories.FavoriteRepository;
import com.realestate.apartment_booking_service.repositories.ReviewRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ApartmentServiceImpl implements ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;
    private final ApartmentLikeRepository apartmentLikeRepository;
    private final AppointmentRepository appointmentRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES = List.of("image/jpeg", "image/jpg", "image/png",
            "image/webp", "image/gif");
    private static final String UPLOAD_DIR = "uploads";

    @Override
    public Apartment createApartment(Apartment apartment, Long agentId, List<MultipartFile> imageFiles) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        apartment.setId(null);
        apartment.setAgent(agent);
        apartment.setImages(storeImageFiles(imageFiles));
        if (apartment.getStatus() == null) {
            apartment.setStatus(ApartmentStatus.AVAILABLE);
        }

        return apartmentRepository.save(apartment);
    }

    @Override
    public Apartment updateApartment(Long apartmentId, Apartment payload, Long agentId,
            List<MultipartFile> imageFiles) {
        Apartment apartment = findById(apartmentId);
        validateOwnership(apartment, agentId);

        apartment.setTitle(payload.getTitle());
        apartment.setTransactionType(payload.getTransactionType());
        apartment.setPrice(payload.getPrice());
        apartment.setPricePerM2(payload.getPricePerM2());
        apartment.setArea(payload.getArea());
        apartment.setLocationAddress(payload.getLocationAddress());
        apartment.setLocationProvinceCode(payload.getLocationProvinceCode());
        apartment.setLocationProvince(payload.getLocationProvince());
        apartment.setLocationDistrictCode(payload.getLocationDistrictCode());
        apartment.setLocationDistrict(payload.getLocationDistrict());
        apartment.setLocationWardCode(payload.getLocationWardCode());
        apartment.setLocationWard(payload.getLocationWard());
        apartment.setLegalStatus(payload.getLegalStatus());
        apartment.setRoomType(payload.getRoomType());
        apartment.setLatitude(payload.getLatitude());
        apartment.setLongitude(payload.getLongitude());
        apartment.setBedrooms(payload.getBedrooms());
        apartment.setBathrooms(payload.getBathrooms());
        apartment.setFurnitureStatus(payload.getFurnitureStatus());
        apartment.setDoorDirection(payload.getDoorDirection());
        apartment.setBalconyDirection(payload.getBalconyDirection());
        apartment.setFloorNumber(payload.getFloorNumber());
        apartment.setBuildingBlock(payload.getBuildingBlock());
        apartment.setDescription(payload.getDescription());
        normalizeLocationFields(apartment);
        List<String> uploadedImages = storeImageFiles(imageFiles);
        if (!uploadedImages.isEmpty()) {
            List<String> mergedImages = new ArrayList<>();
            if (apartment.getImages() != null) {
                mergedImages.addAll(apartment.getImages());
            }
            mergedImages.addAll(uploadedImages);
            apartment.setImages(mergedImages);
        }

        return apartmentRepository.save(apartment);
    }

    @Override
    public Apartment findById(Long apartmentId) {
        return apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"));
    }

    @Override
    public List<Apartment> search(ApartmentFilterRequest filterRequest) {
        String keyword = normalizeBlankToNull(filterRequest.getKeyword());
        String district = normalizeBlankToNull(filterRequest.getDistrict());
        String roomType = normalizeBlankToNull(filterRequest.getRoomType());
        return apartmentRepository.search(
                keyword,
                district,
                roomType,
                filterRequest.getTransactionType(),
                filterRequest.getStatus());
    }

    @Override
    public List<Apartment> findRelatedApartments(Long apartmentId, String roomType) {
        String normalizedRoomType = normalizeBlankToNull(roomType);
        if (normalizedRoomType != null) {
            return apartmentRepository.findTop6ByRoomTypeIgnoreCaseAndIdNotAndStatusOrderByIdDesc(
                    normalizedRoomType, apartmentId, ApartmentStatus.AVAILABLE);
        }
        return apartmentRepository.findTop6ByIdNotAndStatusOrderByIdDesc(apartmentId, ApartmentStatus.AVAILABLE);
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

    @Override
    public Apartment updateStatusForAgent(Long apartmentId, ApartmentStatus status, Long agentId) {
        Apartment apartment = findById(apartmentId);
        validateOwnership(apartment, agentId);
        apartment.setStatus(status);
        return apartmentRepository.save(apartment);
    }

    @Override
    public void deleteApartment(Long apartmentId, Long agentId) {
        Apartment apartment = findById(apartmentId);
        validateOwnership(apartment, agentId);

        reviewRepository.deleteByAppointmentApartmentId(apartmentId);
        appointmentRepository.deleteByApartmentId(apartmentId);
        apartmentLikeRepository.deleteByApartmentId(apartmentId);
        favoriteRepository.deleteByApartmentId(apartmentId);
        apartmentRepository.delete(apartment);
    }

    @Override
    public void boostListing(Long apartmentId, Long agentId, long points) {
        if (points <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_points");
        }

        Apartment apartment = findById(apartmentId);
        validateOwnership(apartment, agentId);

        User agent = apartment.getAgent();
        long availablePoints = agent.getPoints() == null ? 0L : agent.getPoints();
        if (availablePoints < points) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "insufficient_points");
        }

        agent.setPoints(availablePoints - points);
        long currentBoost = apartment.getBoostPoints() == null ? 0L : apartment.getBoostPoints();
        apartment.setBoostPoints(currentBoost + points);

        userRepository.save(agent);
        apartmentRepository.save(apartment);
    }

    @Override
    public boolean toggleFavorite(Long apartmentId, Long userId) {
        Apartment apartment = findById(apartmentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean favoriteAdded;
        if (favoriteRepository.existsByUserIdAndApartmentId(userId, apartmentId)) {
            favoriteRepository.deleteByUserIdAndApartmentId(userId, apartmentId);
            favoriteAdded = false;
        } else {
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .apartment(apartment)
                    .build();
            favoriteRepository.save(favorite);
            favoriteAdded = true;
        }
        return favoriteAdded;
    }

    @Override
    @Transactional
    public boolean isFavorite(Long apartmentId, Long userId) {
        return favoriteRepository.existsByUserIdAndApartmentId(userId, apartmentId);
    }

    @Override
    public boolean toggleLike(Long apartmentId, Long userId) {
        Apartment apartment = findById(apartmentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean liked;
        if (apartmentLikeRepository.existsByUserIdAndApartmentId(userId, apartmentId)) {
            apartmentLikeRepository.deleteByUserIdAndApartmentId(userId, apartmentId);
            liked = false;
        } else {
            ApartmentLike apartmentLike = ApartmentLike.builder()
                    .user(user)
                    .apartment(apartment)
                    .build();
            apartmentLikeRepository.save(apartmentLike);
            liked = true;
        }

        apartment.setLikesCount(apartmentLikeRepository.countByApartmentId(apartmentId));
        apartmentRepository.save(apartment);
        return liked;
    }

    @Override
    @Transactional
    public boolean isLiked(Long apartmentId, Long userId) {
        return apartmentLikeRepository.existsByUserIdAndApartmentId(userId, apartmentId);
    }

    private List<String> storeImageFiles(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return new ArrayList<>();
        }

        Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Cannot prepare upload directory", exception);
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : imageFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            validateImageFile(file);

            String extension = resolveExtension(file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(storedFileName);
            try {
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot store image file",
                        exception);
            }
            imageUrls.add("/uploads/" + storedFileName);
        }
        return imageUrls;
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type");
        }

        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type");
        }
    }

    private String resolveExtension(String originalFilename) {
        String cleanedName = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dotIndex = cleanedName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanedName.length() - 1) {
            return ".jpg";
        }
        String extension = cleanedName.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (extension.length() > 10) {
            return ".jpg";
        }
        return extension;
    }

    private void validateOwnership(Apartment apartment, Long agentId) {
        if (!apartment.getAgent().getId().equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this apartment");
        }
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void normalizeLocationFields(Apartment apartment) {
        apartment.setLocationAddress(normalizeBlankToNull(apartment.getLocationAddress()));
        apartment.setLocationProvinceCode(normalizeBlankToNull(apartment.getLocationProvinceCode()));
        apartment.setLocationProvince(normalizeBlankToNull(apartment.getLocationProvince()));
        apartment.setLocationWardCode(normalizeBlankToNull(apartment.getLocationWardCode()));
        apartment.setLocationWard(normalizeBlankToNull(apartment.getLocationWard()));

        apartment.setLocationDistrictCode(null);
        apartment.setLocationDistrict(null);
    }
}
