package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class Ctg extends ProductSource {

    public static final String BASE_URL = "https://www.ctgbrands.com";

    private final Connection session;

    protected Ctg(Consumer<Product> newProductConsumer,
                  Consumer<Product> modifiedProductConsumer) {
        super(newProductConsumer, modifiedProductConsumer);
        session = Jsoup.newSession()
                .timeout(45 * 1000)
                .maxBodySize(5 * 1024 * 1024);
    }

    @Override
    protected void login() throws IOException {
        logger.info("Login skipped; using cookies for API authentication");
    }

    @Override
    protected void logout() throws IOException {
        logger.info("Logout skipped; API session is stateless with cookies");
    }

    @Override
    protected void browse() throws IOException {
        fetchCartProducts(); // Only fetching products from the cart
    }

    private void fetchCartProducts() throws IOException {
        String cartApiUrl = BASE_URL + "/service/QueryProducts.json" +
                "?CartId=1449388&IncludeInactive=true&OnlyCartItems=1449388&Take=294&op=HydrateCartProducts";

        Connection.Response response = session
                .newRequest(cartApiUrl)
                .header("cookie", "ss-opt=perm; X-UAId=2876; userEmail=twonies.store%40gmail.com; fullName=sayvingsmart%20gmail.%20com; ss-id=4l5CTbW2nuL4WLV40NkH; ss-pid=xJjGGIpFncd6OgY9z3Yf; yotpo_pixel=46cbe683-ef8d-4eae-aa22-fc8159065a92; _sp_ses.0313=*; _sp_id.0313=32084bbb8d3d54c7.1754393194.12.1756813294.1756799000; cartId=1449388")
                .ignoreContentType(true)
                .execute();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.body());
        JsonNode resultsNode = rootNode.path("results");

        if (!resultsNode.isArray() || resultsNode.size() == 0) {
            logger.warn("Cart products node is missing or empty");
            return;
        }

        for (JsonNode productNode : resultsNode) {
            try {
                String productCode = productNode.path("manufacturerNumber").asText();
                String title = productNode.path("shortDescription").asText();

                long upc = Long.parseLong(productNode.path("upcCode").asText());
                String upcStr = String.valueOf(upc);
                if (upcStr.length() < 12) {
                    upcStr = "0".repeat(12 - upcStr.length()) + upcStr;
                }


                Integer inventory = productNode.path("minimumOrderQty").asInt();
                Float cost = productNode.path("cost").isMissingNode() ? 0f :
                        (float) productNode.path("cost").asDouble();
                Float price = productNode.path("price").isMissingNode() ? 0f :
                        (float) productNode.path("price").asDouble();
                Float discount = null; // Optional

                // Description fallback
                String description = productNode.path("longDescription").asText();
                if (description.isEmpty()) {
                    description = productNode.path("shortDescription").asText();
                }

                // Fetch actual product images from product page
                JsonNode imagesNode = productNode.path("productImages");
                String[] images = extractImagesFromApiOrPage(productNode, productCode);

                Product product = new Product(
                        "CTG-" + productCode,
                        title,
                        description,
                        upc,
                        inventory,
                        cost,
                        price,
                        discount,
                        images
                );

                // All cart products belong to "Cart" category
                List<String> categories = List.of("Cart");
                onProductDiscovery(categories, product);

            } catch (Exception e) {
                logger.warn("Error processing product node: {}", productNode, e);
            }
        }
    }
    private String[] fetchProductImages(String productCode) throws IOException {
        Document productPage;
        try {
            productPage = session.newRequest(BASE_URL + "/shop/" + productCode).get();
        } catch (IOException e) {
            logger.warn("Failed to fetch product page for {}: {}", productCode, e.getMessage());
            return new String[0];
        }

        // Grab all <img> tags
        Elements imageElements = productPage.select("img");

        String[] images = imageElements.stream()
                .map(el -> {
                    String url = el.attr("ng-src");
                    if (url.isEmpty()) url = el.attr("data-src");
                    if (url.isEmpty()) url = el.attr("src");
                    return url;
                })
                .filter(url -> !url.isEmpty())
                .toArray(String[]::new);

        if (images.length == 0) {
            logger.warn("No images found on product page for {}", productCode);
        }

        return images;
    }
    private String[] extractImagesFromApiOrPage(JsonNode productNode, String productCode) throws IOException {
        JsonNode imagesNode = productNode.path("productImages");

        if (imagesNode.isArray() && imagesNode.size() > 0) {
            String[] images = new String[imagesNode.size()];

            for (int i = 0; i < imagesNode.size(); i++) {
                JsonNode img = imagesNode.get(i);
                String url = "";

                // Prefer API-provided imageUrl if available
                if (!img.path("imageUrl").asText("").isEmpty()) {
                    url = img.path("imageUrl").asText();
                }
                // Otherwise construct from productId
                else if (!img.path("productId").asText("").isEmpty()) {
                    String id = img.path("productId").asText();
                    url = "https://s3.amazonaws.com/emuncloud-staticassets/productImages/ctg080/large/" + id + ".jpg";
                }

                // Clean URL (optional)
                int qIdx = url.indexOf('?');
                images[i] = qIdx > 0 ? url.substring(0, qIdx) : url;
            }

            // If images found via API, return them
            if (images.length > 0) return images;
        }

        // Fallback: scrape product page
        return fetchProductImages(productCode);
    }



    @Override
    protected void downloadAsset(File imageFile, String assetUrl) throws IOException {
        imageFile.getParentFile().mkdirs();
        Connection.Response resultImageResponse = session
                .newRequest(assetUrl.startsWith("http") ? assetUrl : BASE_URL + assetUrl)
                .ignoreContentType(true)
                .execute();
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            out.write(resultImageResponse.bodyAsBytes());
        }
    }
}