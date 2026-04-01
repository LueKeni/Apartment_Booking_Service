package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.dto.ApartmentAssistantResponse;
import com.realestate.apartment_booking_service.dto.ApartmentAssistantResultDto;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.enums.TransactionType;
import com.realestate.apartment_booking_service.repositories.ApartmentRepository;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentAssistantService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApartmentAssistantServiceImpl implements ApartmentAssistantService {

    private static final Pattern NUMBER_WITH_UNIT_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(tỷ|ty|ti|billion|triệu|trieu|million|k|nghìn|nghin|ngan)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "(?:từ|tu|from)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(tỷ|ty|ti|billion|triệu|trieu|million|k|nghìn|nghin|ngan)?\\s*"
                    + "(?:đến|den|to|-)\\s*(\\d+(?:[.,]\\d+)?)\\s*(tỷ|ty|ti|billion|triệu|trieu|million|k|nghìn|nghin|ngan)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DISTRICT_PATTERN = Pattern.compile(
            "(quan|district)\\s*([a-z0-9]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN_NUMBER_PATTERN = Pattern.compile(
            "\\b(\\d+(?:[.,]\\d+)?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Map<String, List<String>> FEATURE_ALIASES = Map.ofEntries(
            Map.entry("cho phep nuoi pet", List.of("pet", "thu cung", "nuoi pet", "cho nuoi pet", "pet friendly")),
            Map.entry("co ho boi", List.of("ho boi", "be boi", "swimming pool", "pool")),
            Map.entry("co phong gym", List.of("gym", "phong gym", "fitness")),
            Map.entry("full noi that", List.of("full noi that", "day du noi that", "furnished", "noi that")),
            Map.entry("co ban cong", List.of("ban cong", "balcony")),
            Map.entry("co may giat", List.of("may giat", "washing machine")),
            Map.entry("co bai do xe", List.of("bai do xe", "cho de xe", "parking", "gara")),
            Map.entry("co bao ve", List.of("bao ve", "security", "an ninh", "le tan")),
            Map.entry("co thang may", List.of("thang may", "elevator", "lift")),
            Map.entry("co thang bo", List.of("thang bo", "cau thang bo", "stair", "stairs")));
    private static final Set<String> DESCRIPTION_STOP_WORDS = Set.of(
            "tim", "kiem", "can", "ho", "phong", "chung", "cu", "mo", "ta", "co", "o", "tai", "duoi", "tren",
            "tu", "den", "gia", "quan", "district", "rent", "rental", "sale", "buy", "mua", "ban", "thue",
            "studio", "duplex", "br", "pn", "phongngu", "phongtam", "wc");

    private final ApartmentRepository apartmentRepository;

    @Override
    public ApartmentAssistantResponse answer(String message) {
        String normalizedMessage = normalize(message);
        if (normalizedMessage.isBlank()) {
            return ApartmentAssistantResponse.builder()
                    .answer("Vui lòng nhập nhu cầu căn hộ, ví dụ: can ho thue duoi 15 trieu, 2br, quan 2.")
                    .appliedFilters(List.of())
                    .suggestions(List.of())
                    .build();
        }

        if (isOutOfScope(normalizedMessage)) {
            return ApartmentAssistantResponse.builder()
                    .answer("Tôi chỉ hỗ trợ tìm căn hộ theo giá, quận, loại phòng và hình thức thuê hoặc mua.")
                    .appliedFilters(List.of())
                    .suggestions(List.of())
                    .build();
        }

        ParsedQuery query = parse(normalizedMessage);
        List<Apartment> matches = apartmentRepository.findByStatus(ApartmentStatus.AVAILABLE).stream()
                .filter(apartment -> matches(apartment, query))
                .sorted(Comparator.comparing(Apartment::getBoostPoints, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Apartment::getId, Comparator.reverseOrder()))
                .limit(5)
                .toList();

        List<String> appliedFilters = describeFilters(query);
        String answer = buildAnswer(query, matches, appliedFilters);
        List<ApartmentAssistantResultDto> suggestions = matches.stream()
                .map(this::toSuggestion)
                .toList();

        return ApartmentAssistantResponse.builder()
                .answer(answer)
                .appliedFilters(appliedFilters)
                .suggestions(suggestions)
                .build();
    }

    private boolean matches(Apartment apartment, ParsedQuery query) {
        if (query.transactionType() != null && apartment.getTransactionType() != query.transactionType()) {
            return false;
        }
        if (query.roomType() != null) {
            String roomType = apartment.getRoomType() == null ? "" : apartment.getRoomType();
            if (!roomType.equalsIgnoreCase(query.roomType())) {
                return false;
            }
        }
        if (query.district() != null) {
            String district = apartment.getLocationDistrict() == null ? "" : apartment.getLocationDistrict();
            if (!district.toLowerCase(Locale.ROOT).contains(query.district().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (query.minPrice() != null && apartment.getPrice().compareTo(query.minPrice()) < 0) {
            return false;
        }
        if (query.maxPrice() != null && apartment.getPrice().compareTo(query.maxPrice()) > 0) {
            return false;
        }
        if (!query.featureKeywords().isEmpty()) {
            String searchableText = normalize(buildSearchableText(apartment));
            for (String featureKeyword : query.featureKeywords()) {
                List<String> aliases = FEATURE_ALIASES.getOrDefault(featureKeyword, List.of(featureKeyword));
                boolean matched = aliases.stream().anyMatch(searchableText::contains);
                if (!matched) {
                    return false;
                }
            }
        }
        if (!query.descriptionTerms().isEmpty()) {
            String searchableText = normalize(buildSearchableText(apartment));
            for (String descriptionTerm : query.descriptionTerms()) {
                if (!searchableText.contains(descriptionTerm)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ParsedQuery parse(String message) {
        TransactionType transactionType = extractTransactionType(message);
        String roomType = extractRoomType(message);
        String district = extractDistrict(message);
        PriceRange priceRange = extractPriceRange(message);
        List<String> featureKeywords = extractFeatureKeywords(message);
        List<String> descriptionTerms = extractDescriptionTerms(message, featureKeywords, district);

        return new ParsedQuery(
                transactionType,
                roomType,
                district,
                featureKeywords,
                descriptionTerms,
                priceRange.minPrice(),
                priceRange.maxPrice());
    }

    private TransactionType extractTransactionType(String message) {
        if (containsAny(message, "thuê", "thue", "rent", "rental")) {
            return TransactionType.RENT;
        }
        if (containsAny(message, "mua", "bán", "ban", "sale", "buy")) {
            return TransactionType.SALE;
        }
        return null;
    }

    private String extractRoomType(String message) {
        if (containsAny(message, "studio")) {
            return "STUDIO";
        }
        if (containsAny(message, "duplex")) {
            return "DUPLEX";
        }
        if (containsAny(message, "1br", "1 br")) {
            return "1BR";
        }
        if (containsAny(message, "2br", "2 br")) {
            return "2BR";
        }
        if (containsAny(message, "3br", "3 br")) {
            return "3BR";
        }
        return null;
    }

    private String extractDistrict(String message) {
        Matcher matcher = DISTRICT_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(2).toLowerCase(Locale.ROOT).trim();
    }

    private PriceRange extractPriceRange(String message) {
        Matcher rangeMatcher = RANGE_PATTERN.matcher(message);
        if (rangeMatcher.find()) {
            BigDecimal first = toAmount(rangeMatcher.group(1), rangeMatcher.group(2));
            BigDecimal second = toAmount(rangeMatcher.group(3), rangeMatcher.group(4));
            if (first != null && second != null) {
                return new PriceRange(first.min(second), first.max(second));
            }
        }

        Matcher matcher = NUMBER_WITH_UNIT_PATTERN.matcher(message);
        List<BigDecimal> detectedAmounts = new ArrayList<>();
        while (matcher.find()) {
            BigDecimal amount = toAmount(matcher.group(1), matcher.group(2));
            if (amount != null) {
                detectedAmounts.add(amount);
            }
        }

        if (detectedAmounts.isEmpty()) {
            BigDecimal plainAmount = extractPlainAmount(message);
            if (plainAmount == null) {
                return new PriceRange(null, null);
            }
            if (containsAny(message, "tu ", "from", "tren", "it nhat", "toi thieu", "over", "min")) {
                return new PriceRange(plainAmount, null);
            }
            return new PriceRange(null, plainAmount);
        }

        BigDecimal amount = detectedAmounts.getFirst();
        if (containsAny(message, "dưới", "duoi", "tối đa", "toi da", "không quá", "khong qua", "under", "max")) {
            return new PriceRange(null, amount);
        }
        if (containsAny(message, "trên", "tren", "ít nhất", "it nhat", "tối thiểu", "toi thieu", "over", "min")) {
            return new PriceRange(amount, null);
        }
        if (containsAny(message, "tầm giá", "tam gia", "ngân sách", "ngan sach", "budget", "khoảng", "khoang")) {
            return new PriceRange(null, amount);
        }
        return new PriceRange(null, amount);
    }

    private BigDecimal extractPlainAmount(String message) {
        Matcher matcher = PLAIN_NUMBER_PATTERN.matcher(message);
        while (matcher.find()) {
            String token = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);
            String before = message.substring(Math.max(0, start - 16), start);
            String after = message.substring(end, Math.min(message.length(), end + 16));

            if (looksLikeRoomContext(before, after) || looksLikeDistrictContext(before)) {
                continue;
            }

            return new BigDecimal(token.replace(",", "."));
        }
        return null;
    }

    private boolean looksLikeRoomContext(String before, String after) {
        String context = (before + " " + after).toLowerCase(Locale.ROOT);
        return containsAny(context, "phong ngu", "phong tam", "pn", "wc", "bed", "bath", "br");
    }

    private boolean looksLikeDistrictContext(String before) {
        String normalizedBefore = before.toLowerCase(Locale.ROOT);
        return normalizedBefore.contains("quan ") || normalizedBefore.contains("district ");
    }

    private BigDecimal toAmount(String value, String unit) {
        if (value == null || value.isBlank()) {
            return null;
        }
        BigDecimal amount = new BigDecimal(value.replace(",", "."));
        String normalizedUnit = unit == null ? "" : unit.toLowerCase(Locale.ROOT);
        if (normalizedUnit.isBlank() || "k".equals(normalizedUnit) || "nghìn".equals(normalizedUnit)
                || "nghin".equals(normalizedUnit) || "ngan".equals(normalizedUnit)) {
            return amount.multiply(BigDecimal.valueOf(1_000L));
        }
        if ("triệu".equals(normalizedUnit) || "trieu".equals(normalizedUnit) || "million".equals(normalizedUnit)) {
            return amount.multiply(BigDecimal.valueOf(1_000_000L));
        }
        if ("tỷ".equals(normalizedUnit) || "ty".equals(normalizedUnit) || "ti".equals(normalizedUnit)
                || "billion".equals(normalizedUnit)) {
            return amount.multiply(BigDecimal.valueOf(1_000_000_000L));
        }
        return amount;
    }

    private List<String> extractFeatureKeywords(String message) {
        List<String> features = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : FEATURE_ALIASES.entrySet()) {
            if (entry.getValue().stream().anyMatch(message::contains)) {
                features.add(entry.getKey());
            }
        }
        return features;
    }

    private List<String> extractDescriptionTerms(String message, List<String> featureKeywords, String district) {
        String cleaned = message;
        cleaned = DISTRICT_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = RANGE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = NUMBER_WITH_UNIT_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = PLAIN_NUMBER_PATTERN.matcher(cleaned).replaceAll(" ");

        if (district != null) {
            cleaned = cleaned.replace(district, " ");
        }
        for (String featureKeyword : featureKeywords) {
            cleaned = cleaned.replace(featureKeyword, " ");
            for (String alias : FEATURE_ALIASES.getOrDefault(featureKeyword, List.of())) {
                cleaned = cleaned.replace(alias, " ");
            }
        }

        String[] tokens = cleaned.split("[,.;:\\-_/()\\s]+");
        Set<String> terms = new LinkedHashSet<>();
        for (String token : tokens) {
            String normalizedToken = token.trim();
            if (normalizedToken.length() < 3 || DESCRIPTION_STOP_WORDS.contains(normalizedToken)) {
                continue;
            }
            terms.add(normalizedToken);
        }
        return new ArrayList<>(terms);
    }

    private String buildSearchableText(Apartment apartment) {
        return String.join(" ",
                safe(apartment.getTitle()),
                safe(apartment.getDescription()),
                safe(apartment.getLocationAddress()),
                safe(apartment.getLocationDistrict()),
                safe(apartment.getLegalStatus()),
                safe(apartment.getRoomType()),
                safe(apartment.getFurnitureStatus()),
                safe(apartment.getDoorDirection()),
                safe(apartment.getBalconyDirection()),
                safe(apartment.getBuildingBlock()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> describeFilters(ParsedQuery query) {
        List<String> filters = new ArrayList<>();
        if (query.transactionType() != null) {
            filters.add(query.transactionType() == TransactionType.RENT ? "Giao dịch: thuê" : "Giao dịch: mua bán");
        }
        if (query.maxPrice() != null && query.minPrice() != null) {
            filters.add("Giá từ " + formatPrice(query.minPrice()) + " đến " + formatPrice(query.maxPrice()));
        } else if (query.maxPrice() != null) {
            filters.add("Giá tối đa " + formatPrice(query.maxPrice()));
        } else if (query.minPrice() != null) {
            filters.add("Giá từ " + formatPrice(query.minPrice()));
        }
        if (query.roomType() != null) {
            filters.add("Loại phòng: " + query.roomType());
        }
        if (query.district() != null) {
            filters.add("Khu vực: " + query.district());
        }
        query.featureKeywords().forEach(feature -> filters.add("Tien ich/mo ta: " + feature));
        query.descriptionTerms().forEach(term -> filters.add("Tu khoa mo ta: " + term));
        return filters;
    }

    private String buildAnswer(ParsedQuery query, List<Apartment> matches, List<String> appliedFilters) {
        if (appliedFilters.isEmpty()) {
            return "Tôi có thể lọc căn hộ theo giá, quận, loại phòng và thuê hoặc mua. Hãy thử: can ho thue duoi 15 trieu, 2br, quan 2.";
        }

        if (matches.isEmpty()) {
            return "Không tìm thấy căn hộ phù hợp với: " + String.join(", ", appliedFilters)
                    + ". Bạn có thể nới giá hoặc đổi quận để xem thêm kết quả.";
        }

        return "Tôi tìm thấy " + matches.size() + " căn hộ phù hợp với: " + String.join(", ", appliedFilters)
                + ". Danh sách bên dưới là các lựa chọn nổi bật hiện có.";
    }

    private ApartmentAssistantResultDto toSuggestion(Apartment apartment) {
        String imageUrl = apartment.getImages() == null || apartment.getImages().isEmpty()
                ? ""
                : apartment.getImages().getFirst();
        return ApartmentAssistantResultDto.builder()
                .id(apartment.getId())
                .title(apartment.getTitle())
                .priceLabel(formatPrice(apartment.getPrice()))
                .district(apartment.getLocationDistrict())
                .roomType(apartment.getRoomType())
                .detailUrl("/apartments/" + apartment.getId())
                .imageUrl(imageUrl)
                .build();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "Contact";
        }
        if (System.currentTimeMillis() >= 0) {
            DecimalFormat formatter = new DecimalFormat("#,##0.##");
            return "$" + formatter.format(price);
        }

        BigDecimal billion = BigDecimal.valueOf(1_000_000_000L);
        BigDecimal million = BigDecimal.valueOf(1_000_000L);
        DecimalFormat formatter = new DecimalFormat("0.#");
        if (price.compareTo(billion) >= 0) {
            return formatter.format(price.divide(billion, 1, RoundingMode.HALF_UP)) + " tỷ";
        }
        if (price.compareTo(million) >= 0) {
            return formatter.format(price.divide(million, 1, RoundingMode.HALF_UP)) + " triệu";
        }
        return formatter.format(price) + " VND";
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOutOfScope(String message) {
        return containsAny(message, "thoi tiet", "weather", "bong da", "football", "code", "lap trinh");
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private record ParsedQuery(
            TransactionType transactionType,
            String roomType,
            String district,
            List<String> featureKeywords,
            List<String> descriptionTerms,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
    }
}
