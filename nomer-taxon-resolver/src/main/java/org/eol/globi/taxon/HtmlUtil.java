package org.eol.globi.taxon;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;

import java.io.IOException;

public class HtmlUtil {
    public static String getHtmlAsXmlString(String url) throws IOException {
        final WebClient webClient = new WebClient();
        webClient
                .getOptions()
                .setUseInsecureSSL(true);

        final DomNode page = webClient.getPage(url);
        return page.asXml();
    }
}
