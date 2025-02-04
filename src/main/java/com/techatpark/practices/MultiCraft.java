package com.techatpark.practices;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.techatpark.core.model.Category;


public class MultiCraft {

    /**
     * Get All Categories from Multicraft.
     * 
     * @return categories
     * @throws IOException
     */
    public List<Category> getCategories() throws IOException {
        MultiCraftConnection multiCraftConnection = new MultiCraftConnection(System.getenv("DH_MULTICRAFT_COKKIE"));

        String htmlContent = multiCraftConnection.getHTML("/en/brand");

        List<Category> brands = new ArrayList<>();

        Document doc = Jsoup.parse(htmlContent);

        Elements brandsEls = doc.select("ul.brandsList>li>a");


        for (Element brandsEl : brandsEls) {
            // String url = "http://www.example.com/something.html?one=1&two=2&three=3&three=3a";

            // List<NameValuePair> params = URLEncodedUtils.parse(new URI(url), Charset.forName("UTF-8"));

            brands.add(new Category(brandsEl.attr("href")));
        }

        return brands;
    }

}
