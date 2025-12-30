package com.juxa.legal_advice.service;

import com.juxa.legal_advice.model.DiagnosisResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.FontFactory;
import java.io.ByteArrayOutputStream;


@Service
public class PdfService {

    public byte[] generateDiagnosisPdf(DiagnosisResponse response) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Título
            document.add(new Paragraph("Estrategia Maestra", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph(" "));

            // Resumen
            document.add(new Paragraph("Resumen:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(response.getSummary()));
            document.add(new Paragraph(" "));

            // Pasos sugeridos
            document.add(new Paragraph("Pasos sugeridos:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            for (String step : response.getSteps()) {
                document.add(new Paragraph("• " + step));
            }
            document.add(new Paragraph(" "));

            // Nivel de riesgo
            document.add(new Paragraph("Nivel de riesgo: " + response.getRiskLevel()));

            // Nota del asesor
            document.add(new Paragraph("Nota del asesor: " + response.getAdvisorNote()));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}