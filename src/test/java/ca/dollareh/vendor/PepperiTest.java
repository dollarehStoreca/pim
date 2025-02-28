package ca.dollareh.vendor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

class PepperiTest {
    @Test
    void testCrawl() throws URISyntaxException, IOException, InterruptedException {
        new Pepperi().forEach(product -> {
            System.out.println(product);
        });
    }
}