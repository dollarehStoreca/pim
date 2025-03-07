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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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


        Map<String, List<List<String>>> cMap = new HashMap<>();

        String subSubCategory = "";

        int i = 0;
        String newValue;

        String categoryValue ;

        String subCategoryValue ;

        for (Row row : sheet) {
            if (i != 0) {
                String code = row.getCell(1).getStringCellValue().trim();

                categoryValue = row.getCell(3).getStringCellValue().trim();
                subCategoryValue = row.getCell(4).getStringCellValue().trim();

                newValue = row.getCell(5).getStringCellValue().trim();

                if(!newValue.isEmpty() && !newValue.equals(subSubCategory)) {

                    List<List<String>> categories = productSource.getCollection(code);
                    cMap.put(newValue, categories);

                    List<List<String>> subCtegories = new ArrayList<>(categories);
                    subCtegories = subCtegories.stream().map(strings -> {
                        List<String> allExceptLast = new ArrayList<>();
                        for (int j = 0; j < strings.size() -1; j++) {
                            allExceptLast.add(strings.get(j));
                        }
                        return allExceptLast;
                    }).toList();

                    if(cMap.get(subCategoryValue) == null) {
                        cMap.put(subCategoryValue, subCtegories);
                    } else {
                        List<List<String>> consolidated = new ArrayList<>(cMap.get(subCategoryValue));
                        consolidated.addAll(subCtegories);
                        cMap.put(subCategoryValue, consolidated);
                    }

                    List<List<String>> rootCtegories = new ArrayList<>(subCtegories);
                    rootCtegories = rootCtegories.stream().map(strings -> {
                        List<String> allExceptLast = new ArrayList<>();
                        for (int j = 0; j < strings.size() -1; j++) {
                            allExceptLast.add(strings.get(j));
                        }
                        return allExceptLast;
                    }).toList();

                    if(cMap.get(categoryValue) == null) {
                        cMap.put(categoryValue, rootCtegories);
                    } else {
                        List<List<String>> consolidated = new ArrayList<>(cMap.get(categoryValue));
                        consolidated.addAll(rootCtegories);
                        cMap.put(categoryValue, consolidated);
                    }




                    subSubCategory = newValue;
                }



            }
            i++;
        }

//        for (String category : categories) {
//            System.out.println(category);
//        }

        cMap.entrySet().forEach(stringStringEntry -> {

            System.out.println("------------------------------------------\n\n");

            System.out.println("Category : " + stringStringEntry.getKey());

            stringStringEntry.getValue().forEach(strings -> {

                System.out.println(strings.stream().collect(Collectors.joining("-")));

            });

            System.out.println("\n\n------------------------------------------");

        });

    }
}
