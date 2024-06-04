package org.globalbioticinteractions.nomer.cmd;

import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.TermMatchUtil;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "list",
        aliases = {"ls", "dump", "export"},
        description = "Dumps all terms into the defined output schema." +
                "%nFor example:%n"+ "nomer ls col | head -n2" +
                "%nhas expected result:" +
                "%nprovidedExternalId\tprovidedName\tprovidedAuthorship\trelationName\tresolvedExternalId\tresolvedName\t...resolvedAuthorship\tresolvedRank\tresolvedCommonNames\tresolvedPath\tresolvedPathIds\tresolvedPathNames\tresolvedPathAuthorships\tresolvedExternalUrl" +
                "COL:001417c6-d3fc-4f42-aa3d-b1de3a592e58\tCheilostomatida incertae sedis\t\tHAS_ACCEPTED_NAME\tCOL:001417c6-d3fc-4f42-aa3d-b1de3a592e58\tCheilostomatida incertae sedis\t\tsuborder\t\tBiota | Animalia | Bryozoa | Gymnolaemata | Cheilostomatida | Cheilostomatida incertae sedis\tCOL:5T6MX | COL:N | COL:622CG | COL:8ZXG2 | COL:84JWL | COL:001417c6-d3fc-4f42-aa3d-b1de3a592e58\tunranked | kingdom | phylum | class | order | suborder\t|  |  | Allman, 1856 | Busk, 1852 |\thttps://www.catalogueoflife.org/data/taxon/001417c6-d3fc-4f42-aa3d-b1de3a592e58"

)
public class CmdDump extends CmdOutput {

    @Override
    public void run() {
        List<RowHandler> handlers = MatchUtil.getAppendingRowHandlers(
                this,
                getIncludeHeader(),
                getOutputFormat(),
                System.out
        );

        try {
            for (RowHandler handler : handlers) {
                handler.onRow(
                        TermMatchUtil.wildcardRowForSchema(getInputSchema())
                );
            }
        } catch (PropertyEnricherException e) {
            throw new RuntimeException("failed to dump term list", e);
        }
    }

}
