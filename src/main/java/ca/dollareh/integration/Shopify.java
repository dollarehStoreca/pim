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
import java.nio.file.Path;
import java.util.ArrayList;
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

        this.productSource.enrich();

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
                "inventory_quantity", product.inventryQuantity());

        List<Map<String, Object>> imagesList = new ArrayList<>(product.imageUrls().length);

        for (int i = 0; i < product.imageUrls().length; i++) {
            imagesList.add(Map.of("position", i + 1,
                    "alt", product.title(),
                    "src", product.imageUrls()[i]));
        }

        Map<String, Object> productMap
                = Map.of("title", product.title(),
                "body_html", product.description(),
                "handle", product.code(),
                "variants", List.of(variantMap),
                "image", imagesList.get(0),
                "images", imagesList);

        shopifyProduct.put("product", productMap);

        return shopifyProduct;
    }

    public Map<String, Object> getShopifyCollections(final Long productId, Properties collectionProps) {
        Map<String, Object> shopifyCollection = new HashMap<>(2);

        shopifyCollection.put("product_id", productId);

        String collectionId = (String) collectionProps.get("shopify-id");
        if (collectionId != null) {
            shopifyCollection.put("collection_id", Long
                    .parseLong(collectionId.trim()));
        }

        return Map.of("collect", shopifyCollection);
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

}
