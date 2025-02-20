package ca.dollareh.integration;

import ca.dollareh.ProductSource;

import java.nio.file.Path;

public class Shopify {

    private final ProductSource productSource;

    public Shopify(ProductSource productSource) {
        this.productSource = productSource;
    }

    void downloadCSV(final Path csvPath) {

    }

}
