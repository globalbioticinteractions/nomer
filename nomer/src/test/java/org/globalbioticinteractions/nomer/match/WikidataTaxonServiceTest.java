package org.globalbioticinteractions.nomer.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.Taxon;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class WikidataTaxonServiceTest {

    @Test
    public void parseTaxon() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/wikidata/lion.json");
        JsonNode jsonNode = new ObjectMapper().readTree(is);

        assertNotNull(jsonNode);

        Taxon taxon = WikidataTaxonService.parseTaxon(jsonNode);

        assertThat(taxon.getId(), Is.is("WD:Q140"));
        assertThat(taxon.getName(), Is.is("Panthera leo"));
        assertThat(taxon.getCommonNames(), containsString("Leeuw @nl"));
        assertThat(taxon.getCommonNames(), containsString("Lion @en"));
        assertThat(taxon.getPathIds(), Is.is("WD:Q127960 | WD:Q140"));
    }

    @Test
    public void relatedIdentifiers() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/wikidata/lion.json");
        JsonNode jsonNode = new ObjectMapper().readTree(is);

        assertNotNull(jsonNode);


        List<String> relatedIds = WikidataTaxonService.parseRelatedIds(jsonNode);

        assertThat(relatedIds, hasItem("EOL:328672"));
        assertThat(relatedIds, hasItem("ITIS:183803"));
        assertThat(relatedIds.size(), Is.is(9));

    }

}