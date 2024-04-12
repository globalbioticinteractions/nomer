package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;

import java.util.List;

public abstract class ParserServiceAbstract implements TermMatcher, ParserService {

    @Override
    public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {

        for (Term term : list) {
            String name = term.getName();
            // names ending with a capital V or containing "virus" are likely virus names
            if (StringUtils.endsWith(name, "V")
                    || StringUtils.containsIgnoreCase(name, "virus")) {
                Taxon likelyVirusName;
                if (term instanceof Taxon) {
                    likelyVirusName = TaxonUtil.copy((Taxon) term);
                } else {
                    likelyVirusName = new TaxonImpl(term.getName(), term.getId());
                }
                termMatchListener.foundTaxonForTerm(null, term, NameType.NONE, likelyVirusName);
            } else if (StringUtils.isNotBlank(name)) {
                Taxon nameParsed = parse(term, name);
                termMatchListener.foundTaxonForTerm(null, term, NameType.SAME_AS, nameParsed);
            }
        }
    }

    @Override
    abstract public Taxon parse(Term term, String name) throws PropertyEnricherException;

}
