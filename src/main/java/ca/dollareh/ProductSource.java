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

    void forEach(Consumer<Product> productConsumer) throws URISyntaxException, IOException, InterruptedException;

    void downloadAssets(final Product product) throws IOException;

    void logout() throws IOException;


    default void onProductDiscovery(final Consumer<Product> productConsumer, final Product product) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Path productJsonPath = getPath(product);

        if(productJsonPath.toFile().exists()) {
            String productJsonTxt = objectMapper
                    .writeValueAsString(product);
            if (!productJsonTxt.equals(Files.readString(productJsonPath))) {
                Files.writeString(productJsonPath,
                        productJsonTxt);
                productConsumer.accept(product);
            }
        } else {
            productJsonPath.toFile().getParentFile().mkdirs();

            Files.writeString(productJsonPath,
                    objectMapper
                            .writeValueAsString(product));

            productConsumer.accept(product);
        }
    }


    private Path getPath(final Product product) {
        StringBuilder builder = new StringBuilder("workspace/extracted/" + getClass().getSimpleName() + "/");

        if(product.categories() != null) {
            product.categories().forEach(category -> {
                do {
                    builder.append(category.code()).append("/");
                } while (category.parent() == null);
            });
        }


        builder.append(product.code());
        builder.append(".json");

        return Path.of(builder.toString());
    }


}
