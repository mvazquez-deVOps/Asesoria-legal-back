package com.juxa.legal_advice.service;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiBucketService {
    private final Storage storage = StorageOptions.getDefaultInstance().getService();
    private final String bucketName = "asesoria-legal-bucket";

    public String readTextFile(String fileName) {
        Blob blob = storage.get(bucketName, fileName);
        return (blob != null) ? new String(blob.getContent(), StandardCharsets.UTF_8) : "";
    }

    public String readPdfFile(String fileName) {
        Blob blob = storage.get(bucketName, fileName);
        if (blob == null) return "";
        try (PDDocument document = PDDocument.load(blob.getContent())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            return "Error técnico al leer el PDF legal" + e.getMessage();
        }
    }

    public List<String> listKnowledgeDocuments() {
        List<String> files = new ArrayList<>();
        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            String name = blob.getName();
            // Evitamos procesar la hoja de ruta como documento de consulta técnica
            if (!name.contains("HojadeRita")) {
                files.add(name);
            }
        }
        return files;
    }
}
