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

    final Logger logger = LoggerFactory.getLogger(AssociateProductsTest.class);

    @Test
    void testBuildAssociations() throws IOException, InterruptedException {
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

        Map<Long, Map<String, Object>> cMap = shopify.getShopifyCollection();

        Map<String, Long> colleionManeMap = new HashMap<>();

        cMap.entrySet().forEach(longMapEntry -> {
            colleionManeMap.put((String) longMapEntry.getValue().get("title"),longMapEntry.getKey() );
        });

        Properties properties = new Properties();
        properties.load(new FileReader(propFile));


        FileInputStream file = new FileInputStream(Paths.get("sample/Multicraft.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);




        int i = 0;

        String code, productId ;

        String subCategory ;

        String subSubCategory;



        for (Row row : sheet) {
            if (i != 0) {
                code = row.getCell(1).getStringCellValue().trim();

                productId = (String) properties.get(code);

                if(productId != null) {
                    subCategory = row.getCell(4).getStringCellValue().trim();
                    subSubCategory = row.getCell(5).getStringCellValue().trim();

                    logger.info(code + "\t\t" + colleionManeMap.get(subCategory) + "\t\t" + colleionManeMap.get(subSubCategory) );

                }

            }
            i++;
        }

    }

}
