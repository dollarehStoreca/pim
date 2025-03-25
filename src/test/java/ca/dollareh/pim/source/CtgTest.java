package ca.dollareh.pim.source;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class CtgTest {
    final Logger logger = LoggerFactory.getLogger(CtgTest.class);
    @Test
    void testExtract() throws IOException, URISyntaxException {
        ProductSource productSource = ProductSource
                .from(Ctg.class)
                .build();
        productSource.extraxt();
    }
}