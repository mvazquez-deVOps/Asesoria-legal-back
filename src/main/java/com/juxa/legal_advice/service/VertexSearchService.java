package com.juxa.legal_advice.service;

import com.google.cloud.discoveryengine.v1beta.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

            // Unimos los fragmentos encontrados en un solo texto de contexto
            return StreamSupport.stream(response.iterateAll().spliterator(), false)
                    .map(result -> {
                        // Navegamos la estructura de datos para obtener el texto del fragmento
                        var fields = result.getDocument().getDerivedStructData().getFieldsMap();
                        if (fields.containsKey("snippets")) {
                            return fields.get("snippets").getListValue().getValues(0)
                                    .getStructValue().getFieldsMap().get("snippet").getStringValue();
                        }
                        return "";
                    })
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            System.err.println("Error en búsqueda semántica: " + e.getMessage());
            return "No se encontraron fundamentos técnicos específicos.";
        }
    }
}