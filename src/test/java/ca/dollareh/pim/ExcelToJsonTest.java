package ca.dollareh.pim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExcelToJsonTest {

    Logger logger = LoggerFactory.getLogger(ExcelToJsonTest.class);

    @Test
    void buildTransfomation() throws IOException {

        FileInputStream file = new FileInputStream(Paths.get("sample/Multicraft.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        Map<String, Object> productMap;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        List<String> codes = new ArrayList<>();

        File transformFolder = new File("workspace/transform/MultiCraft");
        transformFolder.mkdirs();

        int i = 0;
        for (Row row : sheet) {
            if (i != 0) {
                String code = row.getCell(1).getStringCellValue().trim();
                productMap = new HashMap<>();

                if (code.isEmpty()) {
                    break;
                }

                if(codes.contains(code)) {
                    System.out.println(code);
                }

                codes.add(code);

                System.out.println(i + " : " + code);

                    productMap.put("description", row.getCell(7).getStringCellValue());
                    productMap.put("price", row.getCell(11).getNumericCellValue());

                    Files.writeString(new File(transformFolder,code + ".json").toPath(),
                            objectMapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(productMap));

            }
            i++;
        }


    }
}
