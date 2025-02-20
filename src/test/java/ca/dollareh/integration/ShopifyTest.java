package ca.dollareh.integration;

import ca.dollareh.vendor.MultiCraft;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ShopifyTest {

    @Test
    void testShoppifyMulticraft() {

        Path csvImporPah = Paths.get("data/product_import.csv");

        new Shopify(new MultiCraft())
                .downloadCSV(csvImporPah);
    }

}