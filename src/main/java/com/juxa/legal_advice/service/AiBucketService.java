package com.juxa.legal_advice.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiBucketService {
    private final Storage storage = StorageOptions.getDefaultInstance().getService();
    private final String bucketName = "asesoria-legal-bucket";

    // CACHÉ: Evita latencia y costos de lectura repetitiva en Google Cloud
    private final Map<String, String> contentCache = new ConcurrentHashMap<>();

    public String readTextFile(String fileName) {
        // Si ya lo leímos antes, lo entregamos de inmediato
        if (contentCache.containsKey(fileName)) {
            return contentCache.get(fileName);
        }

        try {
            Blob blob = storage.get(bucketName, fileName);
            if (blob == null) return "";

            String content = new String(blob.getContent(), StandardCharsets.UTF_8);
            contentCache.put(fileName, content); // Guardamos en memoria
            return content;
        } catch (Exception e) {
            System.err.println("Error en Bucket (Text): " + e.getMessage());
            return "";
        }
    }

    public String readPdfFile(String fileName) {
        // Los PDFs también se cachean para ahorrar procesamiento de PDFBox
        if (contentCache.containsKey(fileName)) {
            return contentCache.get(fileName);
        }

        Blob blob = storage.get(bucketName, fileName);
        if (blob == null) return "";

        try (PDDocument document = PDDocument.load(blob.getContent())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            contentCache.put(fileName, text);
            return text;
        } catch (Exception e) {
            return "Error técnico al procesar el documento jurídico: " + e.getMessage();
        }
    }

    /**
     * Lista solo los documentos de la base de conocimientos,
     * ignorando archivos de configuración y carpetas de sistema.
     */
    public List<String> listKnowledgeDocuments() {
        List<String> files = new ArrayList<>();
        try {
            Page<Blob> blobs = storage.list(bucketName);
            for (Blob blob : blobs.iterateAll()) {
                String name = blob.getName();

                // Filtros de exclusión
                if (name.contains("Hoja_deRita") || name.startsWith("logs/") || blob.isDirectory()) {
                    continue;
                }
                files.add(name);
            }
        } catch (Exception e) {
            System.err.println("Error listando documentos: " + e.getMessage());
        }
        return files;
    }

    /**
     * Método específico para listar las plantillas de la carpeta FORMATOS
     * Esto ayudará a JUXA a saber qué opciones tiene para redactar.
     */
    public List<String> listAvailableFormats() {
        List<String> formats = new ArrayList<>();
        try {
            // Asumimos que tus formatos están en una carpeta virtual llamada "FORMATOS/"
            Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix("FORMATOS/"));
            for (Blob blob : blobs.iterateAll()) {
                if (!blob.getName().equals("FORMATOS/") && !blob.isDirectory()) {
                    formats.add(blob.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("Error listando formatos: " + e.getMessage());
        }
        return formats;
    }

    // Método para limpiar la caché si necesitas actualizar un archivo sin reiniciar el servidor
    public void clearCache() {
        this.contentCache.clear();
    }
}