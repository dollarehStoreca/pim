package ca.dollareh.pim.integration;

import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.source.ProductSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.net.URIBuilder;
import org.jsoup.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;

public class Shopify {

    final Logger logger = LoggerFactory.getLogger(Shopify.class);

    private final String baseUrl;
    private final String accessToken;

    private final Path exportPath;

    private final ProductSource productSource;
    private final Long defaultCollectionId;

    private final ObjectMapper objectMapper;

    public Shopify(ProductSource productSource) {
        this.productSource = productSource;

        baseUrl = "https://" + System.getenv("SHOPIFY_STORE_URL")  + "/admin/api/2025-01";
        accessToken = System.getenv("SHOPIFY_ACCESS_TOKEN");

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        exportPath = Path.of("workspace/export/" + getClass().getSimpleName() + "/" + productSource.getClass().getSimpleName());

        exportPath.toFile().mkdirs();

        File collectionMappingsFile = new File(exportPath.toFile().getParentFile(), "collection.properties");

        if (collectionMappingsFile.exists()) {
            Properties cProperties = new Properties();
            try {
                cProperties.load(new FileReader(collectionMappingsFile));
            } catch (IOException e) {
                logger.error("Collection Mapping Properties does not exists {}",collectionMappingsFile);
            }
            defaultCollectionId = Long.parseLong((String) cProperties.get(productSource.getClass().getSimpleName()));
        } else {
            defaultCollectionId = null;
        }
    }

    public void export() throws IOException, InterruptedException {
        Product product = new Product("Sample", "Sample 3" , "Sample", 1L,
                1,1.0f,1.0f,null);

        File productJsonFile = getProductFile(product);

        if(productJsonFile.exists()) {
            update(getProductId(productJsonFile), product);
        } else {
            create(product);
        }
    }

    private void create(final Product product) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = getShopifyRequestBuilder("/products.json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(getShopifyProduct(product))))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if ( response.statusCode() == SC_CREATED) {

            File productJsonFile = getProductFile(product);

            Files.write(productJsonFile.toPath(), response.body(), StandardOpenOption.CREATE);

            Long productId = getProductId(productJsonFile);
            associateCollection(productId, defaultCollectionId);

        } else {
            logger.error("Product {} not created", product.code());
        }
    }

    public void update(final Long productId, final Product product) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = getShopifyRequestBuilder("/products/" + productId + ".json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(getShopifyProduct(product))))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if ( response.statusCode() == HttpStatus.SC_OK) {
            Files.write(getProductFile(product).toPath(), response.body());
        } else {
            logger.error("Product {} not updated", product.code());
        }
    }

    private File getProductFile(final Product product) {
        return new File(exportPath.toFile(), product.code() + ".json");
    }

    private Map<String, Object> getShopifyProduct(final Product product) {
        Map<String, Object> shopifyProduct = new HashMap<>(1);

        Map<String, Object> variantMap
                = Map.of("price", product.price(),
                "compare_at_price", product.discount(),
                "inventory_quantity", product.inventryQuantity(),
                "title", "Default Title",
                "inventory_policy", "deny" ,
                "inventory_management","shopify",
                "option1" ,"Default Title",
                "fulfillment_service", "manual",
                "taxable", true,
                "requires_shipping", true);

        Map<String, Object> productMap
                = Map.of("title", product.title(),
                "body_html", product.description(),
                "handle", product.code(),
                "vendor" , "Dollareh",
                "tags" , "auto-imported",
                "variants", List.of(variantMap));

        shopifyProduct.put("product", productMap);

        return shopifyProduct;
    }

    public void associateCollection(final Long productId, final Long collectionId) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        // Build HTTP request
        HttpRequest request = getShopifyRequestBuilder("/collects.json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(
                            Map.of("collect", Map.of("product_id", productId,
                            "collection_id",collectionId)))))
                .build();

        // Send request and get response
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == SC_CREATED) {
            logger.info("Product {} asociated to Collection {}", productId, collectionId);
        } else {
            logger.info("Product {} can not be asociated to Collection {}", productId, collectionId);
        }

    }

    private Long getProductId(final File productJsonFile) throws IOException {
        Long productId = null;
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(productJsonFile)) {
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == JsonToken.FIELD_NAME && "id".equals(parser.currentName())) {
                    parser.nextToken();
                    productId = parser.getLongValue();
                    break; // Exit early after finding the required field
                }
            }
        }
        return productId;
    }

    private HttpRequest.Builder getShopifyRequestBuilder(final String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + url))
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json");
    }
    
}
