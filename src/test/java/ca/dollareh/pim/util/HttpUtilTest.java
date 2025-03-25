package ca.dollareh.pim.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilTest {

    @Test
    void getRequestParameter() throws URISyntaxException {

        String categpry = HttpUtil.getRequestParameter("/shop/?MainCategory=Home%20Decor&orderBy=Featured,Id&context=shop&page=1","Category");

        Assertions.assertEquals("Personal%20Care", categpry);

    }
}