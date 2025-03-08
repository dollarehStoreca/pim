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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AssociateProductsTest {

    final File propFile = new File("workspace/export/Shopify/MultiCraft/product-mapping.properties");

    @Test
    void testBuildAssociations() throws IOException, InterruptedException {
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

        Map<Long, Map<String, Object>> cMap = shopify.getShopifyCollection();

        Map<String, Long> colleionManeMap = new HashMap<>();

        cMap.entrySet().forEach(longMapEntry -> {
            colleionManeMap.put((String) longMapEntry.getValue().get("title"),longMapEntry.getKey() );
        });

        Long craftId = colleionManeMap.get("Craft");

        Properties properties = new Properties();
        properties.load(new FileReader(propFile));


        FileInputStream file = new FileInputStream(Paths.get("sample/Multicraft Final Order June 02, 2024.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


        String subSubCategory = "";

        int i = 0;
        String subSubCategoryValue;

        String categoryValue ;

        String subCategoryValue ;

        for (Row row : sheet) {
            if (i != 0) {
                String code = row.getCell(1).getStringCellValue().trim();



                subCategoryValue = row.getCell(4).getStringCellValue().trim();

                subSubCategoryValue = row.getCell(5).getStringCellValue().trim();

                String productId = (String) properties.get(code);

                if(productId != null) {
                    System.out.println(code);
                    System.out.println(productId);
                    System.out.println("Collctions");

                    System.out.println(craftId);

                    shopify.associateCollection(Long.valueOf(productId), craftId.toString());

                    Long cId = colleionManeMap.get(subCategoryValue);

                    if(cId != null) {
                        shopify.associateCollection(Long.valueOf(productId), cId.toString());
                    }

                    cId = colleionManeMap.get(subSubCategoryValue);

                    if(cId != null) {
                        shopify.associateCollection(Long.valueOf(productId), cId.toString());
                    }
                }

            }
            i++;
        }

    }

}
