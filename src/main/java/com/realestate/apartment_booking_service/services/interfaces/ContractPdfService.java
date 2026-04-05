package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.entities.ContractAgreement;

public interface ContractPdfService {

    GeneratedContractPdf generateFinalPdf(ContractAgreement contractAgreement);

    record GeneratedContractPdf(String filePath, String publicUrl, String sha256Checksum) {
    }
}
