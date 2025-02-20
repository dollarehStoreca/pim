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

class MultiCraftTest {

    @Test
    void testGetCategories() throws URISyntaxException, IOException, CsvException {
        MultiCraft multiCraft = new MultiCraft();
        Category category = multiCraft.getCategory(null, "colfact~paints~watercolor");

        String[] headers;

        Path filePath = Paths.get("sample/product_template.csv");
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                headers = csvReader.readAll().get(0);
            }
        }

        Path csvImporPah = Paths.get("data/product_import.csv");

        if (csvImporPah.toFile().exists()) {
            csvImporPah.toFile().delete();
        } else {
            csvImporPah.toFile().getParentFile().mkdirs();
        }

        createProducts(csvImporPah, headers, category);

        for (Category subCategory : category.categories()) {
            createProducts(csvImporPah, headers, subCategory);
        }

    }

    private static void createProducts(Path csvImporPah, String[] headers, Category category) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvImporPah.toString()))) {
            writer.writeAll(Collections.singleton(headers));


            for (Product product : category.products()) {

                String[] newLine = new String[headers.length];
                newLine[0] = product.code();
                newLine[1] = product.title();
                newLine[2] = product.description();
                newLine[3] = "Dollareh";
                newLine[4] = category.code();
                if(category.parent() != null) {
                    newLine[5] = category.parent().code();
                }


                newLine[6] = "Imported";
                newLine[7] = "TRUE";

                newLine[16] = "shopify";

                newLine[18] = "deny";

                newLine[19] = "manual";

                newLine[22] = "TRUE";
                newLine[23] = "TRUE";

                newLine[25] = product.imageUrls()[0];

                newLine[45] = "g";
                newLine[50] = "active";

                writer.writeAll(Collections.singleton(newLine));

                // Cleanup Column Values
                Arrays.fill(newLine, "");

                for (int i = 1; i < product.imageUrls().length; i++) {
                    newLine[25] = product.imageUrls()[i];
                    writer.writeAll(Collections.singleton(newLine));
                }
            }
        }
    }

}