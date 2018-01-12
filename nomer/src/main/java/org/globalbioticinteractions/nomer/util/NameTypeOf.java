package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;

interface NameTypeOf {
    NameType nameTypeOf(Taxon taxon);
}
