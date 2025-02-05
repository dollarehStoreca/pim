package ca.dollareh.vendor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.junit.jupiter.api.Test;

class MultiCraftTest {

    @Test
    void testGetCategories() throws URISyntaxException {
        MultiCraft multiCraft = new MultiCraft();
        System.out.println(multiCraft.getCategories("scrapbook~albums"));




    }

}