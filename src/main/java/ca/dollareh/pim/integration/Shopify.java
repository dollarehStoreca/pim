package ca.dollareh.pim.integration;

import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.source.ProductSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

public class Shopify {

    final Logger logger = LoggerFactory.getLogger(Shopify.class);

    private final String baseUrl;
    private final String accessToken;

    private final Path exportPath;
    private final Path enrichmentPath;

    private final ProductSource productSource;
    private final Long defaultCollectionId;

    private final ObjectMapper objectMapper;

    private final String checksumAlgorithm = "SHA-256";

    public Shopify(ProductSource productSource) {
        this.productSource = productSource;

        baseUrl = "https://" + System.getenv("SHOPIFY_STORE_URL") + "/admin/api/2025-01";
        accessToken = System.getenv("SHOPIFY_ACCESS_TOKEN");

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        exportPath = Path.of("workspace/export/" + getClass().getSimpleName() + "/" + productSource.getClass().getSimpleName());
        enrichmentPath = Path.of("workspace/enrichment/" + productSource.getClass().getSimpleName());

        exportPath.toFile().mkdirs();

        File collectionMappingsFile = new File(exportPath.toFile().getParentFile(), "collection.properties");

        if (collectionMappingsFile.exists()) {
            Properties cProperties = new Properties();
            try(FileReader fileReader = new FileReader(collectionMappingsFile)) {
                cProperties.load(fileReader);
            } catch (IOException e) {
                logger.error("Collection Mapping Properties does not exists {}", collectionMappingsFile);
            }
            defaultCollectionId = Long.parseLong((String) cProperties.get(productSource.getClass().getSimpleName()));
        } else {
            try {
                Files.writeString(collectionMappingsFile.toPath(),
                        productSource.getClass().getSimpleName() + "=<COLLECTION_ID_FROM_SHOPIFY>");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException("Collection Mapping not available for " + productSource.getClass().getSimpleName()
                    + " at "+ collectionMappingsFile.getAbsolutePath());
        }
    }

    public void export() throws IOException, InterruptedException {

        File[] enrichedJsonFiles = enrichmentPath.toFile().listFiles((dir, name) -> name.endsWith("AB020A.json"));

        if (enrichedJsonFiles != null) {
            for (File enrichedJsonFile : enrichedJsonFiles) {
                Product enrichedProduct = objectMapper.readValue(enrichedJsonFile, Product.class);

                File shopifyProductJsonFile = getProductFile(enrichedProduct);

                boolean changed = false ;
                if (shopifyProductJsonFile.exists()) {
                    if(isModified(enrichedJsonFile)) {
                        changed = update(getProductId(shopifyProductJsonFile), enrichedProduct);
                    }
                } else {
                    changed = create(enrichedProduct);
                }
                if(changed) {
                    generateAndStoreChecksum(enrichedJsonFile );
                }

            }
        }
    }

    private boolean create(final Product product) throws IOException, InterruptedException {
        boolean created = false;
        try (HttpClient client = HttpClient.newHttpClient()) {

            HttpRequest request = getShopifyRequestBuilder("/products.json").POST(HttpRequest.BodyPublishers.ofString(getShopifyProduct(product))).build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == SC_CREATED) {

                File productJsonFile = getProductFile(product);

                Files.write(productJsonFile.toPath(), response.body(), StandardOpenOption.CREATE);

                Long productId = getProductId(productJsonFile);

                associateCollection(productId, defaultCollectionId);
                createImages(productId, product);

                logger.info("Product {} created", productId);

                created = true;

            } else {
                logger.error("Product {} not created", product.code());
            }
        }
        return created;
    }

    public boolean update(final Long productId, final Product product) throws IOException, InterruptedException {
        boolean updated = false;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = getShopifyRequestBuilder("/products/" + productId + ".json").PUT(HttpRequest.BodyPublishers.ofString(getShopifyProduct(product))).build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == HttpStatus.SC_OK) {
                Files.write(getProductFile(product).toPath(), response.body());
                logger.info("Product {} updated", productId);
                updated = true;
            } else {
                logger.error("Product {} not updated", product.code());

            }
        }
        return updated;
    }

    private File getProductFile(final Product product) {
        return new File(exportPath.toFile(), product.code() + ".json");
    }

    private String getShopifyProduct(final Product product) throws JsonProcessingException {

        Map<String, Object> variantMap = Map.of("price", product.price(), "compare_at_price", product.discount(), "inventory_quantity", product.inventryQuantity(), "title", "Default Title", "inventory_policy", "deny", "inventory_management", "shopify", "option1", "Default Title", "fulfillment_service", "manual", "taxable", true, "requires_shipping", true);

        Map<String, Object> productMap = Map.of("title", product.title(), "body_html", product.description(), "handle", product.code(), "vendor", "Dollareh", "tags", "auto-imported", "variants", List.of(variantMap));


        return objectMapper.writeValueAsString(Map.of("product", productMap));
    }

    public void associateCollection(final Long productId, final Long collectionId) throws IOException, InterruptedException {

        try (HttpClient client = HttpClient.newHttpClient()) {

            // Build HTTP request
            HttpRequest request = getShopifyRequestBuilder("/collects.json").POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("collect", Map.of("product_id", productId, "collection_id", collectionId))))).build();

            // Send request and get response
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == SC_CREATED) {
                logger.info("Product {} asociated to Collection {}", productId, collectionId);
            } else {
                logger.info("Product {} can not be associated to Collection {}", productId, collectionId);
            }
        }
    }

    public void createImages(final Long productId, Product product) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {

            // Build HTTP request
            HttpRequest.Builder requestBuilder = getShopifyRequestBuilder("/products/" + productId + "/images.json");

            for (String imageUrl : product.imageUrls()) {
                File imageFile = productSource.getAssetFile(imageUrl);

                // Convert Image to Base64
                String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));

                // Create JSON request body
                String requestBody = """
                        {
                          "image": {
                            "attachment": "%s"
                          }
                        }
                        """.formatted(base64Image);

                HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

                // Send request and get response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == SC_OK) {
                    logger.info("Image {} upload for product {}", imageUrl, product.code());
                } else {
                    logger.error("Image {} not upload for product {}", imageUrl, product.code());
                }

            }
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
        return HttpRequest.newBuilder().uri(URI.create(baseUrl + url)).header("X-Shopify-Access-Token", accessToken).header("Content-Type", "application/json");
    }

    private void generateAndStoreChecksum(File file) {
        try {
            String checksum = getChecksum(file);

            Path cPath = getChecksumPath(file);

            Files.writeString(cPath,  checksum);

            logger.info("generated checksum for {} ", file);

        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Unable to generate checksum for {} ", file);
        }
    }

    private Path getChecksumPath(final File file) {
        return new File(exportPath.toFile(), file.getName() + ".ck").toPath();
    }

    private boolean isModified(final File file) {
        Path cPath = getChecksumPath(file);
        boolean isModified = !cPath.toFile().exists();

        if (!isModified) {
            try {
                isModified = !Files.readString(cPath).equals(getChecksum(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }


        return isModified;
    }

    private String getChecksum(final File file) throws IOException, NoSuchAlgorithmException {
        Path path = file.toPath();
        byte[] fileBytes = Files.readAllBytes(path);

        // Generate checksum
        MessageDigest digest = MessageDigest.getInstance(checksumAlgorithm);
        byte[] checksumBytes = digest.digest(fileBytes);
        String checksum = file.lastModified() + "$" + bytesToHex(checksumBytes);
        return checksum;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

}
