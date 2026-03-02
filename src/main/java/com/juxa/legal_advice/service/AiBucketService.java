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

    // Inicialización explícita con projectId para evitar problemas con credenciales implícitas
    private final Storage storage = StorageOptions.newBuilder()
            .setProjectId("asesoria-legal-juxa-83a12")
            .build()
            .getService();

    private final String bucketName = "asesoria-legal-bucket";

    // CACHÉ: evita latencia y costos de lectura repetitiva en Google Cloud
    private final Map<String, String> contentCache = new ConcurrentHashMap<>();

    public String readTextFile(String fileName) {
        if (contentCache.containsKey(fileName)) {
            return contentCache.get(fileName);
        }

        try {
            Blob blob = storage.get(bucketName, fileName);
            if (blob == null) {
                System.err.println("Archivo no encontrado en bucket: " + fileName);
                return "";
            }

            String content = new String(blob.getContent(), StandardCharsets.UTF_8);
            contentCache.put(fileName, content);
            return content;
        } catch (Exception e) {
            System.err.println("Error en Bucket (Text): " + e.getMessage());
            return "";
        }
    }

    public String readPdfFile(String fileName) {
        if (contentCache.containsKey(fileName)) {
            return contentCache.get(fileName);
        }

        Blob blob = storage.get(bucketName, fileName);
        if (blob == null) {
            System.err.println("Archivo PDF no encontrado en bucket: " + fileName);
            return "";
        }

        try (PDDocument document = PDDocument.load(blob.getContent())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            contentCache.put(fileName, text);
            return text;
        } catch (Exception e) {
            System.err.println("Error técnico al procesar PDF: " + e.getMessage());
            return "";
        }
    }

    /**
     * Lista los documentos de la base de conocimientos,
     * ignorando carpetas de sistema y logs, pero ya no excluye "Hoja_deRita".
     */
    public List<String> listKnowledgeDocuments() {
        List<String> files = new ArrayList<>();
        try {
            Page<Blob> blobs = storage.list(bucketName);
            for (Blob blob : blobs.iterateAll()) {
                String name = blob.getName();

                // Filtros de exclusión corregidos
                if (name.startsWith("logs/") || blob.isDirectory()) {
                    continue;
                }

                files.add(name);
                System.out.println("Documento encontrado: " + name); // Log de depuración
            }
        } catch (Exception e) {
            System.err.println("Error listando documentos: " + e.getMessage());
        }
        return files;
    }

    public List<String> listStateCodes() {
        List<String> estados = new ArrayList<>();
        try {
            Page<Blob> blobs = storage.list(
                    bucketName,
                    Storage.BlobListOption.prefix("Códigos_Civiles_Penales_Procedimientos/"),
                    Storage.BlobListOption.currentDirectory()
            );

            for (Blob blob : blobs.iterateAll()) {
                if (blob.isDirectory()) {
                    String nombreEstado = blob.getName()
                            .replace("Códigos_Civiles_Penales_Procedimientos/", "")
                            .replace("/", "")
                            .replace("_", " ");
                    estados.add(nombreEstado);
                    System.out.println("Estado encontrado: " + nombreEstado);
                }
            }
        } catch (Exception e) {
            System.err.println("Error listando subcarpetas estatales: " + e.getMessage());
        }
        return estados;
    }


    /**
     * Lista las plantillas de la carpeta FORMATOS
     */
    public List<String> listAvailableFormats() {
        List<String> formats = new ArrayList<>();
        try {
            Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix("FORMATOS/"));
            for (Blob blob : blobs.iterateAll()) {
                if (!blob.getName().equals("FORMATOS/") && !blob.isDirectory()) {
                    formats.add(blob.getName());
                    System.out.println("Formato encontrado: " + blob.getName()); // Log de depuración
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