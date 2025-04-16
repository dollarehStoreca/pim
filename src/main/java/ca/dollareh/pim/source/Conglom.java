package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.Table;
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
            System.out.println(code);

            Float price = priceMap.get(code);

            if(price != null) {

                String imageURL = row.getCell(4).getStringCellValue().trim().replace(URL, "");

                Product product =new Product(code,
                        row.getCell(1).getStringCellValue().trim(),
                        row.getCell(1).getStringCellValue().trim(),null, null,
                        price, price, null,
                        List.of(imageURL.replaceFirst("_CASE", "_IP"),
                                imageURL.replaceFirst("_CASE", "_SP")).toArray(new String[0]));
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
}
