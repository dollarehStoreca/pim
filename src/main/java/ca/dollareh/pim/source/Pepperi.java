package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Pepperi extends ProductSource {

    final Logger logger = LoggerFactory.getLogger(Pepperi.class);

    public static final String BASE_URL = "https://webapi.pepperi.com/17.41.3/webapi/Service1.svc/v1";
    private final HttpClient client ;

    private final String transactionId ;
    private final String authToken ;
    private final String sessionToken ;

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
            List<String> products = getProducts();

            products.stream().parallel().forEach(uuid -> {
                try {
                    getProduct(uuid);
                } catch (IOException | InterruptedException e) {
                    logger.error("Unable to get Product", e);
                }
            });



            // System.out.println(products);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getProducts() throws IOException, InterruptedException {

        JsonNode productNode = getProdutNode("/OrderCenter/Transaction/" +transactionId+"/Items/0/0");

        ArrayNode rowNodes = (ArrayNode) productNode.get("Rows");

        List<String> strings = getProductKeys(rowNodes);

        int totalProducts = productNode.get("TotalRows").asInt();

        productNode = getProdutNode("/OrderCenter/Transaction/" +transactionId+"/Items/1/"+totalProducts);

        rowNodes = (ArrayNode) productNode.get("Rows");

        strings.addAll(getProductKeys(rowNodes));

        System.out.println(totalProducts);

        return strings;

    }

    private void getProduct(String uuid) throws IOException, InterruptedException {
        HttpRequest request = getPepperiRequest("/OrderCenter/Transaction/" +transactionId+ "/TransactionLine/" + uuid + "/Details")
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

        for (JsonNode fieldNode:fieldsNode) {
            if(fieldNode.get( "ApiName").asText().equals("ItemName")) {
                title = fieldNode.get("Value").asText();
            }
            else if(fieldNode.get( "ApiName").asText().equals("ItemUPC")) {
                upc = fieldNode.get("Value").longValue();
            }
            else if(fieldNode.get( "ApiName").asText().equals("ItemCaseQuantity")) {
                quantity = fieldNode.get("Value").asInt();
            }
            else if(fieldNode.get( "ApiName").asText().equals("TSAPPMDiscountUnitPriceAfter1")) {
                price = fieldNode.get("Value").floatValue();
            } else if(fieldNode.get( "ApiName").asText().equals("ItemImages")) {
                String imageUrl = fieldNode.get("Value").asText();
                if(imageUrl.contains(".com")) {
                    imageUrl = imageUrl.substring(imageUrl.indexOf(".com") + 4);
                }

                if(imageUrl.contains("?")) {
                    imageUrl = imageUrl.substring(0, imageUrl.indexOf("?"));
                }
                images = List.of(imageUrl);
            }
            else if(fieldNode.get( "ApiName").asText().equals("ItemMainCategory")) {
                categories.add(fieldNode.get("Value").asText());
            }
            else if(fieldNode.get( "ApiName").asText().equals("ItemTSASecondCategory")) {
                categories.add(fieldNode.get("Value").asText());
            }
            else if(fieldNode.get( "ApiName").asText().equals("ItemTSAThirdCategory")) {
                categories.add(fieldNode.get("Value").asText());
            }
        }

        Product product = new Product(uuid, title, title, upc, quantity,price,price,0f,images.toArray(new String[0]));

        onProductDiscovery(categories, product);


    }

    private static List<String> getProductKeys(ArrayNode rowNodes) {
        List<String> productKeys = new ArrayList<>();

//        System.out.println(rowNodes);
        rowNodes.forEach(rowNode -> {


                productKeys.add((rowNode.get("UID").asText()));

        });


        return productKeys;
    }

    private JsonNode getProdutNode(String url) throws IOException, InterruptedException {
        HttpRequest request = getPepperiRequest(url)
                .POST(HttpRequest.BodyPublishers.ofString("{\"SearchCode\":\"%7b%22OrderID%22%3a%2243d9b745d84e4e54bf71377dc507e148%22%2c%22ViewType%22%3a%22OrderCenterGrid%22%2c%22Top%22%3a100%2c%22TabID%22%3a%22%7b%5c%22JsonFilter%5c%22%3a%5c%22fed05461-1ff0-4a4f-b8bc-2c6a2ae2e8c6%5c%22%7d%22%2c%22OrderBy%22%3a%22%22%2c%22Ascending%22%3atrue%2c%22SearchText%22%3a%22%22%2c%22SmartSearch%22%3a%5b%5d%2c%22SearchAll%22%3afalse%7d\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


        return objectMapper.readTree(response.body());
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
