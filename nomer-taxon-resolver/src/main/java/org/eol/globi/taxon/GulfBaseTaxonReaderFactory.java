package org.eol.globi.taxon;

import org.apache.http.client.cache.Resource;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.FileUtils;
import org.eol.globi.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class GulfBaseTaxonReaderFactory implements TaxonReaderFactory {
    private static final String[] DATA_FILES = {
            "Acanthocephala_O.csv.gz", "Chaetognatha.csv.gz", "Gastrotricha.csv.gz", "Pinophyta.csv.gz",
            "Acoelomorpha.csv.gz", "Chlorophyta.csv.gz", "Gnathostomulida.csv.gz", "Platyhelminthes.csv.gz",
            "Annelida_O.csv.gz", "Chordata_O.csv.gz", "Hemichordata.csv.gz", "Polypodiophyta.csv.gz",
            "Arthropoda_O.csv.gz", "Cnidaria.csv.gz", "Kinorhyncha.csv.gz", "Porifera_O.csv.gz",
            "Ascomycota_O.csv.gz", "Ctenophora.csv.gz", "Loricifera.csv.gz", "Priapulida.csv.gz",
            "Bacillariophyta.csv.gz", "Dicyemida.csv.gz", "Magnoliophyta.csv.gz", "Rhodophyta.csv.gz",
            "Basidiomycota.csv.gz", "Echinodermata.csv.gz", "Mollusca.csv.gz", "Rotifera_O.csv.gz",
            "Brachiopoda.csv.gz", "Echiura.csv.gz", "Nematoda.csv.gz", "Tardigrada.csv.gz",
            "Bryophyta.csv.gz", "Entoprocta.csv.gz", "Nemertea.csv.gz",
            "Bryozoa.csv.gz", "Foraminifera.csv.gz", "Phoronida.csv.gz"};


    @Override
    public Map<String, Resource> getResources() throws IOException {
        Map<String, Resource> readers = new HashMap<String, Resource>();
        for (String filename : DATA_FILES) {
            String resourceName = "/org/eol/globi/taxon/gulfbase/" + filename;
            Resource resource = new Resource() {

                @Override
                public InputStream getInputStream() throws IOException {
                    InputStream resourceAsStream = getClass().getResourceAsStream(resourceName);
                    if (null == resourceAsStream) {
                        throw new IOException("failed to open resource with name [" + resourceName + "]");
                    }

                    return new GZIPInputStream(resourceAsStream);
                }

                @Override
                public long length() {
                    return new File(getClass().getResource(resourceName).getFile()).length();
                }

                @Override
                public void dispose() {

                }
            };

            readers.put(resourceName, resource);
        }
        return readers;
    }
}
