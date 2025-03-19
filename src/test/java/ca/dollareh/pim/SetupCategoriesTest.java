package ca.dollareh.pim;

import ca.dollareh.pim.integration.Shopify;
import ca.dollareh.pim.source.MultiCraft;
import ca.dollareh.pim.source.ProductSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class SetupCategoriesTest {

    final Logger logger = LoggerFactory.getLogger(SetupCategoriesTest.class);

    @Test
    void testCategoriesSetup() throws IOException, InterruptedException {

        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                .build();

        Shopify shopify = new Shopify(productSource);

        List<Map<String,Object>> collections = shopify.getShopifyCollection();

        // Convert List<Map<String, Object>> to Map<Long, String>
        Map<String, Long> collectionsMap = collections.stream()
                .filter(map -> map.containsKey("id") && map.containsKey("title")) // Ensure required keys exist
                .collect(Collectors.toMap(
                        map -> (String) map.get("title"), // Extract title
                        map -> ((Number) map.get("id")).longValue(), // Convert id to Long
                        (existing, replacement) -> replacement // Keep the latest title if ID is duplicated
                ));

        FileInputStream file = new FileInputStream(Paths.get("sample/Multicraft.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        Iterator<Row> rows = sheet.rowIterator();

        rows.next();

        while (rows.hasNext()) {
            Row row = rows.next();
            String code = row.getCell(1).getStringCellValue().trim();
            if(code.isEmpty()) {
                break;
            }

            File productFile = shopify.getProductFile(code);

            if(productFile.exists()) {
                Long productId = shopify.getProductId(productFile);

                Long subCategoryValue = collectionsMap.get(row.getCell(4).getStringCellValue().trim());

                Long subSubCategoryValue = collectionsMap.get(row.getCell(5).getStringCellValue().trim());

                shopify.associateCollection(productId, subCategoryValue);
                shopify.associateCollection(productId, subSubCategoryValue);

                logger.info(row.getRowNum() + " : " + code + " : " + productId+ " : " + subCategoryValue+ " : " + subSubCategoryValue);
            } else {
                // logger.error("Product {} not available", code);
            }


        }

    }

}
