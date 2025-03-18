package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.util.HttpUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
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
    }

    @Override
    protected void logout() throws IOException {
        logger.info("Logged out from Multicraft");
    }

    @Override
    protected void browse() throws IOException, URISyntaxException {
        logger.info("Browsing Multicraft");
        Document document = getHTMLDocument("/en/brand/");

        Element ulElement = document.selectFirst("ul.brandsList");
        Elements liElements = ulElement.children();

        liElements.stream().parallel().forEach(liElement -> {
            try {
                browse(HttpUtil.getRequestParameter(liElement.selectFirst("a").attr("href"), "code"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected File downloadAsset(final String assetUrl) throws IOException {
        return null;
    }

    private void browse(final String category) throws IOException, URISyntaxException {
        logger.info("Browsing brands {}", category);
        Document subBrandDocument = getHTMLDocument("/en/brand/subbrands?code=" + category);
        Element brandUlElements = subBrandDocument.getElementById("skusCards");
        Elements brandLi = brandUlElements.children();

        brandLi.stream().parallel().forEach(brandLiElement -> {
//            logger.info("reading product info....");
            Elements imageElements = brandLiElement.select("img.skuSummary-img");
            String[] imgUrls = imageElements.stream().parallel().map(imageElement -> {
                return imageElements.attr("src");
            }).toArray(String[]::new);

            logger.info("Product title for {}, is {}", category, Arrays.toString(imgUrls));
            Product product = new Product(
                    brandLiElement.selectFirst("div.skuSummary-top-brief").selectFirst("p").text(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        });
    }

    private Document getHTMLDocument(final String url) throws IOException {
        return session.newRequest(BASE_URL + url).get();
    }
}