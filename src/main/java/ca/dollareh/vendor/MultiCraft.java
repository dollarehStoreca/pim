package ca.dollareh.vendor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ca.dollareh.core.model.Product;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ca.dollareh.core.model.Category;

public class MultiCraft {

    private static String BASE_URL = "https://multicraft.ca";

    private final Connection session ;

    public MultiCraft() throws IOException {
        session = Jsoup.newSession()
                .timeout(45 * 1000)
                .maxBodySize(5 * 1024 * 1024);

        Document langingPage = session.newRequest("https://multicraft.ca/en")
                .get();

        String code = langingPage.selectFirst("input[name=\"__RequestVerificationToken\"]").val();

        Document req1 = session.newRequest("https://multicraft.ca/en/user/login")
                .data("UserName", System.getenv("MULTICRAFT_USER"))
                .data("Password", System.getenv("MULTICRAFT_PW"))
                .data("__RequestVerificationToken", code)
                .post();
    }

    /**
     * Get All Categories from Multicraft.
     * 
     * @return categories
     * @throws IOException
     */
    public List<Category> getCategories() throws URISyntaxException, IOException {
        return getCategories(null, getHTMLDocument("/en/brand"));
    }

    /**
     * Get All Categories from Multicraft.
     * @param code
     * @return categories
     * @throws IOException
     */
    public Category getCategory(final Category parent,String code) throws URISyntaxException, IOException {
        return getCategory(parent, code, getHTMLDocument("/en/brand/subbrands?code=" + code));
    }

    private Category getCategory(final Category parent,
                                final String code,
                                 final Document doc) throws URISyntaxException, IOException {

        Category category = new Category(parent, code,new ArrayList<>() , new ArrayList<>());

        List<Category> categories = getCategories(category, doc);
        category.categories().addAll(categories);

        List<Product> products = getProducts(category, doc);
        category.products().addAll(products);

        return category;
    }

    private List<Category> getCategories(final Category parent,final Document doc) throws URISyntaxException, IOException {
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
                categories.add(getCategory(parent, codeOp.get()));
            }
        }
        return categories;
    }

    /**
     * Get All Products from Multicraft.
     * @param category
     * @param brandDoc
     * @return categories
     * @throws IOException
     */
    public List<Product> getProducts(Category category,
                                     Document brandDoc) throws URISyntaxException, IOException {
        List<Product> products = new ArrayList<>();

        Elements skusEls = brandDoc.select("#skusCards>li");

        for (Element skusEl : skusEls) {
            products.add(getProduct(category, skusEl.selectFirst(".summary-id").text()));
        }

        return products;
    }

    /**
     * Get Product from Multicraft.
     * @param category
     * @param productCode
     * @return Product
     * @throws IOException
     */
    public Product getProduct(final Category category,
                              final String productCode) throws IOException {

        Document doc = getHTMLDocument("/en/brand/sku?id=" + productCode);

        Elements imageEls = doc.select("#img-previews>li>img");

        String[] imageUrls = new String[imageEls.size()];

        for (int i = 0; i < imageEls.size(); i++) {
            imageUrls[i] = BASE_URL + imageEls.get(i).attr("src").split("\\?")[0];
        }

        Elements fieldEls = doc.select("div.details-brief > .row");

        float price = 0;
        float discount = 0;

        Long upc = 0L;

        Integer invertyQty = 0;

        for (Element fieldEl : fieldEls) {
            if(fieldEl.selectFirst(".hdr").text().equals("unit price")) {

                String priceText = fieldEl
                        .selectFirst(".vlu")
                        .text()
                        .replace("$","");
                if(priceText.contains(" ")) {
                    String[] spli = priceText.split(" ");
                    price = Float.parseFloat(spli[0]);
                    discount = Float.parseFloat(spli[1]);
                } else {
                    price = Float.parseFloat(priceText);
                }

            } else if(fieldEl.selectFirst(".hdr").text().equals("pack")) {
                String priceText = fieldEl
                        .selectFirst(".vlu")
                        .text();

                invertyQty = Integer.parseInt(priceText);



            }
            else if(fieldEl.selectFirst(".hdr").text().equals("UPC")) {
                String priceText = fieldEl
                        .selectFirst(".vlu")
                        .text();

                upc = Long.parseLong(priceText);



            }
        }

        return new Product(category,
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

    public void logout() throws IOException {
        session.newRequest(BASE_URL + "/en/user/logout")
                .get();
    }
}
