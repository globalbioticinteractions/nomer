package org.eol.globi.taxon;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.cache.Resource;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@PropertyEnricherInfo(name = "discoverlife-taxon", description = "Look up taxa of https://discoverlife.org by name or id with DL:* prefix.")
public class DiscoverLifeService extends OfflineService {

    static final String URL_ENDPOINT_DISCOVER_LIFE = "https://www.discoverlife.org";
    static final String BEE_NAMES = "/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz";
    static final String URL_ENDPOINT_DISCOVER_LIFE_SEARCH = URL_ENDPOINT_DISCOVER_LIFE +
            "/mp/20q?search=";
    static final List<String> PATH_STATIC = Arrays.asList("Animalia", "Arthropoda", "Insecta", "Hymenoptera");
    static final List<String> PATH_STATIC_IDS = PATH_STATIC
            .stream()
            .map(x -> StringUtils.prependIfMissing(x, URL_ENDPOINT_DISCOVER_LIFE_SEARCH))
            .collect(Collectors.toList());
    static final List<String> PATH_NAMES_STATIC = Arrays.asList("kingdom", "phylum", "class", "order", "family", "species");
    private final TermMatcherContext ctx;
    public static final String DISCOVER_LIFE_URL
            = URL_ENDPOINT_DISCOVER_LIFE +
            "/mp/20q" +
            "?act=x_checklist" +
            "&guide=Apoidea_species" +
            "&flags=HAS";

    public DiscoverLifeService(TermMatcherContext ctx) {
        super();
        this.ctx = ctx;
    }

    public static String getBeeNamesAsXmlString() throws IOException {
        final WebClient webClient = new WebClient();

        final DomNode page = getBeePage(webClient);
        return page.asXml();
    }

    private static DomNode getBeePage(WebClient webClient) throws IOException {
        webClient
                .getOptions()
                .setUseInsecureSSL(true);

        return webClient.getPage(DISCOVER_LIFE_URL);
    }

    static InputStream getStreamOfBees() throws IOException {
        return new GZIPInputStream(DiscoverLifeService.class
                .getResourceAsStream(BEE_NAMES)
        );
    }


    @Override
    protected TaxonomyImporter createTaxonomyImporter() {

        return new TaxonomyImporter(
                ctx,
                new TaxonParserForDiscoverLife(),
                new TaxonReaderFactory() {
                    @Override
                    public Map<String, Resource> getResources() throws IOException {
                        return new TreeMap<String, Resource>() {
                            {
                                put(DISCOVER_LIFE_URL, new Resource() {
                                    @Override
                                    public InputStream getInputStream() throws IOException {
                                        // note that SSL certificate used by discover life is not supported by jvm somehow
                                        // so, for the time being, using a static version instead
                                        //return ctx.getResource(DISCOVER_LIFE_URL);
                                        return getStreamOfBees();
                                    }

                                    @Override
                                    public long length() {
                                        try {
                                            return IOUtils.copy(ctx.getResource(DISCOVER_LIFE_URL), new CountingOutputStream(NullOutputStream.NULL_OUTPUT_STREAM));
                                        } catch (IOException e) {
                                            throw new IllegalStateException("failed to count bee names", e);
                                        }
                                    }

                                    @Override
                                    public void dispose() {

                                    }
                                });
                            }
                        };
                    }
                }
        );
    }

}
