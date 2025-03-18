package ca.dollareh.pim.source;

import ca.dollareh.pim.model.Product;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
    }

    @Override
    protected File downloadAsset(final String assetUrl) throws IOException {
        return null;
    }

    private void browse(final String category) throws IOException, URISyntaxException {

    }

    private Document getHTMLDocument(Connection session, final String url) throws IOException {
        return session.newRequest(BASE_URL + url).get();
    }
}
