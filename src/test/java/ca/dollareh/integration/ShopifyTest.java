package ca.dollareh.integration;

import ca.dollareh.vendor.MultiCraft;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ShopifyTest {

    @Test
    void testShoppifyMulticraft() throws IOException, URISyntaxException, CsvException {

        Path csvImporPah = Paths.get("data/product_import.csv");

        new Shopify(new MultiCraft())
                .downloadCSV(csvImporPah);
    }

}