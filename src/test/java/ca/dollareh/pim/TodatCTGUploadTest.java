package ca.dollareh.pim;

import ca.dollareh.pim.integration.Shopify;
import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.source.Ctg;
import ca.dollareh.pim.source.ProductSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class TodatCTGUploadTest {


    @Test
    void testUpload() throws IOException, InterruptedException {

        List<Product> products = getProductsResul(1438172);

        ProductSource productSource = ProductSource
                .from(Ctg.class)
                .onNew(newProduct -> {

                })
                .onModified(updatedProduct -> {

                })
                .build();


       for (Product product: products) {
           try {
               productSource.downloadAssets(product);
               new Shopify(productSource).create(product);
           } catch (Exception e) {
               System.out.println("Unbable to upload " + product);
           }
       }
    }

    private List<Product> getProductsResul(final int cartId) throws IOException, InterruptedException {

        List<Product> products = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.ctgbrands.com/service/QueryProducts.json?CartId="+cartId+"&IncludeInactive=true&OnlyCartItems="+cartId+"&Take=138&op=HydrateCartProducts"))
                .GET()
                .setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:144.0) Gecko/20100101 Firefox/144.0")
                .setHeader("Accept", "application/json, text/plain, */*")
                .setHeader("Accept-Language", "en-US,en;q=0.5")
                .setHeader("X-emun-referrer", "https://www.ctgbrands.com/shop/cart")
                .setHeader("X-cart-id", "1438172")
                .setHeader("X-Instance-StartedAt", "2025-11-11T04:05:23.233Z")
                .setHeader("X-Instance-StartedEpoch", "1762833923233")
                .setHeader("X-Instance-Id", "7b4bf35f-2c57-4bc8-aa7d-0dc64cd8fde5")
                .setHeader("Referer", "https://www.ctgbrands.com/shop/cart")

                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


        JsonNode jsonNode = new ObjectMapper().readTree(response.body());
        JsonNode rootNode = jsonNode.path("results");
        //  System.out.println(rootNode);

        int total = jsonNode.findValue("total").asInt();

        for (JsonNode productNode : rootNode) {
            products.add(getProduct(productNode));
        }


        return products;
    }

    private Product getProduct(JsonNode productNode) {

        String productCode = productNode.path("manufacturerNumber").asText();
        String productTitle = productNode.path("shortDescription").asText();
        String description = productNode.path("longDescription").asText();
        if (description.isEmpty()) {
            description = productNode.path("shortDescription").asText();
        }
        Long upc = productNode.path("upcCode").asLong();
        Integer inventoryQuantity = productNode.path("minimumOrderQty").asInt();
        Float cost = (float) productNode.path("cost").asDouble();
        Float price = (float) productNode.path("price").asDouble();
        Float discount = null;

        List<String> urls = new ArrayList<>();

        if( productNode.get("productImages").get(0) != null ) {
            String url = "https://s3.amazonaws.com/emuncloud-staticassets/productImages/ctg080/large/" + productNode.get("productImages").get(0).get("productId").asText() + ".jpg";

            urls.add(url);

            for (int i = 2; i < 5 ; i++) {
                String newUrl = url.replaceAll(".jpg", "_"+ i + ".jpg");
                System.out.println(newUrl);

                if(isValidImageUrl(newUrl)) {
                    urls.add(newUrl);
                }
            }
        }


        String[] imageUrls = urls.toArray(new String[urls.size()]);
        // Get all image URLs for this product


        return new Product(productCode, productTitle,
                description, upc, inventoryQuantity,
                cost, price, discount, imageUrls);
    }


    public static boolean isValidImageUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();

            return responseCode == HttpURLConnection.HTTP_OK
                    && contentType != null
                    && contentType.startsWith("image/");
        } catch (Exception e) {
            return false;
        }
    }

}
