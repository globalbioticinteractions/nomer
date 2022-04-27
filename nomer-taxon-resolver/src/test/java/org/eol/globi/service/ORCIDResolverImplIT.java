package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class ORCIDResolverImplIT {

    @Test
    public void lookupName() throws IOException {
        ORCIDResolverImpl orcidResolver = new ORCIDResolverImpl();
        orcidResolver.setBaseUrl("https://pub.sandbox.orcid.org/v2.0/");
        Map<String, String> name = orcidResolver.findAuthor("http://orcid.org/0000-0002-2389-8429");
        assertThat(name.get(PropertyAndValueDictionary.NAME), is("Sofia Hernandez"));
        assertThat(name.get(PropertyAndValueDictionary.PATH), is("Sofia | Hernandez | Sofia Hernandez"));
        assertThat(name.get(PropertyAndValueDictionary.PATH_NAMES), is("given-names | family-name | name"));
    }

    @Test
    public void lookupNameProductionFailsOnSandbox() throws IOException {
        Map<String, String> name =  new ORCIDResolverImpl().findAuthor("http://orcid.org/0000-0002-6601-2165");
        assertThat(name.get(PropertyAndValueDictionary.NAME), is("Christopher Mungall"));
        assertThat(name.get(PropertyAndValueDictionary.PATH), is("Christopher | Mungall | Christopher Mungall"));
        assertThat(name.get(PropertyAndValueDictionary.PATH_NAMES), is("given-names | family-name | name"));
        assertThat(name.get(PropertyAndValueDictionary.PATH_IDS), is(" |  | http://orcid.org/0000-0002-6601-2165"));
    }

    @Test
    public void lookupNameProductionFailsOnSandboxHttps() throws IOException {
        Map<String, String> name =  new ORCIDResolverImpl().findAuthor("https://orcid.org/0000-0002-6601-2165");
        assertThat(name.get(PropertyAndValueDictionary.NAME), is("Christopher Mungall"));
    }

}
