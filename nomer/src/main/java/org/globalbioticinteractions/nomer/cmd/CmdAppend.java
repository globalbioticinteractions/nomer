package org.globalbioticinteractions.nomer.cmd;

import org.eol.globi.taxon.RowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(
        name = "append",
        aliases = {"align"},
        description = "Append term match to row from stdin using id and name columns specified in input schema. " +
                "Multiple matches result in multiple rows." +
                "%nFor example:%n"+ "echo -e '\\tHomo sapiens' | nomer append col" +
                "%nhas expected result:%n"+ "\tHomo sapiens\tHAS_ACCEPTED_NAME\tCOL:6MB3T\tHomo sapiens\tLinnaeus, 1758\tspecies\t\tBiota | Animalia | Chordata | Vertebrata | Gnathostomata | Osteichthyes | Sarcopterygii | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens\tCOL:5T6MX | COL:N | COL:CH2 | COL:8V4V3 | COL:8V4V5 | COL:8VVWB | COL:8VSMX | COL:9CK8W | COL:8VLBH | COL:6224G | COL:924GT | COL:LG | COL:8ZXYB | COL:4DT | COL:4PM | COL:58L | COL:6256T | COL:JPH | COL:636X2 | COL:6MB3T\tunranked | kingdom | phylum | subphylum | infraphylum | parvphylum | gigaclass | megaclass | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species\t|  |  |  |  |  |  |  |  | Linnaeus, 1758 | Parker & Haswell, 1897 | Gill, 1872 | Linnaeus, 1758 | Pocock, 1918 | Haeckel, 1866 | Gray, 1825 | Gray, 1825 | Gray, 1825 | Linnaeus, 1758 | Linnaeus, 1758\thttps://www.catalogueoflife.org/data/taxon/6MB3T"
)
public class CmdAppend extends CmdOutput {

    @Override
    public void run() {
        List<RowHandler> handlers = MatchUtil.getAppendingRowHandlers(
                this,
                getIncludeHeader(),
                getOutputFormat(),
                System.out
        );
        MatchUtil.match(handlers.toArray(new RowHandler[0]));
    }

}
