package org.eol.globi.taxon;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TaxonUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubgenusStrippingListener implements TermMatchListener {
    private static final Pattern GENUS_SUBGENUS_PATTERN = Pattern.compile("([A-Z][a-z]+)[ ]+(\\([A-Z][a-z]+\\))[ ]+([a-z]+)");
    private final TermMatchListener termMatchListener;

    public SubgenusStrippingListener(TermMatchListener termMatchListener) {
        this.termMatchListener = termMatchListener;
    }

    @Override
    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
        if (term instanceof Taxon) {
            Matcher matcher = GENUS_SUBGENUS_PATTERN.matcher(term.getName());
            if (matcher.matches()) {
                String genusName = matcher.group(1);
                String specificEpithet = matcher.group(3);
                Taxon simplifiedCopy = TaxonUtil.copy((Taxon) term);
                simplifiedCopy.setName(genusName + " " + specificEpithet);
                termMatchListener.foundTaxonForTerm(aLong, simplifiedCopy, nameType, taxon);
            }
        }

        termMatchListener.foundTaxonForTerm(aLong, term, nameType, taxon);
    }
}
