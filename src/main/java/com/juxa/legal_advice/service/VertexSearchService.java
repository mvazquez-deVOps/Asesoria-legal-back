package com.juxa.legal_advice.service;

import com.google.cloud.discoveryengine.v1beta.SearchServiceSettings;
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
        System.out.println("--- [DEBUG JUXA] Iniciando búsqueda en Vertex Search AI ---");
        System.out.println("--- [DEBUG JUXA] Query: " + query);

        try {
            // 1. Definimos la configuración regional
            SearchServiceSettings settings = SearchServiceSettings.newBuilder()
                    .setEndpoint("us-discoveryengine.googleapis.com:443")
                    .build();

            // 2. PASAMOS LOS SETTINGS AL CLIENTE (Paso crucial para evitar el error INVALID_ARGUMENT)
            try (SearchServiceClient client = SearchServiceClient.create(settings)) {

                SearchRequest request = SearchRequest.newBuilder()
                        .setServingConfig(String.format("projects/%s/locations/%s/dataStores/%s/servingConfigs/default_search",
                                projectId, location, dataStoreId))
                        .setQuery(query)
                        .setPageSize(3)
                        .build();

                SearchServiceClient.SearchPagedResponse response = client.search(request);

                if (!response.iterateAll().iterator().hasNext()) {
                    System.out.println("--- [DEBUG JUXA] Vertex Search NO encontró resultados para esta consulta.");
                    return "No se encontraron fundamentos técnicos";
                }

                System.out.println("--- [DEBUG JUXA] Resultados encontrados exitosamente.");

                return StreamSupport.stream(response.iterateAll().spliterator(), false)
                        .map(result -> {
                            com.google.cloud.discoveryengine.v1beta.Document doc = result.getDocument();
                            String sourceLink = "";
                            var derivedFields = doc.getDerivedStructData().getFieldsMap();

                            if (derivedFields.containsKey("link")) {
                                sourceLink = derivedFields.get("link").getStringValue();
                            } else if (doc.hasContent()) {
                                sourceLink = doc.getContent().getUri();
                            }

                            if (sourceLink == null || sourceLink.isEmpty()) {
                                sourceLink = doc.getName();
                            }

                            String snippet = "";
                            if (derivedFields.containsKey("snippets")) {
                                snippet = derivedFields.get("snippets").getListValue().getValues(0)
                                        .getStructValue().getFieldsMap().get("snippet").getStringValue();
                            }

                            return String.format("### FUENTE: %s\n### CONTENIDO RECUPERADO: %s", sourceLink, snippet);
                        })
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("\n\n---\n\n"));
            }
        } catch (Exception e) {
            System.err.println("--- [ERROR CRÍTICO VERTEX] ---");
            System.err.println("Mensaje: " + e.getMessage());
            e.printStackTrace();
            return "No se encontraron fundamentos técnicos";
        }
    }
}