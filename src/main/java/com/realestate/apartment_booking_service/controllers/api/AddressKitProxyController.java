package com.realestate.apartment_booking_service.controllers.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/address-kit")
public class AddressKitProxyController {

    private static final String ADDRESS_KIT_BASE_URL = "https://production.cas.so/address-kit";

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping(value = "/{effectiveDate}/provinces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProvinces(@PathVariable String effectiveDate) {
        String url = ADDRESS_KIT_BASE_URL + "/" + effectiveDate + "/provinces";
        return forwardGet(url);
    }

    @GetMapping(value = "/{effectiveDate}/provinces/{provinceCode}/communes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCommunesByProvince(
            @PathVariable String effectiveDate,
            @PathVariable String provinceCode) {
        String url = ADDRESS_KIT_BASE_URL + "/" + effectiveDate + "/provinces/" + provinceCode + "/communes";
        return forwardGet(url);
    }

    @GetMapping(value = "/{effectiveDate}/communes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllCommunes(@PathVariable String effectiveDate) {
        String url = ADDRESS_KIT_BASE_URL + "/" + effectiveDate + "/communes";
        return forwardGet(url);
    }

    private ResponseEntity<String> forwardGet(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), "AddressKit request failed", ex);
        } catch (RestClientException ex) {
            log.warn("AddressKit unavailable for url {}", url, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cannot reach AddressKit API", ex);
        }
    }
}
