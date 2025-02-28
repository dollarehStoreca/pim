package ca.dollareh.vendor;

import ca.dollareh.ProductSource;
import ca.dollareh.core.model.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Pepperi implements ProductSource {
    @Override
    public void forEach(final Consumer<Product> productConsumer) throws URISyntaxException, IOException, InterruptedException {
        String baseUrl = "https://webapi.pepperi.com/17.41.3/webapi/Service1.svc/v1/OrderCenter/Transaction/4b7e4afa89184584ae9fed08ec923125/Items/";

        Map<String, Object> map = getPage(baseUrl + "0/0");

        int totalRecords = (int) map.get("TotalRows");

        List<Map<String, Object>> results = (List<Map<String, Object>>) map.get("Rows");

        System.out.println(totalRecords);

        results.addAll((Collection<? extends Map<String, Object>>) getPage(baseUrl + "1/" + totalRecords).get("Rows"));

        results.stream().parallel().forEach(result -> {
            try {
                onProductDiscovery(productConsumer, getProduct(result));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void downloadAssets(final Product product) throws IOException {

    }

    @Override
    public void logout() throws IOException {

    }

    private Product getProduct(Map<String, Object> result) {

        List<Map<String, Object>> fields = (List<Map<String, Object>>) result.get("Fields");

        return new Product(null,
                ((Map<String, Object>)fields.get(2)).get("FormattedValue").toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }


    private static Map<String, Object> getPage(final String baseUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Authorization",System.getenv("PEPPERI_AUTHORIZATION"))
                .header("PepperiSessionToken",System.getenv("PEPPERI_SESSION"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                            "TransactionUID":"4b7e4afa89184584ae9fed08ec923125",
                            "SearchCode":"%7b%22OrderID%22%3a%224b7e4afa89184584ae9fed08ec923125%22%2c%22ViewType%22%3a%22OrderCenterGrid%22%2c%22Top%22%3a100%2c%22TabID%22%3a%22%7b%5c%22JsonFilter%5c%22%3a%5c%22fed05461-1ff0-4a4f-b8bc-2c6a2ae2e8c6%5c%22%7d%22%2c%22OrderBy%22%3a%22%22%2c%22Ascending%22%3atrue%2c%22SearchText%22%3a%22%22%2c%22SmartSearch%22%3a%5b%5d%2c%22SearchAll%22%3afalse%7d",
                            "IndexStart":0,
                            "IndexEnd":1
                        }
                        """))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body()
                , new TypeReference<>() {});
    }
}
