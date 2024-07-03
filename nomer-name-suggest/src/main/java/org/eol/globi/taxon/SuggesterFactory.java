package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.eol.globi.service.ServiceUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.ArrayList;
import java.util.List;

public class SuggesterFactory {

    public static final String NOMER_TAXON_NAME_STOPWORD_URL = "nomer.taxon.name.stopword.url";
    public static final String NOMER_TAXON_NAME_CORRECTION_URL = "nomer.taxon.name.correction.url";

    static List<NameSuggester> createSuggesterEnsemble(TermMatcherContext ctx) {
        final NameSuggester manualSuggestor = createManualSuggester(ctx);

        final NameSuggester stopwordRemover = createStopwordRemover(ctx);

        return new ArrayList<NameSuggester>() {
            {
                // give manual suggestions first try
                addSuggesters();
                addSuggesters();
            }

            private void addSuggesters() {
                add(manualSuggestor);
                add(stopwordRemover);
                add(new SnakeCaseUndoer());
                add(new PeriodAsWhitespaceUndoer());
                add(new AllCapsUndoer());
                add(new CapitalizeFirstWord());
                add(new NameScrubber());
            }
        };
    }

    public static NameSuggester createStopwordRemover(TermMatcherContext ctx) {
        return new RemoveStopWordService() {{
            ServiceUtil.initWith(this, NOMER_TAXON_NAME_STOPWORD_URL, ctx);
        }};
    }

    public static NameSuggester createManualSuggester(TermMatcherContext ctx) {
        return new ManualSuggester() {{
            ServiceUtil.initWith(this, NOMER_TAXON_NAME_CORRECTION_URL, ctx);
        }};
    }
}
