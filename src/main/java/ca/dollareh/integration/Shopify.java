package ca.dollareh.integration;

import ca.dollareh.ProductSource;
import ca.dollareh.core.model.Category;
import ca.dollareh.core.model.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shopify {


    final Logger logger = LoggerFactory.getLogger(Shopify.class);

    private static final String[] headers = new String[]{
            "Handle","Title","Body (HTML)"
            ,"Vendor","Product Category","Type","Tags"
            ,"Published","Option1 Name","Option1 Value"
            ,"Option2 Name","Option2 Value","Option3 Name"
            ,"Option3 Value","Variant SKU","Variant Grams"
            ,"Variant Inventory Tracker","Variant Inventory Qty"
            ,"Variant Inventory Policy","Variant Fulfillment Service"
            ,"Variant Price","Variant Compare At Price","Variant Requires Shipping"
            ,"Variant Taxable","Variant Barcode","Image Src","Image Position"
            ,"Image Alt Text","Gift Card","SEO Title"
            ,"SEO Description","Google Shopping / Google Product Category"
            ,"Google Shopping / Gender","Google Shopping / Age Group"
            ,"Google Shopping / MPN","Google Shopping / AdWords Grouping"
            ,"Google Shopping / AdWords Labels","Google Shopping / Condition"
            ,"Google Shopping / Custom Product","Google Shopping / Custom Label 0"
            ,"Google Shopping / Custom Label 1","Google Shopping / Custom Label 2"
            ,"Google Shopping / Custom Label 3","Google Shopping / Custom Label 4"
            ,"Variant Image","Variant Weight Unit","Variant Tax Code"
            ,"Cost per item","Price / International"
            ,"Compare At Price / International","Status","Collection"
            ,"custom.box_quantity","UPC"};

    private final ProductSource productSource;

    public Shopify(ProductSource productSource) {
        this.productSource = productSource;
    }

    void downloadCSV(final Path csvPath) throws IOException, URISyntaxException {

        // Delete CSV If Exists
        if (csvPath.toFile().exists()) {
            csvPath.toFile().delete();
        } else {
            csvPath.toFile().getParentFile().mkdirs();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


        try (CSVWriter writer = new CSVWriter(new FileWriter(csvPath.toString()))) {
            writer.writeAll(Collections.singleton(headers));

            productSource.forEach(originalProduct -> {

                logger.info("Product Received {}" , originalProduct.code());

                Path jsonPath = Path.of("workspace/transform/" + productSource.getClass().getSimpleName());

                Optional<File> enrichedProductFile = findFile(jsonPath, originalProduct.code() + ".json");

                if(enrichedProductFile.isPresent()) {
                    try {
                        writeProduct(writer, originalProduct.merge(objectMapper
                                    .readValue(enrichedProductFile.get(), Product.class)));
                        productSource.downloadAssets(originalProduct);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            productSource.logout();
        }
    }

    private static Optional<File> findFile(Path rootFolderPath, String fileName) {
        if (!Files.exists(rootFolderPath) || !Files.isDirectory(rootFolderPath)) {
            return Optional.empty();
        }

        try (Stream<Path> paths = Files.walk(rootFolderPath)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .map(Path::toFile)
                    .findFirst();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static void writeProduct(CSVWriter writer, Product product) {
        String[] newLine = new String[headers.length];
        newLine[0] = product.code();
        newLine[1] = product.title();
        newLine[2] = product.description();
        newLine[3] = "Dollareh";

        Category category = product.categories().get(0);

        newLine[4] = category.code();
        if(category.parent() != null) {
            newLine[5] = category.parent().code();
        }

        newLine[6] = "Imported";
        newLine[7] = "TRUE";

        newLine[16] = "shopify";

        newLine[17] = "" + product.inventryCode();

        newLine[18] = "deny";

        newLine[19] = "manual";

        newLine[20] = "" + product.price();

        newLine[22] = "TRUE";
        newLine[23] = "TRUE";

        newLine[25] = product.imageUrls()[0];

        newLine[45] = "g";
        newLine[50] = "active";

        newLine[51] = category.code();

        newLine[53] = "" + product.upc();

        writer.writeAll(Collections.singleton(newLine));

        // Cleanup Column Values
        Arrays.fill(newLine, "");

        if(product.imageUrls().length >= 2 ) {
            for (int i = 1; i < 2; i++) {
                newLine[0] = product.code();
                newLine[25] = product.imageUrls()[i];
                writer.writeAll(Collections.singleton(newLine));
            }
        }
    }

}
