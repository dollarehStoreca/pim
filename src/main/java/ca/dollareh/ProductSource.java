package ca.dollareh;

import ca.dollareh.core.model.Product;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface ProductSource {
    void forEach(Consumer<Product> productConsumer) throws URISyntaxException, IOException;

    void downloadAssets(final Product product) throws IOException;

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

}
