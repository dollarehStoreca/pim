package ca.dollareh;

import ca.dollareh.core.model.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface ProductSource {
    void forEach(Consumer<Product> productConsumer) throws URISyntaxException, IOException;

    void downloadAssets(final Product product) throws IOException;

    void logout() throws IOException;

    default Path getPath(final Product product) {
        StringBuilder builder = new StringBuilder("workspace/extracted/" + getClass().getSimpleName() + "/");

        product.categories().forEach(category1 -> {
            do {
                builder.append(category1.code()).append("/");
            } while (category1.parent() == null);
        });

        builder.append(product.code());
        builder.append(".json");

        Path productJsonPath = Path.of(builder.toString());
        return productJsonPath;
    }

    default void onProductDiscovery(final Consumer<Product> productConsumer, final Product product) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Path productJsonPath = getPath(product);

        if(productJsonPath.toFile().exists()) {
            String productJsonTxt = objectMapper
                    .writeValueAsString(product);

            if (!productJsonTxt.equals(Files.readString(productJsonPath))) {
                System.out.println("Product Modified " + product.code());
                Files.writeString(productJsonPath,
                        productJsonTxt);
                productConsumer.accept(product);
            }

        } else {
            System.out.println("New Product found " + product.code());

            productJsonPath.toFile().getParentFile().mkdirs();

            Files.writeString(productJsonPath,
                    objectMapper
                            .writeValueAsString(product));

            productConsumer.accept(product);
        }
    }

}
