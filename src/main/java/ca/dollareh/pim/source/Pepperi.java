package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Pepperi extends ProductSource {

    final Logger logger = LoggerFactory.getLogger(Pepperi.class);

    public static final String BASE_URL = "https://webapi.pepperi.com/17.42.1/webapi/Service1.svc/v1";
    private final HttpClient client;

    private final String transactionId;
    private final String authToken;
    private final String sessionToken;

    protected Pepperi(Consumer<Product> newProductConsumer,
                      Consumer<Product> modifiedProductConsumer) {
        super(newProductConsumer, modifiedProductConsumer);

        client = HttpClient.newHttpClient();

        transactionId = System.getenv("PEPPERI_TRANSACTION_ID");
        authToken = System.getenv("PEPPERI_AUTH_TOKEN");
        sessionToken = System.getenv("PEPPERI_SESSION_TOKEN");
    }

    @Override
    protected void login() throws IOException {

    }

    @Override
    protected void logout() throws IOException {

    }

    @Override
    protected void browse() throws IOException, URISyntaxException {
        try {
            getProductsCSV();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void getProductsCSV() throws IOException, InterruptedException {
//        HttpRequest request = getPepperiRequest("/OrderCenter/Transaction/" + transactionId + "/ExportToCsv")
//                .POST(HttpRequest.BodyPublishers.ofString("{\"ViewType\":\"OrderCenterGrid\",\"OrderBy\":\"\",\"Ascending\":true,\"SearchText\":\"\"}"))
//                .build();
//
//        HttpResponse<InputStream> response = null;
//
//        response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        Path csvPath = Paths.get("sample", "Pepperi.csv");
        System.out.println(csvPath);

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
           String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                String sku = data[2].replaceAll("\"", "");

                getProductUID(sku);
            }
        }
    }

    private void getProductUID(String sku) throws IOException {


        HttpRequest request = getPepperiRequest("/OrderCenter/Transaction/" + transactionId + "/Items/SearchAll")
                .POST(HttpRequest.BodyPublishers.ofString("{\"ViewType\":\"OrderCenterView3\",\"Top\":100,\"OrderBy\":\"\",\"Ascending\":true,\"SearchText\":\"" + sku + "\"}"))
                .build();


        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        JsonNode jsonNode = objectMapper.readTree(response.body());

        if (jsonNode.get("Rows") != null) {
            ArrayNode nodes = (ArrayNode) jsonNode.get("Rows");
            try {
                getProduct(nodes.get(0).get("UID").asText(), sku);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void getProduct(String uuid,String sku) throws IOException, InterruptedException {
        HttpRequest request = getPepperiRequest("/OrderCenter/Transaction/" + transactionId + "/TransactionLine/" + uuid + "/Details")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode productNode = objectMapper.readTree(response.body()).get("Data");

        String title = null;
        String description = null;
        List<String> images = null;
        Long upc = null;

        Integer quantity = null;
        Float price = null;

        List<String> categories = new ArrayList<>();

        ArrayNode fieldsNode = (ArrayNode) productNode.get("Fields");

        for (JsonNode fieldNode : fieldsNode) {
            if (fieldNode.get("ApiName").asText().equals("ItemName")) {
                title = fieldNode.get("Value").asText();
            } else if (fieldNode.get("ApiName").asText().equals("ItemUPC")) {
                upc = fieldNode.get("Value").longValue();
            } else if (fieldNode.get("ApiName").asText().equals("ItemCaseQuantity")) {
                quantity = fieldNode.get("Value").asInt();
            } else if (fieldNode.get("ApiName").asText().equals("TSAPPMDiscountUnitPriceAfter1")) {
                String value = String.valueOf(fieldNode.get("Value"));
                String newValue = value.replaceAll("^\"|\"$", "");
                price = Float.parseFloat(newValue);
            } else if (fieldNode.get("ApiName").asText().equals("ItemImages")) {
                String imageUrl = fieldNode.get("Value").asText();
                if (imageUrl.contains(".com")) {
                    imageUrl = imageUrl.substring(imageUrl.indexOf(".com") + 4);
                }

                if (imageUrl.contains("?")) {
                    imageUrl = imageUrl.substring(0, imageUrl.indexOf("?"));
                }
                images = List.of(imageUrl);
            } else if (fieldNode.get("ApiName").asText().equals("ItemMainCategory")) {
                categories.add(fieldNode.get("Value").asText());
            } else if (fieldNode.get("ApiName").asText().equals("ItemTSASecondCategory")) {
                categories.add(fieldNode.get("Value").asText());
            } else if (fieldNode.get("ApiName").asText().equals("ItemTSAThirdCategory")) {
                categories.add(fieldNode.get("Value").asText());
            }
        }

        Product product = new Product("UNQ-"+sku, title, title, upc, quantity, price, price, 0f, images.toArray(new String[0]));

        onProductDiscovery(categories, product);

    }

    private HttpRequest.Builder getPepperiRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + url))
                .setHeader("Authorization", authToken)
                .setHeader("Content-Type", "application/json")
                .setHeader("PepperiSessionToken", sessionToken);
    }

    @Override
    protected void downloadAsset(File imageFile, String assetUrl) throws IOException {

        URL url = new URL("https://cdn.pepperi.com" + assetUrl);
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
