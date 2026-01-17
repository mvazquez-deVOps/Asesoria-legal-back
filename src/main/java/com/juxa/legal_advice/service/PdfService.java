package com.juxa.legal_advice.service;

import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.model.SubscriptionPlan;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.awt.Color;

@Service
public class PdfService {

    public byte[] generateDiagnosisPdf(DiagnosisResponse response, SubscriptionPlan plan) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // FUENTES
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLUE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            // ENCABEZADO
            String headerText = (plan == SubscriptionPlan.BUSINESS_PLAN)
                    ? "DIAGNÓSTICO LEGAL- JUXA" : "PLAN DE ACCIÓN JURÍDICA - JUXA";

            document.add(new Paragraph(headerText, titleFont));
            document.add(new Paragraph("Referencia: #" + response.getDiagnosisId(), bodyFont));
            document.add(new Paragraph("Consultante: " + response.getClientName(), bodyFont));
            document.add(new Paragraph("-----------------------------------------------------------"));

            // CONTENIDO (Ahora vendrá limpio de JSON gracias al Service)
            document.add(new Paragraph("\nDICTAMEN ESTRATÉGICO:", sectionFont));
            document.add(new Paragraph(response.getSummary(), bodyFont));

            document.add(new Paragraph("\nNIVEL DE RIESGO: " + response.getRiskLevel(), sectionFont));
            document.add(new Paragraph("\nNOTA DEL ASESOR:", sectionFont));
            document.add(new Paragraph(response.getAdvisorNote(), bodyFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF", e);
        }
    }
}