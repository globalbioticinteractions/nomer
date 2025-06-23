package org.eol.globi.taxon;

import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class NCBIServiceIT extends NCBIServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected NCBIService getEnricher() {
        try {
            return new NCBIService(new ResourceServiceHTTP(is -> is, folder.newFolder("tmpCacheDir")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
