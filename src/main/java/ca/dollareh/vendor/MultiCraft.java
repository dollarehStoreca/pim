package ca.dollareh.vendor;

import ca.dollareh.core.model.Product;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class MultiCraft extends ProductSource {

    public static final String BASE_URL = "https://multicraft.ca";

    MultiCraft(final Consumer<Product> newProductConsumer,
               final Consumer<Product> modifiedProductConsumer) {
        super(newProductConsumer, modifiedProductConsumer);
    }

    @Override
    public void browse() throws IOException, URISyntaxException {
        Connection session = Jsoup.newSession()
                .timeout(45 * 1000)
                .maxBodySize(5 * 1024 * 1024);

        Document langingPage = session.newRequest(BASE_URL +"/en")
                .get();

        String code = langingPage.selectFirst("input[name=\"__RequestVerificationToken\"]").val();

        session.newRequest(BASE_URL + "/en/user/login")
                .data("UserName", System.getenv("MULTICRAFT_USER"))
                .data("Password", System.getenv("MULTICRAFT_PW"))
                .data("__RequestVerificationToken", code)
                .post();

        browse(path, null, session);

        session.newRequest(BASE_URL + "/en/user/logout")
                .get();
    }


    public void browse(final Path currentDir, String category, Connection session) throws IOException, URISyntaxException {

        Document document = getHTMLDocument(session, "/en/brand" + (category == null ? "" : "/subbrands?code=" + category));

        Elements brandsEls = document.select("ul.brandsList>li>a");

        Path categoryDir = category == null ? currentDir : new File(currentDir.toFile(), category).toPath();

        categoryDir.toFile().mkdirs();

        for (Element brandsAnchorEl : brandsEls) {
            new URIBuilder(brandsAnchorEl.attr("href"))
                    .getQueryParams()
                    .stream()
                    .filter(nameValuePair -> nameValuePair.getName().equals("code"))
                    .findFirst()
                    .map(NameValuePair::getValue)
                    .ifPresent(code -> {
                        try {
                            browse(categoryDir, code.trim(), session);
                        } catch (IOException | URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        Elements skusEls = document.select("#skusCards>li");

        skusEls.stream().parallel().forEach(skusEl -> {
            try {
                onProductDiscovery(categoryDir,
                        getProduct(
                                skusEl.selectFirst(".summary-id").text(),
                                session));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Get Product from Multicraft.
     * @param productCode
     * @return Product
     * @throws IOException
     */
    private Product getProduct(final String productCode,
                               Connection session) throws IOException {

        Document doc = getHTMLDocument(session, "/en/brand/sku?id=" + productCode);

        Elements imageEls = doc.select("#img-previews>li>img");

        String[] imageUrls = new String[imageEls.size()];

        for (int i = 0; i < imageEls.size(); i++) {
            imageUrls[i] = BASE_URL + imageEls.get(i).attr("src").split("\\?")[0];
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

    private Document getHTMLDocument(Connection session, final String url) throws IOException {
        return session.newRequest(BASE_URL + url).get();
    }
}
