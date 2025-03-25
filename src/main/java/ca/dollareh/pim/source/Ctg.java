package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import ca.dollareh.pim.util.HttpUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

public class Ctg extends ProductSource{

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

    }

    @Override
    protected void logout() throws IOException {

    }

    @Override
    protected void browse() throws IOException, URISyntaxException {
        browse("/everyday");
//        browse("/seasonal");
//        browse("/home-décor");
    }

    private void browse(String url) throws IOException, URISyntaxException {

        Document categoryListingDocument = getHTMLDocument(url);

        Elements categoryElements = categoryListingDocument.select("div.usn-sc-container>div.row>div.col-vh>div>div>div>div>a");

        categoryElements.stream().forEach(anchorEl -> {
            String browseUrl = anchorEl.attr("href");

            try {
                String category = HttpUtil.getRequestParameter(browseUrl, "Category");
                String mainCategory = HttpUtil.getRequestParameter(browseUrl, "MainCategory");

                System.out.println(browseUrl);

                if(mainCategory == null || category == null) {
                    System.out.println("D");
                }
                List<String> categories = List.of(mainCategory, category);

                browseProducts(categories, browseUrl);



            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (NullPointerException np) {
                System.out.println(np);
            }




        });



    }

    private void browseProducts(List<String> categories, String browseUrl) {
        System.out.println(categories);
        System.out.println(browseUrl);
    }



    @Override
    protected void downloadAsset(File imageFile, String assetUrl) throws IOException {
        imageFile.getParentFile().mkdirs();
        Connection.Response resultImageResponse = session
                .newRequest(BASE_URL + assetUrl)
                .ignoreContentType(true)
                .execute();
        FileOutputStream out = new FileOutputStream(imageFile);
        out.write(resultImageResponse.bodyAsBytes());  // resultImageResponse.body() is where the image's contents are.
        out.close();
    }

    private Document getHTMLDocument(final String url) throws IOException {
        return session.newRequest(BASE_URL + url).get();
    }

}
