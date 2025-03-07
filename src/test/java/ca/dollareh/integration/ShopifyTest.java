package ca.dollareh.integration;

import ca.dollareh.vendor.MultiCraft;
import ca.dollareh.vendor.ProductSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ShopifyTest {

    @Test
    void testUpdateWebsite() throws IOException, InterruptedException {
        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                .onNew(newProduct -> {
                    System.out.println("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    System.out.println("Product Modified " + updatedProduct);
                })
                .build();

        System.out.println(new Shopify(productSource).createCollection("Electronics", List.of("SS")));

        System.out.println(new Shopify(productSource).createCollection("Electronics 2", null));

        System.out.println(new Shopify(productSource).createCollection("Electronics 3", new ArrayList<>()));
        ;

        // new Shopify(productSource).export();
        // new MultiCraft().downloadImage("https://multicraft.ca/pics/01/061542172868.jpg");
    }

}