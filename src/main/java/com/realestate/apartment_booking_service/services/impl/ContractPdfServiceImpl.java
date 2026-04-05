package com.realestate.apartment_booking_service.services.impl;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.realestate.apartment_booking_service.entities.ContractAgreement;
import com.realestate.apartment_booking_service.services.interfaces.ContractPdfService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ContractPdfServiceImpl implements ContractPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public GeneratedContractPdf generateFinalPdf(ContractAgreement contractAgreement) {
        try {
            Path contractsDirectory = Paths.get("uploads", "contracts").toAbsolutePath().normalize();
            Files.createDirectories(contractsDirectory);

            String fileName = contractAgreement.getContractCode() + ".pdf";
            Path filePath = contractsDirectory.resolve(fileName);
            byte[] pdfBytes = buildPdfBytes(contractAgreement);
            Files.write(filePath, pdfBytes);

            return new GeneratedContractPdf(
                    filePath.toString(),
                    "/uploads/contracts/" + fileName,
                    sha256(pdfBytes));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot write contract PDF", ex);
        }
    }

    private byte[] buildPdfBytes(ContractAgreement contractAgreement) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 48, 48, 56, 56);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(resolveBaseFont(), 18, Font.BOLD);
            Font headingFont = new Font(resolveBaseFont(), 12, Font.BOLD);
            Font bodyFont = new Font(resolveBaseFont(), 11, Font.NORMAL);

            Paragraph eyebrow = new Paragraph("Apartment Booking Service", headingFont);
            eyebrow.setSpacingAfter(8);
            document.add(eyebrow);

            Paragraph title = new Paragraph("Hop dong xac nhan giao dich can ho dien tu", titleFont);
            title.setSpacingAfter(8);
            document.add(title);

            document.add(new Paragraph("Ma hop dong: " + contractAgreement.getContractCode(), bodyFont));
            document.add(new Paragraph("Trang thai: " + contractAgreement.getStatus().name(), bodyFont));
            document.add(Chunk.NEWLINE);

            PdfPTable infoTable = new PdfPTable(new float[] { 2.2f, 5.8f });
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(16);
            addRow(infoTable, "Ben A", safe(contractAgreement.getUserPartyNameSnapshot()), bodyFont);
            addRow(infoTable, "Ben B", safe(contractAgreement.getAgentPartyNameSnapshot()), bodyFont);
            addRow(infoTable, "Ten can ho", safe(contractAgreement.getApartmentTitleSnapshot()), bodyFont);
            addRow(infoTable, "Dia chi", safe(contractAgreement.getApartmentAddressSnapshot()), bodyFont);
            addRow(infoTable, "Hinh thuc", safe(contractAgreement.getTransactionTypeSnapshot()), bodyFont);
            addRow(infoTable, "Gia tri tham chieu", safe(contractAgreement.getPriceSnapshot()), bodyFont);
            addRow(infoTable, "Ngay xem can ho", safe(contractAgreement.getViewingDateSnapshot()), bodyFont);
            addRow(infoTable, "Ben A ky luc", formatDateTime(contractAgreement.getUserSignedAt()), bodyFont);
            addRow(infoTable, "Ben B ky luc", formatDateTime(contractAgreement.getAgentSignedAt()), bodyFont);
            document.add(infoTable);

            Paragraph termsHeading = new Paragraph("Dieu khoan xac nhan", headingFont);
            termsHeading.setSpacingAfter(8);
            document.add(termsHeading);

            Paragraph terms = new Paragraph(
                    "Hai ben xac nhan thong tin giao dich neu tren la chinh xac tai thoi diem ky. "
                            + "Hop dong nay duoc khoa cung noi dung sau khi du chu ky hai ben va co gia tri doi chieu phap ly.",
                    bodyFont);
            terms.setLeading(18);
            terms.setSpacingAfter(18);
            document.add(terms);

            PdfPTable signaturesTable = new PdfPTable(2);
            signaturesTable.setWidthPercentage(100);
            signaturesTable.setSpacingBefore(8);
            signaturesTable.setSpacingAfter(8);
            signaturesTable.addCell(buildSignatureCell(
                    "Ben A - Nguoi dat",
                    contractAgreement.getUserSignerName(),
                    contractAgreement.getUserSignedAt() == null ? null : formatDateTime(contractAgreement.getUserSignedAt()),
                    contractAgreement.getUserSignatureDataUrl(),
                    bodyFont,
                    headingFont));
            signaturesTable.addCell(buildSignatureCell(
                    "Ben B - Dai dien tu van",
                    contractAgreement.getAgentSignerName(),
                    contractAgreement.getAgentSignedAt() == null ? null : formatDateTime(contractAgreement.getAgentSignedAt()),
                    contractAgreement.getAgentSignatureDataUrl(),
                    bodyFont,
                    headingFont));
            document.add(signaturesTable);

            document.close();
            return outputStream.toByteArray();
        } catch (IOException | DocumentException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot generate contract PDF", ex);
        }
    }

    private PdfPCell buildSignatureCell(
            String title,
            String signerName,
            String signedAt,
            String signatureDataUrl,
            Font bodyFont,
            Font headingFont) throws IOException, DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorder(Rectangle.BOX);

        Paragraph titleParagraph = new Paragraph(title, headingFont);
        titleParagraph.setSpacingAfter(6);
        cell.addElement(titleParagraph);
        cell.addElement(new Paragraph("Nguoi ky: " + safe(signerName), bodyFont));
        cell.addElement(new Paragraph("Thoi diem ky: " + safe(signedAt), bodyFont));
        cell.addElement(Chunk.NEWLINE);

        if (signatureDataUrl != null && signatureDataUrl.startsWith("data:image")) {
            Image signature = Image.getInstance(decodeDataUrl(signatureDataUrl));
            signature.scaleToFit(180, 70);
            cell.addElement(signature);
        } else {
            cell.addElement(new Paragraph("Chua co chu ky", bodyFont));
        }
        return cell;
    }

    private void addRow(PdfPTable table, String label, String value, Font bodyFont) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, bodyFont));
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.BOTTOM);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, bodyFont));
        valueCell.setPadding(8);
        valueCell.setBorder(Rectangle.BOTTOM);
        table.addCell(valueCell);
    }

    private BaseFont resolveBaseFont() throws DocumentException, IOException {
        String windowsDirectory = System.getenv("WINDIR");
        if (windowsDirectory != null && !windowsDirectory.isBlank()) {
            Path arialPath = Paths.get(windowsDirectory, "Fonts", "arial.ttf");
            if (Files.exists(arialPath)) {
                return BaseFont.createFont(arialPath.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private byte[] decodeDataUrl(String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature payload");
        }
        return Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot hash contract PDF", ex);
        }
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMAT.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
