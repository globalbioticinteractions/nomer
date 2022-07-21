package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class BatNamesUtil {

    public static final char NON_BREAKING_SPACE = '\u00A0';


    public static String toPatchedXmlString(String htmlAsXmlString) {
        String patchedXml = StringUtils.replace(
                StringUtils.replace(
                        StringUtils.replace(
                                StringUtils.replace(
                                        htmlAsXmlString,
                                        "'target=\"_blank'\"", "target=\"_blank\""),
                                "'target=\"_blank\"",
                                "target=\"_blank\""),
                        "'=\"\"", ""),
                "\"target=\"_blank\"",
                "target=\"_blank\"");
        return patchedXml.replace(NON_BREAKING_SPACE, ' ');
    }

    public static String getGenusXml(String genusName) throws IOException {
        String htmlAsXmlString = HtmlUtil.getHtmlAsXmlString
                ("https://batnames.org/genera/" + genusName);

        return toPatchedXmlString(htmlAsXmlString);
    }
}
