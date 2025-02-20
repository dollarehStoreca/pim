package ca.dollareh;

import ca.dollareh.core.model.Product;

import java.util.function.Consumer;

public interface ProductSource {
    void forEach(Consumer<Product> productConsumer);
}
