package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;

public class Symaksales extends ProductSource {

    public static final String BASE_URL = "https://symaksales.com/";

    private final Connection session;
    private final List<Product> collectedProducts = new ArrayList<>();
    private final Map<String, String> cookies = new HashMap<>();

    protected Symaksales(Consumer<Product> newProductConsumer,
                         Consumer<Product> modifiedProductConsumer) {
        super(newProductConsumer, modifiedProductConsumer);
        session = Jsoup.newSession()
                .timeout(90_000)
                .maxBodySize(5 * 1024 * 1024)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Connection", "close");

        // Read cookies from system properties (set these in your IntelliJ Run config)
        String aspNetSessionId = System.getenv("ASP_NET_SessionId");
        if (aspNetSessionId != null && !aspNetSessionId.isBlank()) {
            session.cookie("ASP.NET_SessionId", aspNetSessionId);
            System.out.println("Injected ASP.NET_SessionId cookie: " + aspNetSessionId);
        }

    }

    @Override
    protected void login() throws IOException {}

    @Override
    protected void logout() throws IOException {}

    @Override
    protected void browse() throws IOException, URISyntaxException {
        collectedProducts.clear();
        browse("/Products");
        saveProductsToCSV(new File("products.csv"));
        System.out.println("Total Products collected: " + collectedProducts.size());
    }

    private void browse(String url) throws IOException, URISyntaxException {
        collectedProducts.clear();

        // Start page index from 1 if URL is just "/Products"
        int currentPage = 1;
        boolean hasNextPage = true;

        while (hasNextPage) {
            String pageUrl;

            if (url.endsWith("/")) {
                pageUrl = BASE_URL + url.substring(1) + "Page/" + currentPage + "/";
            } else if (url.equalsIgnoreCase("/Products")) {
                // Special case first page
                pageUrl = BASE_URL + "Products/Page/" + currentPage + "/";
            } else {
                // fallback to the passed URL
                pageUrl = BASE_URL + url.substring(1);
            }

            System.out.println("Fetching page URL: " + pageUrl);
            Document doc = getHTMLDocument(pageUrl);

            Element productsContainer = doc.selectFirst("#ctl00_ContentPlaceHolder1_dtlProducts");

            if (productsContainer == null) {
                System.out.println("Products container not found on page " + currentPage + " - stopping.");
                break;
            }

            Elements productSpans = productsContainer.select("> span");
            System.out.println("DEBUG: Found " + productSpans.size() + " product spans on the page " + currentPage);

            for (Element productSpan : productSpans) {
                String sku = productSpan.select("div.ProductListItem.SKU span").text().trim();
                if (sku.isEmpty()) continue;

                String name = productSpan.select("a.ProductListItemDesc").text().trim();
                String imageUrl = productSpan.select("img").attr("src").trim();

                Element priceSpan = productSpan.selectFirst("div[class*=Price] > span");
                String listPrice = "";
                if (priceSpan != null) {
                    listPrice = priceSpan.text().trim();
                }
                Float price = parseFloatSafe(listPrice);

                Element qtyInput = productSpan.selectFirst("div.ProductListItem.QTY input");
                String qtyStr = qtyInput != null ? qtyInput.attr("value").trim() : "";
                Integer quantity = null;
                try {
                    quantity = Integer.parseInt(qtyStr);
                } catch (Exception e) {
                    quantity = null;
                }

                String upc = "";
                String description = "";
                Element popupDiv = productSpan.selectFirst("div#Product_" + sku);
                if (popupDiv != null) {
                    Elements modalDescriptions = popupDiv.select("div.modal_description");
                    for (Element desc : modalDescriptions) {
                        Element strong = desc.selectFirst("strong");
                        if (strong != null) {
                            String label = strong.text().trim();
                            if (label.equalsIgnoreCase("UPC #:")) {
                                Element upcDiv = desc.selectFirst("div");
                                if (upcDiv != null) upc = upcDiv.text().trim();
                            }
                            if (label.equalsIgnoreCase("Item Name:")) {
                                Element nameDiv = desc.selectFirst("div");
                                if (nameDiv != null) description = nameDiv.text().trim();
                            }
                        }
                    }
                }

                if (!imageUrl.isEmpty() && imageUrl.startsWith("/")) {
                    imageUrl = BASE_URL + imageUrl;
                }

                Product p = new Product(
                        "SYM-"+sku,
                        name,
                        description,
                        upc.isEmpty() ? null : parseLongSafe(upc),
                        quantity,
                        price,
                        price,
                        0.0f,
                        new String[]{imageUrl}
                );

                onProductDiscovery(new ArrayList<>(), p);

            }

            // Check if there is a next page link on this page; if not, stop
            Element nextPageElement = doc.selectFirst("#ctl00_ContentPlaceHolder1_lnkNextTop");
            if (nextPageElement == null || nextPageElement.attr("href").isEmpty()) {
                System.out.println("No 'Next' page link found on page " + currentPage + " - ending pagination.");
                hasNextPage = false;
            } else {
                System.out.println("Next page exists: " + nextPageElement.attr("href"));
                currentPage++;
            }
        }
    }

    /**
     * Helper method to sanitize asset URLs into safe filenames for storage.
     */
    private String sanitizeAssetUrlToFilename(String assetUrl) {
        // Remove protocol and domain (e.g. https://symaksales.com/)
        String safePath = assetUrl;
        if (assetUrl.startsWith("http://") || assetUrl.startsWith("https://")) {
            int idx = assetUrl.indexOf("/", assetUrl.indexOf("://") + 3);
            if (idx != -1) {
                safePath = assetUrl.substring(idx + 1);  // E.g. "Images/ProductsLg/k0395.jpg"
            } else {
                safePath = "unknown_asset";
            }
        }
        // Replace illegal characters in filenames for Windows (:\*?"<>|)
        safePath = safePath.replaceAll("[\\\\:*?\"<>|]", "_");
        return safePath;
    }

    /**
     * Overrides the method to get a safe File for storing assets.
     */
    private static final File ASSETS_DIR = new File("workspace/extracted/Symaksales/assets");

    @Override
    public File getAssetFile(final String assetUrl) {
        String safeFilename = sanitizeAssetUrlToFilename(assetUrl);
        return new File(ASSETS_DIR, safeFilename);
    }


    @Override
    protected void downloadAsset(File ignored, String assetUrl) throws IOException {
        File imageFile = getAssetFile(assetUrl);
        imageFile.getParentFile().mkdirs();

        String fullUrl = assetUrl.startsWith("http") ? assetUrl :
                BASE_URL + (assetUrl.startsWith("/") ? assetUrl.substring(1) : assetUrl);

        Connection.Response resultImageResponse = session
                .newRequest(fullUrl)
                .ignoreContentType(true)
                .cookies(cookies)
                .execute();

        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            out.write(resultImageResponse.bodyAsBytes());
        }
    }

    private Document getHTMLDocument(final String url) throws IOException {
        String fullUrl = url.startsWith("http") ? url : BASE_URL + (url.startsWith("/") ? url.substring(1) : url);

        int retries = 3;
        IOException lastException = null;

        while (retries-- > 0) {
            try {
                return session
                        .newRequest(fullUrl)
                        .cookies(cookies)
                        .get();
            } catch (java.net.SocketException e) {
                lastException = e;
                System.out.println("Connection issue: " + e.getMessage() + " — retrying...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
            }
        }
        throw lastException;
    }

    private void saveProductsToCSV(File outputFile) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null) parent.mkdirs();

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(new String[]{
                    "Code", "Title", "Description", "UPC", "InventoryQuantity",
                    "Cost", "Price", "Discount", "ImageUrls"
            });

            for (Product p : collectedProducts) {
                writer.writeNext(new String[]{
                        safeString(p.code()),
                        safeString(p.title()),
                        safeString(p.description()),
                        p.upc() == null ? "" : p.upc().toString(),
                        p.inventoryQuantity() == null ? "" : p.inventoryQuantity().toString(),
                        p.cost() == null ? "" : p.cost().toString(),
                        p.price() == null ? "" : p.price().toString(),
                        p.discount() == null ? "" : p.discount().toString(),
                        p.imageUrls() == null ? "" : String.join("|", p.imageUrls())
                });
            }
        }
    }

    private static String safeString(String s) {
        return s == null ? "" : s;
    }

    private static Float parseFloatSafe(String text) {
        if (text == null || text.isBlank()) return 0f;
        try {
            String cleaned = text.replaceAll("[^0-9.]", "");
            if (cleaned.isBlank()) return 0f;
            return Float.parseFloat(cleaned);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static Long parseLongSafe(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            String cleaned = text.replaceAll("\\D", "");
            if (cleaned.isBlank()) return null;
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
