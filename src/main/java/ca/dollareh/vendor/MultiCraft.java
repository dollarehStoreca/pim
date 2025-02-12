package ca.dollareh.vendor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ca.dollareh.core.model.Product;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ca.dollareh.core.model.Category;

import static ca.dollareh.vendor.MultiCraftConnection.BASE_URL;


public class MultiCraft {

    private final MultiCraftConnection multiCraftConnection ;

    public MultiCraft() {
        multiCraftConnection = new MultiCraftConnection(System.getenv("DH_MULTICRAFT_COKKIE"));
    }

    /**
     * Get All Categories from Multicraft.
     * 
     * @return categories
     * @throws IOException
     */
    public List<Category> getCategories() throws URISyntaxException {
        String htmlContent =
                multiCraftConnection.getHTML("/en/brand");
        Document doc = Jsoup.parse(htmlContent);
        return getCategories(doc);
    }

    /**
     * Get All Categories from Multicraft.
     * @param code
     * @return categories
     * @throws IOException
     */
    public Category getCategory(String code) throws URISyntaxException {
        String htmlContent =
                multiCraftConnection.getHTML("/en/brand/subbrands?code=" + code);
        Document doc = Jsoup.parse(htmlContent);
        return getCategory(code, doc);
    }

    private Category getCategory(final String code,
                                 final Document doc) throws URISyntaxException {
        return new Category(code, getCategories(doc), getProducts(doc));
    }

    private List<Category> getCategories(final Document doc) throws URISyntaxException {
        List<Category> categories = new ArrayList<>();

        Elements brandsEls = doc.select("ul.brandsList>li>a");

        for (Element brandsAnchorEl : brandsEls) {

            Optional<String> codeOp = new URIBuilder(brandsAnchorEl.attr("href"))
                    .getQueryParams()
                    .stream()
                    .filter(nameValuePair -> nameValuePair.getName().equals("code"))
                    .findFirst()
                    .map(NameValuePair::getValue);

            if (codeOp.isPresent()) {
                categories.add(getCategory(codeOp.get()));
            }
        }
        return categories;
    }


    /**
     * Get All Products from Multicraft.
     * @param brandDoc
     * @return categories
     * @throws IOException
     */
    public List<Product> getProducts(Document brandDoc) throws URISyntaxException {
        List<Product> products = new ArrayList<>();

        Elements skusEls = brandDoc.select("#skusCards>li");

        for (Element skusEl : skusEls) {
            products.add(getProduct(skusEl.selectFirst(".summary-id").text()));
        }

        return products;
    }

    /**
     * Get Product from Multicraft.
     * @param productCode
     * @return Product
     * @throws IOException
     */
    public Product getProduct(final String productCode) {


        final String productHtml = multiCraftConnection.getHTML("/en/brand/sku?id=" + productCode);

        Document doc = Jsoup.parse(productHtml);


        Elements imageEls = doc.select("#img-previews>li>img");

        String[] imageUrls = new String[imageEls.size()];


        for (int i = 0; i < imageEls.size(); i++) {
            imageUrls[i] = BASE_URL + imageEls.get(i).attr("src").split("\\?")[0];
        }


        Elements fieldEls = doc.select("div.details-brief > .row");

        float price = 0;

        for (Element fieldEl : fieldEls) {

            if(fieldEl.selectFirst(".hdr").text().equals("unit price")) {
                price = Float.parseFloat(fieldEl
                        .selectFirst(".vlu")
                        .text()
                        .replace("$",""));
            }

        }


        return new Product(productCode
                , doc.selectFirst(".details-desc").text()
                , doc.selectFirst(".details-blurb").text()
                , price
                , imageUrls
        );
    }



}
