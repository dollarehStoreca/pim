package ca.dollareh.vendor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import ca.dollareh.core.model.Category;
import ca.dollareh.core.model.Product;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.junit.jupiter.api.Test;

class MultiCraftTest {

    @Test
    void testGetCategories() throws URISyntaxException, IOException, CsvException {
        MultiCraft multiCraft = new MultiCraft();
        Category category = multiCraft.getCategory("scrapbook~albums~refills");

        String[] firsLines ;

        Path filePath = Paths.get("sample/product_template.csv");
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReader(reader)) {

                firsLines = csvReader.readAll().get(0);

            }
        }


        Path csvImporPah = Paths.get("sample/product_import.csv");

        if (csvImporPah.toFile().exists()) {
            csvImporPah.toFile().delete();
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvImporPah.toString()))) {
            writer.writeAll(Collections.singleton(firsLines));


            for (Product product: category.products()) {

                String[] newLine = List.of(product.code(), product.title(), product.description()).toArray(new String[0]);
                writer.writeAll(Collections.singleton(newLine));
            }

        }





    }

}