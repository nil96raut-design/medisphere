package com.healthtrack.service;

import com.healthtrack.entity.LabTestOrder;
import com.healthtrack.repository.LabTestOrderRepository;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class LabReportService {

    private final LabTestOrderRepository labTestOrderRepository;
    private final TenantValidator tenantValidator;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateReport(Long orderId, UserPrincipal currentUser) {
        LabTestOrder order = labTestOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found"));
        tenantValidator.validateHospitalAccess(order.getHospital().getId(), currentUser.getHospitalId());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        document.add(new Paragraph("HealthTrack \u2014 Lab Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Order ID: " + order.getId()));
        document.add(new Paragraph("Patient: " + order.getPatient().getFirstName() + " " + order.getPatient().getLastName()));
        document.add(new Paragraph("Test: " + order.getTestName()));
        document.add(new Paragraph("Requested by: " + order.getRequestedBy().getFullName()));
        document.add(new Paragraph("Status: " + order.getStatus().name()));
        document.add(new Paragraph(" "));

        if (order.getResultValues() != null) {
            document.add(new Paragraph("Results:"));
            document.add(new Paragraph(order.getResultValues()));
        }

        if (order.getTechnicianNotes() != null) {
            document.add(new Paragraph("Technician Notes:"));
            document.add(new Paragraph(order.getTechnicianNotes()));
        }

        if (order.getCompletedAt() != null) {
            document.add(new Paragraph("Completed: " + order.getCompletedAt().toString()));
        }

        document.close();

        byte[] pdf = baos.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "lab-report-" + orderId + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
