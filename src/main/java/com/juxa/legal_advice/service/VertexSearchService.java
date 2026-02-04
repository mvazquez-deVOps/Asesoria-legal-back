package com.juxa.legal_advice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.google.cloud.discoveryengine.v1beta.SearchServiceClient;
import com.google.cloud.discoveryengine.v1beta.SearchRequest;


@Service
public class VertexSearchService {

    @Value("${gcp.project-id}") private String projectId;
    @Value("${gcp.location}") private String location;
    @Value("${gcp.data-store-id}") private String dataStoreId;

    public String searchLegalKnowledge(String query) {
        try (SearchServiceClient client = SearchServiceClient.create()) {
            SearchRequest request = SearchRequest.newBuilder()
                    .setServingConfig(String.format("projects/%s/locations/%s/dataStores/%s/servingConfigs/default_search",
                            projectId, location, dataStoreId))
                    .setQuery(query)
                    .setPageSize(3) // Solo traemos los 3 fragmentos más relevantes
                    .build();

            SearchServiceClient.SearchPagedResponse response = client.search(request);

            return StreamSupport.stream(response.iterateAll().spliterator(), false)
                    .map(result -> {
                        // Accedemos al objeto Document de Discovery Engine
                        com.google.cloud.discoveryengine.v1beta.Document doc = result.getDocument();

                        // 1. Obtener la URI o Link de los metadatos estructurados
                        // Discovery Engine guarda la URL original en 'content' o 'derivedStructData'
                        String sourceLink = "";

                        // Intentamos obtener la URI desde los datos derivados (donde Vertex guarda el link de GCS)
                        var derivedFields = doc.getDerivedStructData().getFieldsMap();
                        if (derivedFields.containsKey("link")) {
                            sourceLink = derivedFields.get("link").getStringValue();
                        } else if (doc.hasContent()) {
                            // Si no está en link, revisamos el campo content (URI de GCS o Web)
                            sourceLink = doc.getContent().getUri();
                        }

                        // Si aún es vacío, usamos el nombre único del recurso en GCP como identificador
                        if (sourceLink == null || sourceLink.isEmpty()) {
                            sourceLink = doc.getName();
                        }

                        // 2. Extraer el fragmento de texto (Snippet)
                        String snippet = "";
                        if (derivedFields.containsKey("snippets")) {
                            snippet = derivedFields.get("snippets").getListValue().getValues(0)
                                    .getStructValue().getFieldsMap().get("snippet").getStringValue();
                        }

                        // Formateamos para que Gemini identifique claramente la fuente y el texto
                        return String.format("[Fuente Documental: %s]\n%s", sourceLink, snippet);
                    })
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            // Es vital capturar el error para que el asistente pueda seguir respondiendo con su conocimiento base
            System.err.println("Error en búsqueda semántica: " + e.getMessage());
            return "No se encontraron fundamentos técnicos externos, procede con tu base de datos interna.";
        }
    }
}