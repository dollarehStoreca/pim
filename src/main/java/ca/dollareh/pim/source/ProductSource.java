package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class ProductSource {

    public static final String COLLECTION_SEPARATOR = "-";

    final Logger logger = LoggerFactory.getLogger(ProductSource.class);

    protected final Path path = Path.of("workspace/extracted/" + getClass().getSimpleName());

    protected final Path transformPath = Path.of("workspace/transform/" + getClass().getSimpleName());

    protected final Path enrichmentPath = Path.of("workspace/enrichment/" + getClass().getSimpleName());

    private final Path assetsPath = Path.of("workspace/extracted/"+ getClass().getSimpleName() +"/assets/" );

    final Consumer<Product> newProductConsumer;
    final Consumer<Product> modifiedProductConsumer;

    private final ObjectMapper objectMapper;

    protected ProductSource(final Consumer<Product> newProductConsumer,
                            final Consumer<Product> modifiedProductConsumer) {
        this.newProductConsumer = newProductConsumer;
        this.modifiedProductConsumer = modifiedProductConsumer;

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // Use a builder to instantiate ProductSource
    public static ProductSourceBuilder from(Class<? extends ProductSource> productSourceClassr) {
        return new ProductSourceBuilder(productSourceClassr);
    }

    private void enrich() {
        Validator validator = Validation.buildDefaultValidatorFactory()
                .getValidator();

        File[] transformedFiles = transformPath.toFile().listFiles(pathname -> pathname.getName().endsWith(".json"));

        Arrays.stream(transformedFiles)
                .parallel()
                .forEach(transformedJsonFile -> {
                    String productCode = transformedJsonFile.getName().replaceAll(".json","");
                    try {
                        List<File> originalJsonFiles = findOriginalProductJson(productCode);

                        if (!originalJsonFiles.isEmpty()) {

                            File originalJsonFile = originalJsonFiles.get(0);

                            Product originalProduct = objectMapper
                                    .readValue(originalJsonFile, Product.class);

                            File enrichedProductCollectionsFile = new File(enrichmentPath.toFile(),
                                    originalProduct.code() + ".csv");

                            enrichedProductCollectionsFile.getParentFile().mkdirs();

                            Files.writeString(enrichedProductCollectionsFile.toPath(), originalJsonFiles.stream().map(file ->
                                    file.getName()
                                            .replaceFirst((originalProduct.code()+ COLLECTION_SEPARATOR),"")
                                            .replaceFirst(".json","").toLowerCase()

                            ).collect(Collectors.joining("\n")));

                            Product transformProduct = objectMapper
                                    .readValue(transformedJsonFile, Product.class);

                            Product enrichedProduct = originalProduct.merge(transformProduct);

                            Set<ConstraintViolation<Product>> violations = validator.validate(enrichedProduct);

                            if(violations.isEmpty()) {
                                File enrichedProductFile = new File(enrichmentPath.toFile(),
                                        originalProduct.code() + ".json");

                                enrichedProductFile.getParentFile().mkdirs();

                                downloadAssets(enrichedProduct);

                                Files.writeString(enrichedProductFile.toPath(), objectMapper.writeValueAsString(enrichedProduct));
                            }
                            else {
                                for (ConstraintViolation<Product> violation : violations) {
                                    throw new IllegalArgumentException(productCode + " : " + violation.getMessage());
                                }
                            }
                        } else {
                            logger.info(this.getClass().getSimpleName() + " does not contain product " + productCode);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    protected abstract void login() throws IOException;

    protected abstract void logout() throws IOException;

    protected abstract void browse() throws IOException, URISyntaxException;

    protected abstract File downloadAsset(final String assetUrl) throws IOException;

    protected void downloadAssets(final Product product) throws IOException {
        for (String imageUrl: product.imageUrls()) {
            downloadAsset(imageUrl);
        }
    }

    protected void onProductDiscovery(final List<String> categories,
                                      final Product product) throws IOException {

        String category = categories.isEmpty() ? "" : categories.stream()
                .collect(Collectors.joining(COLLECTION_SEPARATOR));

        Path productJsonPath = new File(path.toFile(), product.code() + COLLECTION_SEPARATOR + category + ".json").toPath();

        if (productJsonPath.toFile().exists()) {
            String productJsonTxt = objectMapper
                    .writeValueAsString(product);
            if (!productJsonTxt.equals(Files.readString(productJsonPath))) {
                Files.writeString(productJsonPath,
                        productJsonTxt);
                modifiedProductConsumer.accept(product);
            }
        } else {
            productJsonPath.toFile().getParentFile().mkdirs();
            Files.writeString(productJsonPath,
                    objectMapper
                            .writeValueAsString(product));
            newProductConsumer.accept(product);
        }
    }

    public File getAssetFile(final String assetUrl) {
        return Path.of(assetsPath +"/" + assetUrl).toFile();
    }


    public List<List<String>>  getCollection(String code) {

        File[] files = path.toFile().listFiles((dir, name) -> name.startsWith(code+COLLECTION_SEPARATOR));

        if(files == null || files.length == 0) {
            return new ArrayList<>();
        }

        return List.of(files).stream().map(file ->
        List.of(file.getName()
                .replaceFirst(code + COLLECTION_SEPARATOR,"")
                .replaceFirst(".json","").split(COLLECTION_SEPARATOR)))

                .collect(Collectors.toList());
    }

    private List<File> findOriginalProductJson(String code) {
        File[] files = path.toFile().listFiles((dir, name) -> name.startsWith(code+COLLECTION_SEPARATOR));

        return List.of(files) ;
    }

    public void extraxt() throws IOException, URISyntaxException {
        login();
        this.browse();
        this.enrich();
        logout();
    }

    // Builder class
    public static class ProductSourceBuilder {

        private final Class<? extends ProductSource> productSourceClass;

        private Consumer<Product> newProductConsumer = p -> {
        };  // Default empty consumer
        private Consumer<Product> modifiedProductConsumer = p -> {
        }; // Default empty consumer

        public ProductSourceBuilder(Class<? extends ProductSource> productSourceClass) {
            this.productSourceClass = productSourceClass;
        }

        public ProductSourceBuilder onNew(Consumer<Product> consumer) {
            this.newProductConsumer = consumer;
            return this;
        }

        public ProductSourceBuilder onModified(Consumer<Product> consumer) {
            this.modifiedProductConsumer = consumer;
            return this;
        }

        public ProductSource build() {
            try {
                return productSourceClass
                        .getDeclaredConstructor(Consumer.class, Consumer.class)
                        .newInstance(newProductConsumer, modifiedProductConsumer);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ProductSource", e);
            }
        }
    }

}
