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
        return getCategories((String) null);
    }

    /**
     * Get All Categories from Multicraft.
     * @param code
     * @return categories
     * @throws IOException
     */
    public List<Category> getCategories(String code) throws URISyntaxException {

        String htmlContent = code == null ?
                multiCraftConnection.getHTML("/en/brand") :
                multiCraftConnection.getHTML("/en/brand/subbrands?code=" + code);

        Document doc = Jsoup.parse(htmlContent);

        return getCategories(doc);
    }

    private List<Category> getCategories(final Document doc) throws URISyntaxException {
        List<Category> categories = new ArrayList<>();

        Elements brandsEls = doc.select("ul.brandsList>li>a");

        List<Product> products = getProducts(doc);

        for (Element brandsAnchorEl : brandsEls) {

            Optional<String> codeOp = new URIBuilder(brandsAnchorEl.attr("href"))
                    .getQueryParams()
                    .stream()
                    .filter(nameValuePair -> nameValuePair.getName().equals("code"))
                    .findFirst()
                    .map(NameValuePair::getValue);

            if (codeOp.isPresent()) {
                categories.add(new Category(codeOp.get(), getCategories(codeOp.get()),products));
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
            products.add(new Product(skusEl.id()));
        }

        return products;
    }

}
