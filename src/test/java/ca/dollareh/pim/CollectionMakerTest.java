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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CollectionMakerTest {

    final Logger logger = LoggerFactory.getLogger(CollectionMakerTest.class);

    @Test
    void test() throws IOException {

        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                .onNew(newProduct -> {
                    logger.info("New Product Found " + newProduct);
                })
                .onModified(updatedProduct -> {
                    logger.info("Product Modified " + updatedProduct);
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





            }
            i++;
        }

//        for (String category : categories) {
//            logger.info(category);
//        }

        cMap.entrySet().forEach(stringStringEntry -> {

            logger.info("------------------------------------------\n\n");

            logger.info("Shopify Collection : " + stringStringEntry.getKey());

            SortedSet<String> colletionPaths = new TreeSet<>();

            stringStringEntry.getValue().forEach(strings -> {
                String path = strings.stream().collect(Collectors.joining("-"));

                if(!path.trim().isEmpty()) {
                    colletionPaths.add("MultiCraft" + "-" + path);
                }

            });


            logger.info(colletionPaths.stream().toList().toString());




            logger.info("\n\n------------------------------------------");

        });

    }
}
