package ca.dollareh.integration;

import ca.dollareh.core.model.Product;
import ca.dollareh.vendor.ProductSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Shopify {

    final Logger logger = LoggerFactory.getLogger(Shopify.class);

    private final String baseUrl;

    private final Path exportPath;

    private final ObjectMapper objectMapper;

    private final ProductSource productSource;

    public Shopify(ProductSource productSource) {
        this.productSource = productSource;

        baseUrl = System.getenv("SHOPIFY_BASE_URL");

        exportPath = Path.of("workspace/export/" + getClass().getSimpleName() + "/" + productSource.getClass().getSimpleName());

        exportPath.toFile().mkdirs();

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void export() throws IOException {

        Map<Long, Map<String, Object>> collectionsMap =  getShopifyCollection();

        collectionsMap.entrySet().stream().filter(longMapEntry -> longMapEntry.getKey() == 329128738986L)
                .parallel().forEach(longMapEntry -> {

            System.out.println(getMetafields(longMapEntry.getKey()));

        });

        // this.productSource.enrich();

        File propertiesFile = new File(exportPath.toFile(), "product-mapping.properties");

        Properties properties = new Properties();

        if (propertiesFile.exists()) {
            properties.load(new FileReader(propertiesFile));
        }


        Path enrichmentPath = Path.of("workspace/enrichment/" + productSource.getClass().getSimpleName());

        List<File> enrichedJsonFiles = List.of(enrichmentPath.toFile().listFiles()[0]);

        for (File enrichedJsonFile : enrichedJsonFiles) {
            try {
                Product enrichedProduct = objectMapper
                        .readValue(enrichedJsonFile, Product.class);

                Map<String, Object> shopifyProduct = getShopifyProduct(enrichedProduct);

                File shopifyProductFile = new File(exportPath.toFile(), enrichedProduct.code() + ".json");

                if (shopifyProductFile.exists()) {

                    JsonNode existingShoppifyProduct = objectMapper.readTree(shopifyProductFile);

                    JsonNode newShoppifyProduct = objectMapper.readTree(objectMapper.writeValueAsString(shopifyProduct));

                    if (!existingShoppifyProduct.equals(newShoppifyProduct)) {
                        String shopifyProductId = (String) properties.get(enrichedProduct.code());
                        update(shopifyProductId, shopifyProduct);
                    }


                } else {
                    Map<String, Object> createdProduct = create(shopifyProduct);
                    Long id = (Long) ((Map<String, Object>) createdProduct.get("product")).get("id");
                    properties.put(enrichedProduct.code(), id.toString());

                    createImages(id, enrichedProduct);
                }

                objectMapper.writeValue(shopifyProductFile, shopifyProduct);

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        properties.store(new FileOutputStream(propertiesFile), "Updated");

    }

    public Map<String, Object> getShopifyProduct(final Product product) {
        Map<String, Object> shopifyProduct = new HashMap<>(1);

        Map<String, Object> variantMap
                = Map.of("price", product.price(),
                "compare_at_price", product.discount(),
                "inventory_quantity", product.inventryQuantity(),
                "title", product.title(),
                "inventory_policy", "deny" ,
                "inventory_management","shopify",
                "option1" ,product.title(),
                "fulfillment_service", "manual",
                "taxable", true,
                "requires_shipping", true);

        Map<String, Object> productMap
                = Map.of("title", product.title(),
                "body_html", product.description(),
                "handle", product.code(),
                "vendor" , "Dollareh",
                "variants", List.of(variantMap));

        shopifyProduct.put("product", productMap);

        return shopifyProduct;
    }

    public Map<String, Object> getShopifyCollectionAssociation(final Long productId, Properties collectionProps) {
        Map<String, Object> shopifyCollection = new HashMap<>(2);

        shopifyCollection.put("product_id", productId);

        String collectionId = (String) collectionProps.get("shopify-id");
        if (collectionId != null) {
            shopifyCollection.put("collection_id", Long
                    .parseLong(collectionId.trim()));
        }

        return Map.of("collect", shopifyCollection);
    }

    public void createImages(final Long productId, Product product) throws IOException, InterruptedException {

        for (String imageUrl : product.imageUrls()) {
            File imageFile = productSource.downloadAsset(imageUrl);

            createImage(productId, imageFile.toPath());
        }

    }


    public Map<String, Object> create(Map<String, Object> shopifyProduct) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products.json"))
                .header("X-Shopify-Access-Token", System.getenv("SHOPIFY_ACCESS_TOKEN"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(shopifyProduct)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body()
                , new TypeReference<>() {
                });
    }

    /**
     * Gets all the shopify collections as Map.
     * Key os the Map is Collection Id
     * Value of the Map is Collection Object as Map<String, Object>
     * @return collectionsMap
     */
    private Map<Long, Map<String, Object>> getShopifyCollection() {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl  + "/custom_collections.json"))
                .header("X-Shopify-Access-Token", System.getenv("SHOPIFY_ACCESS_TOKEN"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch Shopify collections: " + response.body());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, List<Map<String, Object>>> responseBody = objectMapper.readValue(response.body(),
                    new TypeReference<>() {});

            Map<Long, Map<String, Object>> collectionsMap = new HashMap<>();
            List<Map<String, Object>> collections = responseBody.get("custom_collections");

            if (collections != null) {
                for (Map<String, Object> collection : collections) {
                    Long id = ((Number) collection.get("id")).longValue();
                    collectionsMap.put(id, collection);
                }
            }

            return collectionsMap;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching Shopify collections", e);
        }
    }

    private List<Map<String, Object>> getMetafields(Long collectionId) {
        HttpClient client = HttpClient.newHttpClient();

        String metafieldsUrl = baseUrl + "/collections/" + collectionId + "/metafields.json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metafieldsUrl))
                .header("X-Shopify-Access-Token", System.getenv("SHOPIFY_ACCESS_TOKEN"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch metafields for collection " + collectionId);
            }

            Map<String, List<Map<String, Object>>> responseBody = objectMapper.readValue(response.body(),
                    new TypeReference<>() {
                    });

            return responseBody.getOrDefault("metafields", Collections.emptyList());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching metafields for collection " + collectionId, e);
        }
    }


    public Map<String, Object> update(String productId, Map<String, Object> shopifyProduct) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/" + productId + ".json"))
                .header("X-Shopify-Access-Token", System.getenv("SHOPIFY_ACCESS_TOKEN"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(shopifyProduct)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body()
                , new TypeReference<>() {
                });
    }

    private void createImage(Long productId, Path productImage) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        // Convert Image to Base64
        String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(productImage));

        // Create JSON request body
        String requestBody = """
        {
          "image": {
            "attachment": "%s"
          }
        }
        """.formatted(base64Image);

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/" + productId + "/images.json"))
                .header("X-Shopify-Access-Token", System.getenv("SHOPIFY_ACCESS_TOKEN"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send request and get response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
