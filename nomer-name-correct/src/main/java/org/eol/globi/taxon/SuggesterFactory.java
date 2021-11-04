package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.Initializing;
import org.eol.globi.service.NameSuggester;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.UKSISuggestionService;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SuggesterFactory {

    public static final String NOMER_TAXON_NAME_STOPWORD_URL = "nomer.taxon.name.stopword.url";
    public static final String NOMER_TAXON_NAME_CORRECTION_URL = "nomer.taxon.name.correction.url";

    private static void initWith(Initializing service, String propertyName, TermMatcherContext ctx) {
        try {
            if (ctx != null && StringUtils.isNoneBlank(ctx.getProperty(propertyName))) {
                service.init(ctx.retrieve(
                        CacheUtil.getValueURI(ctx, propertyName))
                );
            }
        } catch (IOException | PropertyEnricherException e) {
            throw new IllegalArgumentException("failed to instantiate name service [" + service.getClass().getSimpleName() + "] using property [" + propertyName + "]", e);
        }
    }

    static List<NameSuggester> createSuggesterEnsemble(TermMatcherContext ctx) {
        final NameSuggester manualSuggestor = createManualSuggester(ctx);

        final NameSuggester stopwordRemover = createStopwordRemover(ctx);
        final GlobalNamesCanon gnCanon = new GlobalNamesCanon();

        return new ArrayList<NameSuggester>() {
            {
                // give manual suggestions first try
                add(manualSuggestor);
                add(stopwordRemover);
                add(new SnakeCaseUndoer());
                add(new PeriodAsWhitespaceUndoer());
                add(new AllCapsUndoer());

                // map using UK species inventory
                add(new UKSISuggestionService() {
                    {
                        initWith(this, "nomer.taxon.name.uksi.url", ctx);
                    }
                });
                // attempt to extract canonical name
                add(gnCanon);
                // manual suggestions again
                add(manualSuggestor);
                add(stopwordRemover);
                add(new NameScrubber());
                add(gnCanon);
                add(manualSuggestor);
                add(new NameScrubber());
            }
        };
    }

    public static NameSuggester createStopwordRemover(TermMatcherContext ctx) {
        return new RemoveStopWordService() {{
            initWith(this, NOMER_TAXON_NAME_STOPWORD_URL, ctx);
        }};
    }

    public static NameSuggester createManualSuggester(TermMatcherContext ctx) {
        return new ManualSuggester() {{
            initWith(this, NOMER_TAXON_NAME_CORRECTION_URL, ctx);
        }};
    }
}
