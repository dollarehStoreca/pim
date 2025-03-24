package ca.dollareh.pim.integration;

import ca.dollareh.pim.source.MultiCraft;
import ca.dollareh.pim.source.ProductSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class ShopifyTest {


    final Logger logger = LoggerFactory.getLogger(ShopifyTest.class);

    @Test
    void testUpdateWebsite() throws IOException, InterruptedException {
        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                .onNew(newProduct -> {
                    logger.info("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    logger.info("Product Modified " + updatedProduct);
                })
                .build();

        // new Shopify(productSource).createCollectionMappings();
        new Shopify(productSource).export();
    }

}