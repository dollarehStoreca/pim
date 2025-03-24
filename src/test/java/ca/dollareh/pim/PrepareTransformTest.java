package ca.dollareh.pim;

import ca.dollareh.pim.source.Pepperi;
import ca.dollareh.pim.source.ProductSource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PrepareTransformTest {

    @Test
    void testPrepare() throws IOException {

        final Class<? extends ProductSource> productSourceClass = Pepperi.class;

        File extractedFolder = new File("workspace/extracted/" + productSourceClass.getSimpleName());
        File transformFolder = new File("workspace/transform/" + productSourceClass.getSimpleName());

        transformFolder.mkdirs();

        File[] jsonFiles = extractedFolder.listFiles((dir, name) -> name.endsWith(".json"));

        for (File jsonFile: jsonFiles) {
            System.out.println(jsonFile.getAbsolutePath());

            String fileName = jsonFile.getName();

            fileName = fileName.substring(0, fileName.lastIndexOf("-"));
            fileName = fileName.substring(0, fileName.lastIndexOf("-"));
            fileName = fileName.substring(0, fileName.lastIndexOf("-"));

            File newJsonFile = new File(transformFolder, fileName + ".json");

            if(!newJsonFile.exists()) {
                Files.writeString(newJsonFile.toPath(), "{\"price\": 10}");
            }
        }
    }

}
