package ca.dollareh.pim.source;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

class PepperiTest {
    final Logger logger = LoggerFactory.getLogger(PepperiTest.class);
    @Test
    void testMultiCraft() throws IOException, URISyntaxException {
        ProductSource productSource = ProductSource
                .from(Pepperi.class)
                .onNew(newProduct -> {
                    logger.info("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    logger.info("Product Modified " + updatedProduct);
                })
                .build();
        productSource.extraxt();
    }
}