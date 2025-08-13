package ca.dollareh.pim.source;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class SymaksalesTest {
    final Logger logger = LoggerFactory.getLogger(SymaksalesTest.class);
    @Test
    void testExtract() throws IOException, URISyntaxException {
        ProductSource productSource = ProductSource
                .from(Symaksales.class)
                .build();
        productSource.extraxt();
    }
}