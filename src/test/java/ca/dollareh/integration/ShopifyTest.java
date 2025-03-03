package ca.dollareh.integration;

import ca.dollareh.vendor.MultiCraft;
import ca.dollareh.vendor.ProductSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ShopifyTest {

    @Test
    void testUpdateWebsite() throws IOException {
        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                .onNew(newProduct -> {
                    System.out.println("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    System.out.println("Product Modified " + updatedProduct);
                })
                .build();

        new Shopify(productSource).export();
        // new MultiCraft().downloadImage("https://multicraft.ca/pics/01/061542172868.jpg");
    }

}