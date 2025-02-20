package ca.dollareh.vendor;

import ca.dollareh.core.model.Category;
import ca.dollareh.core.model.Product;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

class MultiCraftTest {

    @Test
    void testGetCategories() throws URISyntaxException, IOException, CsvException {
        Path csvImporPah = Paths.get("data/product_import.csv");
        downloadProductsCSV(csvImporPah);
    }

    public void downloadProductsCSV(final Path csvImporPah) throws IOException, CsvException, URISyntaxException {

        // Create Header
        String[] headers;

        Path filePath = Paths.get("sample/product_template.csv");
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                headers = csvReader.readAll().get(0);
            }
        }

        // Delete CSV If Exists
        if (csvImporPah.toFile().exists()) {
            csvImporPah.toFile().delete();
        } else {
            csvImporPah.toFile().getParentFile().mkdirs();
        }




        try (CSVWriter writer = new CSVWriter(new FileWriter(csvImporPah.toString()))) {

            MultiCraft multiCraft = new MultiCraft(product -> {
                // I can perfom steps on product
                System.out.println(product);
                writeProduct(writer, headers, product);
            });

            writer.writeAll(Collections.singleton(headers));

            Category category = multiCraft.getCategory(null, "scrapbook~albums");

            multiCraft.logout();

        }



    }


    private static void writeProduct(CSVWriter writer, String[] headers, Product product) {
        String[] newLine = new String[headers.length];
        newLine[0] = product.code();
        newLine[1] = product.title();
        newLine[2] = product.description();
        newLine[3] = "Dollareh";
        newLine[4] = product.category().code();
        if(product.category().parent() != null) {
            newLine[5] = product.category().parent().code();
        }

        newLine[6] = "Imported";
        newLine[7] = "TRUE";

        newLine[16] = "shopify";

        newLine[17] = "" + product.inventryCode();

        newLine[18] = "deny";

        newLine[19] = "manual";

        newLine[20] = "" + product.price();

        newLine[22] = "TRUE";
        newLine[23] = "TRUE";

        newLine[25] = product.imageUrls()[0];

        newLine[45] = "g";
        newLine[50] = "active";

        newLine[51] = product.category().code();

        newLine[53] = "" + product.upc();

        writer.writeAll(Collections.singleton(newLine));

        // Cleanup Column Values
        Arrays.fill(newLine, "");

        if(product.imageUrls().length >= 2 ) {
            for (int i = 1; i < 2; i++) {
                newLine[0] = product.code();
                newLine[25] = product.imageUrls()[i];
                writer.writeAll(Collections.singleton(newLine));
            }
        }
    }

}