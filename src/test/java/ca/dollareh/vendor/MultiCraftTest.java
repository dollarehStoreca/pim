package ca.dollareh.vendor;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

class MultiCraftTest {

    @Test
    void testGetCategories() throws URISyntaxException {
        MultiCraft multiCraft = new MultiCraft();
        System.out.println(multiCraft.getCategory("scrapbook~albums"));
    }

}