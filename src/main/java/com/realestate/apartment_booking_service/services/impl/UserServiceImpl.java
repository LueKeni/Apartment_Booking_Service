package com.realestate.apartment_booking_service.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.apartment_booking_service.dto.RegisterRequest;
import com.realestate.apartment_booking_service.entities.AgentProfile;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.NotificationType;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.enums.UserStatus;
import com.realestate.apartment_booking_service.repositories.AgentProfileRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.NotificationService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import jakarta.transaction.Transactional;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES =
        List.of("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars";
    private static final String AVATAR_PUBLIC_PREFIX = "/uploads/avatars";
    private static final String ID_CARD_UPLOAD_DIR = "uploads/verifications/id-cards";
    private static final String ID_CARD_PUBLIC_PREFIX = "/uploads/verifications/id-cards";
    private static final String PORTRAIT_UPLOAD_DIR = "uploads/verifications/portraits";
    private static final String PORTRAIT_PUBLIC_PREFIX = "/uploads/verifications/portraits";
    private static final long MAX_ID_CARD_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> FRONT_SIDE_TYPES = Set.of("old", "new", "chip_front");
    private static final Set<String> FRONT_SIDE_TYPE_NEW =
        Set.of("cmnd_09_front", "cmnd_12_front", "cccd_12_front", "cccd_chip_front");

    @Value("${fpt.ai.idr.url:https://api.fpt.ai/vision/idr/vnm/}")
    private String fptIdRecognitionUrl;

    @Value("${fpt.ai.idr.api-key:}")
    private String fptIdRecognitionApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User updateUserStatus(Long userId, boolean banned) {
        User user = findById(userId);
        user.setStatus(banned ? UserStatus.BANNED : UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Override
    public User verifyAgent(Long userId, boolean verified) {
        User user = findById(userId);
        if (user.getRole() != Role.AGENT) {
            user.setRole(Role.AGENT);
            userRepository.save(user);
        }

        AgentProfile profile = agentProfileRepository.findByUserId(userId)
                .orElseGet(() -> agentProfileRepository.save(createDefaultAgentProfile(user)));

        if (profile.getVerificationSubmittedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_not_submitted");
        }

        LocalDateTime previousReviewedAt = profile.getVerificationReviewedAt();
        boolean previousVerified = Boolean.TRUE.equals(profile.getVerifiedStatus());

        profile.setVerifiedStatus(verified);
        profile.setVerificationReviewedAt(LocalDateTime.now());
        profile.setVerificationNote(verified ? "APPROVED" : "REJECTED");
        agentProfileRepository.save(profile);

        if (previousReviewedAt == null || previousVerified != verified) {
            sendVerificationDecisionNotification(user, verified);
        }

        return user;
    }

    @Override
    public AgentProfile findAgentProfileByUserId(Long userId) {
        User user = findById(userId);
        return agentProfileRepository.findByUserId(userId)
                .orElseGet(() -> agentProfileRepository.save(createDefaultAgentProfile(user)));
    }

    @Override
    public AgentProfile submitAgentVerification(Long userId, MultipartFile idCardFile, MultipartFile portraitFile) {
        User user = findById(userId);
        if (user.getRole() != Role.AGENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_only_for_agent");
        }

        validateVerificationImage(idCardFile, true);
        validateVerificationImage(portraitFile, false);

        JsonNode idCardData = requestFptIdRecognition(idCardFile);

        AgentProfile profile = agentProfileRepository.findByUserId(userId)
                .orElseGet(() -> agentProfileRepository.save(createDefaultAgentProfile(user)));

        profile.setIdCardImage(storeImageFile(idCardFile, ID_CARD_UPLOAD_DIR, ID_CARD_PUBLIC_PREFIX));
        profile.setPortraitImage(storeImageFile(portraitFile, PORTRAIT_UPLOAD_DIR, PORTRAIT_PUBLIC_PREFIX));
        profile.setIdCardNumber(sanitizeOcrValue(idCardData.path("id").asText(null)));
        profile.setIdCardName(sanitizeOcrValue(idCardData.path("name").asText(null)));
        profile.setIdCardDob(sanitizeOcrValue(idCardData.path("dob").asText(null)));
        profile.setIdCardType(sanitizeOcrValue(idCardData.path("type").asText(null)));
        profile.setIdCardTypeNew(sanitizeOcrValue(idCardData.path("type_new").asText(null)));
        profile.setIdCardAddress(sanitizeOcrValue(idCardData.path("address").asText(null)));
        profile.setVerifiedStatus(false);
        profile.setVerificationSubmittedAt(LocalDateTime.now());
        profile.setVerificationReviewedAt(null);
        profile.setVerificationNote("PENDING");

        return agentProfileRepository.save(profile);
    }

    @Override
    public boolean canAgentPublishListing(Long userId) {
        return agentProfileRepository.findByUserId(userId)
                .map(profile -> Boolean.TRUE.equals(profile.getVerifiedStatus()))
                .orElse(false);
    }

    @Override
    public User updateUserRole(Long userId, Role role) {
        User user = findById(userId);
        user.setRole(role);
        User savedUser = userRepository.save(user);

        if (role == Role.AGENT) {
            agentProfileRepository.findByUserId(userId)
                    .orElseGet(() -> agentProfileRepository.save(createDefaultAgentProfile(savedUser)));
        }

        return savedUser;
    }

    @Override
    public User updateProfile(Long userId, String fullName, String phone, String avatar, MultipartFile avatarFile) {
        User user = findById(userId);

        if (fullName == null || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        user.setFullName(fullName.trim());
        user.setPhone(normalizeBlankToNull(phone));

        String storedAvatarPath = storeAvatarFile(avatarFile);
        if (storedAvatarPath != null) {
            user.setAvatar(storedAvatarPath);
        } else {
            user.setAvatar(normalizeBlankToNull(avatar));
        }

        return userRepository.save(user);
    }

    private AgentProfile createDefaultAgentProfile(User user) {
        return AgentProfile.builder()
                .user(user)
                .responseRate(0.0)
                .activeListings(0)
                .successDeals(0)
                .verifiedStatus(false)
                .averageRating(0.0)
                .reviewCount(0)
                .build();
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String storeAvatarFile(MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return null;
        }
        validateAvatarFile(avatarFile);
        return storeImageFile(avatarFile, AVATAR_UPLOAD_DIR, AVATAR_PUBLIC_PREFIX);
    }

    private String storeImageFile(MultipartFile file, String uploadDirectory, String publicPrefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Path uploadPath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Cannot prepare upload directory", exception);
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + extension;
        Path targetPath = uploadPath.resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot store image file", exception);
        }

        return publicPrefix + "/" + storedFileName;
    }

    private void validateAvatarFile(MultipartFile file) {
        validateImageContentType(file, "unsupported_avatar_type");
    }

    private void validateVerificationImage(MultipartFile file, boolean idCardFile) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_file_required");
        }

        validateImageContentType(file, "verification_invalid_image_type");

        BufferedImage image = readBufferedImage(file);
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_invalid_image_data");
        }

        if (idCardFile) {
            if (file.getSize() > MAX_ID_CARD_FILE_SIZE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_id_card_file_too_large");
            }
        }
    }

    private void validateImageContentType(MultipartFile file, String reason) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }
        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }
    }

    private BufferedImage readBufferedImage(MultipartFile file) {
        try {
            return ImageIO.read(file.getInputStream());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_invalid_image_data", exception);
        }
    }

    private JsonNode requestFptIdRecognition(MultipartFile idCardFile) {
        if (fptIdRecognitionApiKey == null || fptIdRecognitionApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_api_key_missing");
        }

        byte[] fileBytes;
        try {
            fileBytes = idCardFile.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_invalid_image_data", exception);
        }

        String fileName = StringUtils.hasText(idCardFile.getOriginalFilename())
                ? StringUtils.cleanPath(idCardFile.getOriginalFilename())
                : "id-card.jpg";

        ByteArrayResource imageResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", fptIdRecognitionApiKey.trim());
        headers.set("api_key", fptIdRecognitionApiKey.trim());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpHeaders partHeaders = new HttpHeaders();
        String contentType = idCardFile.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            partHeaders.setContentType(MediaType.parseMediaType(contentType));
        } else {
            partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("image", new HttpEntity<>(imageResource, partHeaders));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(fptIdRecognitionUrl, request, String.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "verification_ocr_call_failed", exception);
        }

        String bodyRaw = response.getBody();
        if (bodyRaw == null || bodyRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "verification_ocr_empty_response");
        }

        JsonNode body;
        try {
            body = objectMapper.readTree(bodyRaw);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "verification_ocr_invalid_response", exception);
        }

        int errorCode = body.path("errorCode").asInt(-1);
        if (errorCode != 0) {
            if (errorCode == 2 || errorCode == 3 || errorCode == 7 || errorCode == 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_not_id_card");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_ocr_failed");
        }

        JsonNode data = body.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_not_id_card");
        }

        JsonNode idCardData = data.get(0);
        String type = sanitizeOcrValue(idCardData.path("type").asText(null));
        String typeNew = sanitizeOcrValue(idCardData.path("type_new").asText(null));
        if (!isFrontSideDocument(type, typeNew)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_front_side_required");
        }

        return idCardData;
    }

    private boolean isFrontSideDocument(String type, String typeNew) {
        String normalizedType = normalizeOcrType(type);
        String normalizedTypeNew = normalizeOcrType(typeNew);
        return FRONT_SIDE_TYPES.contains(normalizedType) || FRONT_SIDE_TYPE_NEW.contains(normalizedTypeNew);
    }

    private String normalizeOcrType(String value) {
        String normalizedValue = normalizeBlankToNull(value);
        if (normalizedValue == null) {
            return "";
        }
        return normalizedValue.toLowerCase(Locale.ROOT);
    }

    private String sanitizeOcrValue(String value) {
        String normalizedValue = normalizeBlankToNull(value);
        if (normalizedValue == null) {
            return null;
        }
        if ("N/A".equalsIgnoreCase(normalizedValue)) {
            return null;
        }
        return normalizedValue;
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

    private void sendVerificationDecisionNotification(User user, boolean verified) {
        if (verified) {
            notificationService.createNotification(
                    user.getId(),
                    "Verification approved",
                    "Your collaborator verification has been approved. You can now publish listings.",
                    NotificationType.SYSTEM,
                    "/agent/listings");
            return;
        }

        notificationService.createNotification(
                user.getId(),
                "Verification needs resubmission",
                "Your collaborator verification was rejected. Please upload clearer ID card and portrait images.",
                NotificationType.SYSTEM,
                "/agent/profile");
    }
}
