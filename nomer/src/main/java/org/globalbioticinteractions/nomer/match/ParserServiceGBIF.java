package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;

import java.util.ArrayList;
import java.util.List;

public class ParserServiceGBIF extends ParserServiceAbstract {

    private final NameParser parser = new NameParserGBIF();

    @Override
    public Taxon parse(Term term, String nameString) throws PropertyEnricherException {
        try {
            ParsedName nameParsed = parser.parse(nameString, Rank.UNRANKED, null);
            Taxon taxonParsed = new TaxonImpl();
            taxonParsed.setName(nameParsed.canonicalNameWithoutAuthorship());
            taxonParsed.setAuthorship(nameParsed.authorshipComplete());
            if (!nameParsed.getRank().isAmbiguous()) {
                taxonParsed.setRank(StringUtils.lowerCase(nameParsed.getRank().toString()));
                List<String> path  = new ArrayList<>();
                List<String> pathNames  = new ArrayList<>();
                String genus = nameParsed.getGenus();
                if (StringUtils.isNotBlank(genus)) {
                    path.add(genus);
                    pathNames.add("genus");
                }

                String infragenericEpithet = nameParsed.getInfragenericEpithet();
                if (StringUtils.isNotBlank(infragenericEpithet)) {
                    path.add(infragenericEpithet);
                    pathNames.add("infragenericEpithet");
                }

                String specificEpithet = nameParsed.getSpecificEpithet();
                if (StringUtils.isNotBlank(specificEpithet)) {
                    path.add(specificEpithet);
                    pathNames.add("specificEpithet");
                }

                String infraspecificEpithet = nameParsed.getInfraspecificEpithet();
                if (StringUtils.isNotBlank(infraspecificEpithet)) {
                    path.add(infraspecificEpithet);
                    pathNames.add("infraspecificEpithet");
                }

                String pathString = StringUtils.join(path, CharsetConstant.SEPARATOR);
                if (StringUtils.isNotBlank(pathString)) {
                    taxonParsed.setPath(pathString);
                }

                String pathNamesString = StringUtils.join(pathNames, CharsetConstant.SEPARATOR);
                if (StringUtils.isNotBlank(pathNamesString)) {
                    taxonParsed.setPathNames(pathNamesString);
                }

            }


            return taxonParsed;
        } catch (UnparsableNameException e) {
            throw new PropertyEnricherException("failed to parse [" + term.getName() + "]", e);
        }
    }

}
