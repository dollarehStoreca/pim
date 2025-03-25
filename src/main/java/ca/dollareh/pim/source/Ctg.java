package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class Ctg extends ProductSource{

    public static final String BASE_URL = "https://multicraft.ca";

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
