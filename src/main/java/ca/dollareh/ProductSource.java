package ca.dollareh;

import ca.dollareh.core.model.Product;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public interface ProductSource {
    void forEach(Consumer<Product> productConsumer) throws URISyntaxException, IOException;
}
