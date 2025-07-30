package ca.dollareh.pim.integration;

import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.source.ProductSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            String cIdStr = (String) cProperties.get(productSource.getClass().getSimpleName());
            if(cIdStr == null) {
                Optional<Map<String, Object>> defaultCollection =  getDefaultCollection();
                if(defaultCollection.isPresent()) {
                    defaultCollectionId = (Long) defaultCollection.get().get("id");
                    cProperties.put(productSource.getClass().getSimpleName(),defaultCollectionId.toString());
                    try (FileWriter fileWriter = new FileWriter(collectionMappingsFile)){
                        cProperties.store(fileWriter, "Updated with " + defaultCollection.get().get("title"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }else {
                    throw new RuntimeException("Collection Mapping not available for " + productSource.getClass().getSimpleName());
                }
            } else {
                defaultCollectionId = Long.parseLong(cIdStr);
            }

        } else {
            Optional<Map<String, Object>> defaultCollection =  getDefaultCollection();
            if(defaultCollection.isPresent()) {
                try {
                    defaultCollectionId = (Long) defaultCollection.get().get("id");
                    Files.writeString(collectionMappingsFile.toPath(),
                            productSource.getClass().getSimpleName() + "=" + defaultCollectionId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Collection Mapping not available for " + productSource.getClass().getSimpleName());
            }
        }
    }

    public void export() throws IOException, InterruptedException {

        File[] enrichedJsonFiles = enrichmentPath.toFile()
                .listFiles((dir, name) -> name.endsWith(".json"));

        if (enrichedJsonFiles != null) {
            for (File enrichedJsonFile : enrichedJsonFiles) {
                Product enrichedProduct = objectMapper.readValue(enrichedJsonFile, Product.class);

                File shopifyProductJsonFile = getProductFile(enrichedProduct.code());

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

                File productJsonFile = getProductFile(product.code());

                Files.write(productJsonFile.toPath(), response.body(), StandardOpenOption.CREATE);

                Long productId = getProductId(productJsonFile);

                logger.info("Product {} created", productId);

                associateCollection(productId, defaultCollectionId);
                createImages(productId, product);
                setInventoryItem(productJsonFile, product);


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
                File productJsonFile = getProductFile(product.code());
                Files.write(productJsonFile.toPath(), response.body());
                logger.info("Product {} updated", productId);

                setInventoryItem(productJsonFile, product);

                updated = true;
            } else {
                logger.error("Product {} not updated", product.code());
            }
        }
        return updated;
    }

    public File getProductFile(final String code) {
        return new File(exportPath.toFile(), code + ".json");
    }

    private String getShopifyProduct(final Product product) throws JsonProcessingException {

        Map<String, Object> variantMap = new HashMap<>();
        variantMap.put("price", product.price());
//        variantMap.put("compare_at_price", product.discount());
        variantMap.put("inventory_quantity", product.inventoryQuantity());
        variantMap.put("title", "Default Title");
        variantMap.put("inventory_policy", "deny");
        variantMap.put("inventory_management", "shopify");
        variantMap.put("option1", "Default Title");
        variantMap.put("fulfillment_service", "manual");
        variantMap.put("taxable", true);
        variantMap.put("requires_shipping", true);
        variantMap.put("sku",product.code());

        Map<String, Object> productMap = Map.of("title", product.title(),
                "body_html", product.description(),
                "handle", product.code(),
                "vendor", "Dollareh",
                "tags", "auto-imported-unique",
                "variants", List.of(variantMap));

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
                logger.error("Product {} can not be associated to Collection {}", productId, collectionId);
            }
        }
    }

    public void createImages(final Long productId, Product product) throws IOException, InterruptedException {

        try (HttpClient client = HttpClient.newHttpClient()) {
            // Build HTTP request
            HttpRequest.Builder requestBuilder = getShopifyRequestBuilder("/products/" + productId + "/images.json");

            for (String imageUrl : product.imageUrls()) {
                File imageFile = imageUrl.trim().isEmpty() ?
                        new File("sample/no-image.png")
                        : productSource.getAssetFile(imageUrl);
                if(imageFile.exists()) {

                    logger.info("Creating Image {} for Product {}", imageFile, productId );

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
    }

    private void setInventoryItem(final File productJsonFile, final Product product) throws IOException, InterruptedException {
        Long inventoryItemId = null;
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(productJsonFile)) {
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == JsonToken.FIELD_NAME && "inventory_item_id".equals(parser.currentName())) {
                    parser.nextToken();
                    inventoryItemId = parser.getLongValue();
                    break; // Exit early after finding the required field
                }
            }
        }


        try (HttpClient client = HttpClient.newHttpClient()) {
            // Build HTTP request
            HttpRequest request = getShopifyRequestBuilder("/inventory_items/"+inventoryItemId+".json")
                    .PUT(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(
                                    Map.of("inventory_item",
                                            Map.of("id", inventoryItemId,
                                                    "cost", product.cost())))))
                    .build();

            // Send request and get response
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == SC_OK) {
                logger.info("Inventory item {} set for product {}", inventoryItemId, product.code());
            } else {
                logger.error("Inventory item {} not set for product {}", inventoryItemId, product.code());
            }
        }
    }


    public Long getProductId(final File productJsonFile) throws IOException {
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

    private Optional<Map<String, Object>> getDefaultCollection() {
        List<Map<String, Object>> collections = getShopifyCollection(); // The value you're searching for

        return collections.stream()
                .filter(this::isVendor)
                .findFirst();
    }

    private boolean isVendor(Map<String, Object> collection) {
        Long collectionId = (Long) collection.get("id");
        List<Map<String, Object>> metaFields = getMetafields(collectionId);

        Map<String, Object> objectMap = metaFields.stream().filter(metaField ->
                metaField.get("namespace").equals("product")
                        && metaField.get("key").equals("vendor")
                        && metaField.get("value").toString().contains("\"" + productSource.getClass().getSimpleName() + "\"")
        ).findFirst().orElse(null);

        return objectMap != null ;
    }


    public List<Map<String, Object>> getShopifyCollection() {
        List<Map<String, Object>> allCollections = new ArrayList<>();
        try (HttpClient client = HttpClient.newHttpClient()) {
            String nextPageUrl = "/custom_collections.json?limit=250"; // Start with first page

            try {
                while (nextPageUrl != null) {
                    HttpRequest request = getShopifyRequestBuilder(nextPageUrl).GET().build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Failed to fetch Shopify collections: " + response.body());
                    }

                    Map<String, List<Map<String, Object>>> responseBody = objectMapper.readValue(response.body(),
                            new TypeReference<>() {
                            });

                    List<Map<String, Object>> collections = responseBody.get("custom_collections");
                    if (collections != null) {
                        allCollections.addAll(collections);
                    }

                    // Check for pagination links
                    Optional<String> nextPageLink = response.headers()
                            .allValues("Link")
                            .stream()
                            .flatMap(link -> Arrays.stream(link.split(",")))
                            .filter(link -> link.contains("rel=\"next\""))
                            .findFirst();

                    if (nextPageLink.isPresent()) {
                        String link = nextPageLink.get();
                        int start = link.indexOf("<") + 1;
                        int end = link.indexOf(">");
                        nextPageUrl = link.substring(start, end).replace(baseUrl, "");
                    } else {
                        nextPageUrl = null; // No more pages
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching Shopify collections", e);
            }
        }

        return allCollections;
    }

    private List<Map<String, Object>> getMetafields(Long collectionId) {
        try (HttpClient client = HttpClient.newHttpClient()) {

            HttpRequest request = getShopifyRequestBuilder("/collections/" + collectionId + "/metafields.json")
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
    }


    private HttpRequest.Builder getShopifyRequestBuilder(final String url) {
        return HttpRequest.newBuilder().uri(URI.create(baseUrl + url)).header("X-Shopify-Access-Token", accessToken).header("Content-Type", "application/json");
    }

    private void generateAndStoreChecksum(File file) {
        try {
            String checksum = getChecksum(file);

            Path cPath = getChecksumPath(file);

            Files.writeString(cPath,  checksum);

            logger.debug("generated checksum for {} ", file);

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
        String checksumAlgorithm = "SHA-256";
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
