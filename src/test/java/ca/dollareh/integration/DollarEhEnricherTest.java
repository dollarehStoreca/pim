package ca.dollareh.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DollarEhEnricherTest {

    @Test
    void enrichMultoCraft() throws IOException {

        FileInputStream file = new FileInputStream(Paths.get("sample/Multicraft Final Order June 02, 2024.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        Map<String, Object> productMap ;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        int i = 0;
        for (Row row : sheet) {
            if (i != 0) {
                String code = row.getCell(1).getStringCellValue();
                productMap = new HashMap<>();
                for (Cell cell : row) {
                    if (cell.getColumnIndex() == 6) {
                        productMap.put("title", cell.getStringCellValue());
                    } else if (cell.getColumnIndex() == 7) {
                        productMap.put("description", cell.getStringCellValue());
                    } else if (cell.getColumnIndex() == 9) {
                        productMap.put("inventryCode", cell.getNumericCellValue());
                    } else if (cell.getColumnIndex() == 10) {
                        productMap.put("discount", cell.getNumericCellValue());
                    } else if (cell.getColumnIndex() == 11) {
                        productMap.put("price", cell.getNumericCellValue());
                    }
                }


                Path jsonPath = Path.of("workspace/MultiCraft/" + code + ".json");

                Files.writeString(jsonPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(productMap));

            }
            i++;
        }


    }
}
