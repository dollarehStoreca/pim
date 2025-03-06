package ca.dollareh.vendor;

import ca.dollareh.core.model.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ProductSource {

    protected final Path path = Path.of("workspace/extracted/" + getClass().getSimpleName());

    protected final Path transformPath = Path.of("workspace/transform/" + getClass().getSimpleName());

    protected final Path enrichmentPath = Path.of("workspace/enrichment/" + getClass().getSimpleName());

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

    public void enrich() {
        Arrays.stream(transformPath.toFile().listFiles(pathname -> pathname.getName().endsWith(".json")))
                .parallel()
                .forEach(transformedJsonFile -> {
                    try {
                        Optional<File> originalJsonFile = findOriginalProductJson(transformedJsonFile.getName());

                        if (originalJsonFile.isPresent()) {
                            Product originalProduct = objectMapper
                                    .readValue(originalJsonFile.get(), Product.class);

                            Product transformProduct = objectMapper
                                    .readValue(transformedJsonFile, Product.class);

                            Product enrichedProduct = originalProduct.merge(transformProduct);

                            File enrichedProductFile = new File(enrichmentPath.toFile(),
                                    originalJsonFile.get().getName());

                            enrichedProductFile.getParentFile().mkdirs();

                            Files.writeString(enrichedProductFile.toPath(), objectMapper.writeValueAsString(enrichedProduct));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public abstract void browse() throws IOException, URISyntaxException;

    public abstract File downloadAsset(final String assetUrl) throws IOException;

    protected void onProductDiscovery(final List<String> categories,
                                      final Product product) throws IOException {

        String category = categories.isEmpty() ? "" : "-" + categories.stream().collect(Collectors.joining("-"));

        Path productJsonPath = new File(path.toFile(), product.code() + category + ".json").toPath();

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

    private Optional<File> findOriginalProductJson(String fileName) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return Optional.empty();
        }

        try (Stream<Path> paths = Files.walk(path)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .map(Path::toFile)
                    .findFirst();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
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
