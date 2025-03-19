package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.util.HttpUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MultiCraft extends ProductSource {

    public static final String BASE_URL = "https://multicraft.ca";

    private final Connection session;

    MultiCraft(final Consumer<Product> newProductConsumer,
               final Consumer<Product> modifiedProductConsumer) {
        super(newProductConsumer, modifiedProductConsumer);
        session = Jsoup.newSession()
                .timeout(45 * 1000)
                .maxBodySize(5 * 1024 * 1024);
    }

    @Override
    protected void login() throws IOException {
        logger.info("Logging into Multicraft");
        Document langingPage = session.newRequest(BASE_URL +"/en")
                .get();

        String code = langingPage.selectFirst("input[name=\"__RequestVerificationToken\"]").val();

        session.newRequest(BASE_URL + "/en/user/login")
                .data("UserName", System.getenv("MULTICRAFT_USER"))
                .data("Password", System.getenv("MULTICRAFT_PW"))
                .data("__RequestVerificationToken", code)
                .post();
    }

    @Override
    protected void logout() throws IOException {
        logger.info("Logged out from Multicraft");

        session.newRequest(BASE_URL + "/en/user/logout")
                .get();
    }

    @Override
    protected void browse() throws IOException {
        logger.info("Browsing Multicraft");
        Document document = getHTMLDocument("/en/brand/");

        // Get Sub Categories

        Element ulElement = document.selectFirst("ul.brandsList");
        if(ulElement != null) {
            Elements liElements = ulElement.children();

            liElements.stream().parallel().forEach(liElement -> {
                try {
                    browse(List.of(HttpUtil.getRequestParameter(liElement.selectFirst("a").attr("href"), "code")));
                } catch (IOException | URISyntaxException e) {
                    logger.error("Browsing Failed", e);
                }
            });
        }


    }

    @Override
    protected void downloadAsset(final File imageFile, final String assetUrl) throws IOException {



            imageFile.getParentFile().mkdirs();
            Connection.Response resultImageResponse = session
                    .newRequest(BASE_URL + assetUrl)
                    .ignoreContentType(true)
                    .execute();
            FileOutputStream out = new FileOutputStream(imageFile);
            out.write(resultImageResponse.bodyAsBytes());  // resultImageResponse.body() is where the image's contents are.
            out.close();

    }

    private void browse(final List<String> categories) throws IOException, URISyntaxException {
        logger.info("Browsing brand {}", categories);
        Document subBrandDocument = getHTMLDocument("/en/brand/subbrands?code=" + categories.getLast());

        // Get Products

        Elements skusEls = subBrandDocument.select("#skusCards>li");

        skusEls.stream().parallel().forEach(skusEl -> {
            String productCode = skusEl.selectFirst(".summary-id").text().trim();
            try {
                onProductDiscovery(categories, getProduct(productCode));
            } catch (IOException e) {
                logger.error("Product Not Obtained {}", productCode);
            }
        });


        // Get Sub Categories

        Elements brandsEls = subBrandDocument.select("ul.brandsList>li>a");

        for (Element brandsAnchorEl : brandsEls) {
            String code = HttpUtil.getRequestParameter(brandsAnchorEl.attr("href"), "code");
            List<String> subCategoryPath = new ArrayList<>(categories);
            subCategoryPath.add(code);
            browse(subCategoryPath);
        }

    }

    /**
     * Get Product from Multicraft.
     * @param productCode
     * @return Product
     * @throws IOException
     */
    private Product getProduct(final String productCode) throws IOException {

        Document doc = getHTMLDocument("/en/brand/sku?id=" + productCode);

        Elements imageEls = doc.select("#img-previews>li>img");

        String[] imageUrls = new String[imageEls.size()];

        for (int i = 0; i < imageEls.size(); i++) {
            imageUrls[i] = imageEls.get(i).attr("src").split("\\?")[0];
        }

        Elements fieldEls = doc.select("div.details-brief > .row");

        float price = 0;
        float discount = 0;

        Long upc = null;

        Integer invertyQty = 0;

        for (Element fieldEl : fieldEls) {
            if (fieldEl.selectFirst(".hdr").text().equals("unit price")) {

                String priceText = fieldEl
                        .selectFirst(".vlu")
                        .text()
                        .replace("$", "");
                if (priceText.contains(" ")) {
                    String[] spli = priceText.split(" ");
                    price = Float.parseFloat(spli[0]);
                    discount = Float.parseFloat(spli[1]);
                } else {
                    price = Float.parseFloat(priceText);
                }

            } else if (fieldEl.selectFirst(".hdr").text().equals("pack")) {
                String priceText = fieldEl
                        .selectFirst(".vlu")
                        .text();

                invertyQty = Integer.parseInt(priceText);

            } else if (fieldEl.selectFirst(".hdr").text().equals("UPC")) {
                String upcTxt = fieldEl
                        .selectFirst(".vlu")
                        .text();
                if (!upcTxt.trim().isEmpty()) {
                    upc = Long.parseLong(upcTxt);
                }

            }
        }

        return new Product(
                productCode
                , doc.selectFirst(".details-desc") == null ? null : doc.selectFirst(".details-desc").text()
                , doc.selectFirst(".details-blurb") == null ? null : doc.selectFirst(".details-blurb").text()
                , upc
                , invertyQty
                , price
                , discount
                , imageUrls
        );
    }

    private Document getHTMLDocument(final String url) throws IOException {
        return session.newRequest(BASE_URL + url).get();
    }
}
