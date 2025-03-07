package ca.dollareh;

import ca.dollareh.integration.Shopify;
import ca.dollareh.vendor.MultiCraft;
import ca.dollareh.vendor.ProductSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class CollectionMakerTest {
    @Test
    void test() throws IOException {

        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                .onNew(newProduct -> {
                    System.out.println("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    System.out.println("Product Modified " + updatedProduct);
                })
                .build();

        Shopify shopify = new Shopify(productSource);

        FileInputStream file = new FileInputStream(Paths.get("sample/Multicraft Final Order June 02, 2024.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SortedSet<String> categories = new TreeSet<>();

        Map<String, String> SubCategories = new HashMap<>();

        int i = 0;
        for (Row row : sheet) {
            if (i != 0) {
                String code = row.getCell(1).getStringCellValue();

                categories.add(row.getCell(3).getStringCellValue().trim());
                categories.add(row.getCell(4).getStringCellValue().trim());

                SubCategories.put(row.getCell(5).getStringCellValue().trim(), code);

            }
            i++;
        }

//        for (String category : categories) {
//            System.out.println(category);
//        }

        SubCategories.entrySet().forEach(stringStringEntry -> {
            if(!stringStringEntry.getKey().trim().isEmpty()) {

                System.out.println(stringStringEntry);

                System.out.println(productSource.getCollection(stringStringEntry.getValue()));
            }
        });

    }
}
