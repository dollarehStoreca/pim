package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Conglom extends ProductSource{

    public static final String URL = "https://media.conglom.com";

    protected Conglom(Consumer<Product> newProductConsumer, Consumer<Product> modifiedProductConsumer) {
        super(newProductConsumer, modifiedProductConsumer);
    }

    @Override
    protected void login() throws IOException {

    }

    @Override
    protected void logout() throws IOException {

    }

    @Override
    protected void browse() throws IOException, URISyntaxException {
        Map<String, List<String>> imageUrlsByItem = getImageUrlsByItem();
//        System.exit(0);

        Map<String, Float> priceMap = getPriceMap();

        FileInputStream file = new FileInputStream(Paths.get("sample/conglom/Retail Image Link Catalogue.xlsx").toFile());
        Workbook workbook = new XSSFWorkbook(file);

        Sheet sheet = workbook.getSheetAt(0);

        Iterator<Row> rows = sheet.rowIterator();

        rows.next();
        rows.next();

        while (rows.hasNext()) {
            Row row = rows.next();
            String code = row.getCell(0).getStringCellValue().trim();
            if(code.isEmpty()) {
                break;
            }

            Float price = priceMap.get(code);

            if(price != null) {

//                String imageURL = row.getCell(4).getStringCellValue().trim().replace(URL, "");

                // Find Actual Images
//                String imageURLIP = imageURL.replaceFirst("_CASE", "_IP");
//                String imageURLSP = imageURLIP.replaceFirst("_IP", "_SP");


                Product product =new Product(code,
                        row.getCell(1).getStringCellValue().trim(),
                        row.getCell(1).getStringCellValue().trim(),null, null,
                        price, price, null,
                        imageUrlsByItem.get(code) == null ? new String[0] : imageUrlsByItem.get(code).toArray(new String[0]));
//                        List.of(imageURLIP,
//                                imageURLSP).toArray(new String[0]));
                onProductDiscovery(List.of(row.getCell(2).getStringCellValue().trim()),product);
            }

        }

    }

    private static Map<String, Float> getPriceMap() throws IOException {
        Map<String, Float> priceMap = new HashMap<>();
        File file = new File("sample/conglom/Conglom EASTGEN Price_September 5th 2024.pdf");
        try (PDDocument document = Loader.loadPDF(file)) {
            ObjectExtractor extractor = new ObjectExtractor(document);
            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                Page page = extractor.extract(i + 1);
                List<Table> tables = sea.extract(page);
                for (Table table : tables) {
                    int rowCount = table.getRowCount();

                    for (int rowNumber = 0; rowNumber < rowCount; rowNumber++) {
                        String code = table.getCell(rowNumber,0).getText().trim();
                        if(!code.isEmpty()) {

                            String price = table.getCell(rowNumber,4).getText().trim().replaceAll(" ","");
                            if(price.startsWith("$")) {

                                price = price.substring(1);
                                priceMap.put(code, Float.parseFloat(price));
                            }
                        }
                    }
                }
            }
        }
        return priceMap;
    }

    @Override
    protected void downloadAsset(File imageFile, String assetUrl) throws IOException {
        URL url = new URL(URL + assetUrl);
        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream(imageFile);
        byte[] buffer = new byte[2048];

        int length = 0;

        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();
    }

    private Map<String, List<String>> getImageUrlsByItem() throws IOException {

        Map<String, List<String>> imageUrlMap = new HashMap<>();
        File file = new File("sample/conglom/Conglom-Catalogue.pdf");

        try (PDDocument document = Loader.loadPDF(file)) {
            ObjectExtractor extractor = new ObjectExtractor(document);
            BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                Page page = extractor.extract(i + 1);
                List<Table> tables = bea.extract(page);

                for (Table table : tables) {
                    int rowCount = table.getRowCount();

                    for (int rowNumber = 2; rowNumber < rowCount; rowNumber++) {
                        String code = table.getCell(rowNumber, 0).getText().trim();

                        if (!code.isEmpty()) {
                            String imageUrl = table.getCell(rowNumber, 3).getText().trim().replaceAll(" ", "");

                            /*
                             * sometimes the column index 3 is not giving image url for few rows, changing col index to 5 gets the image url.
                             */
                            if(StringUtil.isBlank(imageUrl) || !imageUrl.startsWith("https")) {
                                imageUrl = table.getCell(rowNumber, 5).getText().trim().replaceAll(" ", "");
                            }
                            if(!StringUtil.isBlank(imageUrl) && imageUrl.startsWith("https") && !imageUrl.contains("_CASE")){
                                if(imageUrl.contains(".com")) {
                                    imageUrl = imageUrl.substring(imageUrl.indexOf(".com") + 4);
                                }

                                if(imageUrl.contains("?")) {
                                    imageUrl = imageUrl.substring(0, imageUrl.indexOf("?"));
                                }
                                imageUrlMap.computeIfAbsent(code, k -> new ArrayList<>()).add(imageUrl);
                            }
                        }
                    }
                }
            }
        }
        imageUrlMap.values().stream().filter(value -> value.size() > 2).forEach(System.out::println);

        return imageUrlMap;
    }
}
