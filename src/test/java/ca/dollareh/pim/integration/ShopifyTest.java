package ca.dollareh.pim.integration;

import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.source.Ctg;
import ca.dollareh.pim.source.MultiCraft;
import ca.dollareh.pim.source.Pepperi;
import ca.dollareh.pim.source.ProductSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void testPepperi() throws IOException, InterruptedException {
        ProductSource productSource = ProductSource
                .from(Pepperi.class)
                .onNew(newProduct -> {
                    logger.info("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    logger.info("Product Modified " + updatedProduct);
                })
                .build();

        //new Shopify(productSource).createCollectionMappings();

        new Shopify(productSource).export();
    }

    @Test
    void testUpload() throws IOException, InterruptedException {
        ProductSource productSource = ProductSource
                .from(Ctg.class)
                .onNew(newProduct -> {
                    logger.info("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    logger.info("Product Modified " + updatedProduct);
                })
                .build();

        Product product = new Product("SBK","Karri","Sobhanbabu",123456345L,12,2f,4f,1f,null);
        // new Shopify(productSource).createCollectionMappings();
        new Shopify(productSource).create(product);
    }

}