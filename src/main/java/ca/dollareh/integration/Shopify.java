package ca.dollareh.integration;

import ca.dollareh.vendor.ProductSource;
import ca.dollareh.core.model.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shopify {

    final Logger logger = LoggerFactory.getLogger(Shopify.class);

    protected final Path exportPath = Path.of("workspace/export/" + getClass().getSimpleName());

    private final ProductSource productSource;

    public Shopify(ProductSource productSource) {
        this.productSource = productSource;

        exportPath.toFile().mkdirs();
    }

    public void export() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        Path enrichmentPath = Path.of("workspace/enrichment/" + productSource.getClass().getSimpleName());

        Map<String, Object> shoppifyTemplate = objectMapper.readValue(Path.of("sample/product-creation.json").toFile()
                , new TypeReference<>() {});

        try (Stream<Path> paths = Files.walk(enrichmentPath)) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                    .toList();

            jsonFiles.forEach(enrichedJsonPath -> {

                try {
                    Product enrichedProduct = objectMapper
                            .readValue(enrichedJsonPath.toFile(), Product.class);

                    // Message Conversion

                    Map<String, Object> productMap = ((Map<String, Object>) shoppifyTemplate.get("product"));

                    productMap.put("title", enrichedProduct.title());
                    productMap.put("body_html", enrichedProduct.description());


                    File shoppifyJson = new File(exportPath.toFile(), enrichedProduct.code() + ".json");

                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(shoppifyJson, shoppifyTemplate);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
