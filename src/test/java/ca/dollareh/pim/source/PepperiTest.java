package ca.dollareh.pim.source;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PepperiTest {
    final Logger logger = LoggerFactory.getLogger(PepperiTest.class);

    @Test
    void testMultiCraft() throws IOException, URISyntaxException {
         ProductSource productSource = ProductSource
                 .from(Pepperi.class)
                 .build();
         productSource.extraxt();
    }
}