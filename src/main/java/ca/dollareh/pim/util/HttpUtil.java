package ca.dollareh.pim.util;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;

public class HttpUtil {
    public static String getRequestParameter(final String url,final String param) throws URISyntaxException {
        return new URIBuilder(url)
                .getQueryParams()
                .stream()
                .filter(nameValuePair ->{
                    return nameValuePair.getName().equals(param);
                })
                .findFirst()
                .map(NameValuePair::getValue)
                .orElse(null);
    }
}
