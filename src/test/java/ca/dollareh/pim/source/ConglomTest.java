package ca.dollareh.pim.source;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

class ConglomTest {
    final Logger logger = LoggerFactory.getLogger(ConglomTest.class);
    @Test
    void testConglom() throws IOException, URISyntaxException {
        ProductSource productSource = ProductSource
                .from(Conglom.class)
                .build();
        productSource.extraxt();
    }
}