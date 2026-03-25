package com.juxa.legal_advice.service;

import com.google.cloud.discoveryengine.v1beta.SearchServiceClient;
import com.google.cloud.discoveryengine.v1beta.SearchRequest;
import com.google.cloud.discoveryengine.v1beta.SearchResponse;
import com.google.cloud.discoveryengine.v1beta.SearchServiceSettings;
import com.google.cloud.spring.core.DefaultCredentialsProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.stream.LangCollectors.collect;

@Service
public class VertexSearchService {

    @Value("${gcp.project-id}") private String projectId;
    @Value("${gcp.location}") private String location;
    @Value("${gcp.data-store-id}") private String dataStoreId;

    private SearchServiceClient client;
    private final Map<String, String> cacheConsultas = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            SearchServiceSettings settings = SearchServiceSettings.newBuilder()
                    .setEndpoint("us-discoveryengine.googleapis.com:443")
                    .build();
            this.client = SearchServiceClient.create(settings);
            System.out.println("--- [SISTEMA] Cliente de Vertex Search inicializado (v1beta) ---");
        } catch (Exception e) {
            System.err.println("--- [ERROR] No se pudo inicializar Vertex Search: " + e.getMessage());

        }
    }

    public String searchLegalKnowledge(String query) {
        String queryKey = query.trim().toLowerCase();
        if (cacheConsultas.containsKey(queryKey)) {
            System.out.println("--- [CACHE] Reutilizando resultado para: " + queryKey);
            return cacheConsultas.get(queryKey);
        }

        System.out.println("--- [VERTEX] Buscando fundamentos para: " + query);

        try {
            // Configuramos la petición con Snippets y un tamaño de página de 5
            SearchRequest request = SearchRequest.newBuilder()
                    .setServingConfig(String.format(
                            "projects/%s/locations/%s/dataStores/%s/servingConfigs/default_search",
                            projectId, location, dataStoreId))
                    .setQuery(query)
                    .setPageSize(15)
                    .setContentSearchSpec(SearchRequest.ContentSearchSpec.newBuilder()
                            .setSnippetSpec(SearchRequest.ContentSearchSpec.SnippetSpec.newBuilder()
                                    .setReturnSnippet(true) // Crucial para obtener el texto real
                                    .build())
                            .build())
                    .build();

            // Ejecutamos la búsqueda
            SearchResponse response = client.search(request).getPage().getResponse();

            if (response.getResultsCount() == 0) {
                System.out.println("--- [AVISO] Vertex no encontró fragmentos relevantes. ---");
                return "No se encontraron fundamentos técnicos específicos en la base de datos.";
            }

            // Procesamos los resultados de la PRIMERA PÁGINA únicamente
            String contextoLegal = StreamSupport.stream(response.getResultsList().spliterator(), false)
                    .map(result -> {
                        var doc = result.getDocument();
                        var structFields = doc.getStructData().getFieldsMap();
                        var derivedFields = doc.getDerivedStructData().getFieldsMap();

                        String contenidoFinal = "Texto no disponible";

                        // PRIORIDAD 1: El contenido original del JSON (porque ya vimos que tus archivos tienen la llave 'contenido')
                        if (structFields.containsKey("contenido")) {
                            contenidoFinal = structFields.get("contenido").getStringValue();
                        }
                        // PRIORIDAD 2: El artículo (para darle contexto a Gemini sobre qué número de artículo es)
                        String numeroArticulo = "";
                        if (structFields.containsKey("articulo")) {
                            numeroArticulo = "Art. " + structFields.get("articulo").getStringValue() + ": ";
                        }

                        // PRIORIDAD 3: Snippets (como respaldo si lo anterior fallara)
                        if (contenidoFinal.equals("Texto no disponible") && derivedFields.containsKey("snippets")) {
                            var values = derivedFields.get("snippets").getListValue().getValuesList();
                            if (!values.isEmpty()) {
                                contenidoFinal = values.get(0).getStructValue().getFieldsMap().get("snippet").getStringValue();
                            }
                        }

                        System.out.println("--- [ÉXITO] Contenido recuperado de: " + doc.getName());
                        System.out.println("--- [TEXTO] " + (contenidoFinal.length() > 100 ? contenidoFinal.substring(0, 100) : contenidoFinal));

                        return String.format("### FUENTE: %s\n### CONTENIDO: %s%s", doc.getName(), numeroArticulo, contenidoFinal);
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));

// Guardamos en cache y retornamos
            cacheConsultas.put(queryKey, contextoLegal);
            return contextoLegal;

        } catch (Exception e) {
            System.err.println("--- [ERROR CRÍTICO] Error en comunicación con Vertex: " + e.getMessage());
            return "Error al recuperar información legal.";
        }
    }

    @PreDestroy
    public void cleanup() {
        if (client != null) {
            client.close();
        }
    }
}