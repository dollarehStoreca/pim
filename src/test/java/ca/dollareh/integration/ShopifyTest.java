package ca.dollareh.integration;

import ca.dollareh.vendor.MultiCraft;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

class ShopifyTest {

    @Test
    void testShoppifyMulticraft() throws IOException, URISyntaxException {
//        new Shopify(new MultiCraft())
//                .downloadCSV(
//                        Paths.get("data/product_import.csv")
//                );

        new MultiCraft().downloadImage("https://multicraft.ca/pics/01/061542172868.jpg");
    }

}