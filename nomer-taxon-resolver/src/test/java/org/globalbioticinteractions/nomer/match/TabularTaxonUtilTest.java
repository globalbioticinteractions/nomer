package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class TabularTaxonUtilTest {

    @Test
    public void indexPhthirapteraTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonicalName\n" +
                "TPT_v1,1,,,,,,,,\"Haematopinus acuticeps Ferris, 1933\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,acuticeps,,species,,\"Ferris, 1933\",,,,,,Haematopinus acuticeps\n" +
                "TPT_v1,2,,,,,,,,\"Haematopinus apri Goureau, 1866\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,apri,,species,,\"Goureau, 1866\",,,,,,Haematopinus apri\n" +
                "TPT_v1,3,,,,,,,,\"Haematopinus asini (Linnaeus, 1758)\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,asini,,species,,\"(Linnaeus, 1758)\",,,,,,Haematopinus asini\n" +
                "TPT_v1,4,,,,,,,,\"Haematopinus breviculus Fahrenholz, 1939\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,breviculus,,species,,\"Fahrenholz, 1939\",,,,,,Haematopinus breviculus\n" +
                "TPT_v1,5,,,,,,,,\"Haematopinus bufali (de Geer, 1778)\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,bufali,,species,,\"(de Geer, 1778)\",,,,,,Haematopinus bufali\n" +
                "TPT_v1,6,,,,,,,,\"Haematopinus channabasavannai Krishna Rao, Khuddus & Kuppuswamy, 1977\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,channabasavannai,,species,,\"Krishna Rao, Khuddus & Kuppuswamy, 1977\",,,,,,Haematopinus channabasavannai\n" +
                "TPT_v1,7,,,,,,,,\"Haematopinus eurysternus Denny, 1842\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,eurysternus,,species,,\"Denny, 1842\",,,,,,Haematopinus eurysternus\n" +
                "TPT_v1,8,,,,,,,,\"Haematopinus gorgonis Werneck, 1952\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,gorgonis,,species,,\"Werneck, 1952\",,,,,,Haematopinus gorgonis\n" +
                "TPT_v1,9,,,,,,,,\"Haematopinus jeannereti Paulian & Pajot, 1966\",,,,,,,,Animalia,Arthropoda,Insecta,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,,Haematopinidae,,,,Haematopinus,,jeannereti,,species,,\"Paulian & Pajot, 1966\",,,,,,Haematopinus jeannereti\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation1 = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        Taxon taxon = nameRelation1.getLeft();

        assertThat(taxon.getAuthorship(), Is.is("Ferris, 1933"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Psocodea | Troctomorpha | Nanopsocetae | Phthiraptera | Anoplura | Haematopinidae | Haematopinus | acuticeps"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | suborder | infraorder | parvorder | nanorder | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("1"));
        assertThat(taxon.getName(), Is.is("Haematopinus acuticeps"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("TPT_v1"));

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        taxon = nameRelation.getLeft();

        assertThat(taxon.getAuthorship(), Is.is("Goureau, 1866"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Psocodea | Troctomorpha | Nanopsocetae | Phthiraptera | Anoplura | Haematopinidae | Haematopinus | apri"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | suborder | infraorder | parvorder | nanorder | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("2"));
        assertThat(taxon.getName(), Is.is("Haematopinus apri"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("TPT_v1"));


    }

    @Test
    public void indexAcariTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonicalName\n" +
                "TPT,acari_6995,,,,,,,,Allothyridae,,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Holothyrida,,,,,Holothyroidea,Allothyridae,,,,,,,,family,,,,,,,,Allothyridae\n" +
                "GBIF,Acari_10682343,,,6996,,,,,\"Dicrogonatus Gerlach, Lehtinen & Madl, 2010\",,,,,\"Gerlach, J., & Marusik, Y. M. (Eds.). (2010). Arachnida and Myriapoda of the Seychelles islands. Siri Scientific Press.\",,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Dicrogonatus,,,,genus,,\"Gerlach, Lehtinen & Madl, 2010\",,,accepted,,,Dicrogonatus\n" +
                "GBIF,acari_10766101,,,10682343,,,,,\"Dicrogonatus gardineri (Warburton, 1912)\",,,,,,,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Dicrogonatus,,gardineri,,species,,\"(Warburton, 1912)\",,,accepted,,,Dicrogonatus gardineri\n" +
                "GBIF,acari_10857865,,,10682343,,,,,\"Dicrogonatus niger (Thon, 1906)\",,,,,,,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Dicrogonatus,,niger,,species,,\"(Thon, 1906)\",,,accepted,,,Dicrogonatus niger\n" +
                "GBIF,acari_6892347,,,4663001,6892348,,,,\"Haplothyrus expolitissimus (Berlese, 1923)\",,,,,,,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Haplothyrus,,expolitissimus,,species,,\"(Berlese, 1923)\",,,accepted,,,Haplothyrus expolitissimus\n" +
                "GBIF,acari_6892348,,6892347,4663001,,,,,\"Holothyrus expolitissimus Berlese, 1923\",Haplothyrus expolitissimus,,,,,,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Holothyrus,,expolitissimus,,species,,\"Berlese, 1923\",,,synonym,,,Holothyrus expolitissimus\n" +
                "GBIF,acari_4664671,,,4664666,,,,,\"Holothyrus australasiae Womersley, 1935\",,,,,\"Womersley, Herbert. 1935. A species of Acarina of the genus Holothyrus from Australia and New Zealand. Annals and Magazine of Natural History, Including Zoology, Botany and Geology, Being a Continuation of the 'Magazine of Botany and Zoology', and of Louden and Charlesworth's 'Magazine of Natural History', Series 10 16(91): 154-157, plate VIII. \",,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Holothyrus,,australasiae,,species,,\"Womersley, 1935\",,,accepted,,,Holothyrus australasiae\n" +
                "GBIF,acari_6892351,,,4664666,,,,,\"Holothyrus coccinella Gervais, 1842\",,,,,\"Gervais, F. L. P. 1842. Annales de la Société Entomologique de France, Series 1-6 11: xlv_��xlviii. \",,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Holothyrus,,coccinella,,species,,\"Gervais, 1842\",,,accepted,,,Holothyrus coccinella\n" +
                "GBIF,acari_4664673,,,4664666,,,,,\"Holothyrus constrictus Domrow, 1955\",,,,,\"Domrow, Robert. 1955. A second species of Holothyrus (Acarina: Holothyroidea) from Australia. Proceedings of the Linnean Society of New South Wales 79(375-376): 159-162. \",,,Animalia,Arthropoda,Arachnida,,,Holothyrida,,,,,,Holothyridae,,,,Holothyrus,,constrictus,,species,,\"Domrow, 1955\",,,accepted,,,Holothyrus constrictus\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation1 = TabularTaxonUtil.parseNameRelations(labeledCSVParser);

        assertThat(TaxonUtil.isResolved(nameRelation1.getRight()), Is.is(true));
        assertThat(nameRelation1.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        Taxon taxon = nameRelation1.getLeft();

        assertNull(taxon.getAuthorship());
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Arachnida | Acari | Parasitiformes | Holothyrida | Holothyroidea | Allothyridae"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | subclass | superorder | order | superfamily | family"));
        assertThat(taxon.getId(), Is.is("acari_6995"));
        assertThat(taxon.getName(), Is.is("Allothyridae"));
        assertThat(taxon.getRank(), Is.is("family"));
        assertThat(taxon.getAuthorship(), Is.is(nullValue()));
        assertThat(taxon.getNameSource(), Is.is("TPT"));

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        taxon = nameRelation.getLeft();

        assertThat(taxon.getAuthorship(), Is.is("Gerlach, Lehtinen & Madl, 2010"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Dicrogonatus"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus"));
        assertThat(taxon.getId(), Is.is("Acari_10682343"));
        assertThat(taxon.getName(), Is.is("Dicrogonatus"));
        assertThat(taxon.getRank(), Is.is("genus"));
        assertThat(taxon.getNameSource(), Is.is("GBIF"));
        assertThat(TaxonUtil.isResolved(nameRelation.getRight()), Is.is(true));

        for (int i = 0; i < 4; i++) {
            labeledCSVParser.getLine();
        }

        nameRelation = TabularTaxonUtil.parseNameRelations(labeledCSVParser);

        assertThat(nameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));

        Taxon resolvedTaxon = nameRelation.getRight();
        assertThat(resolvedTaxon.getExternalId(), Is.is("6892347"));
        assertThat(TaxonUtil.isResolved(resolvedTaxon), Is.is(false));
        taxon = nameRelation.getLeft();

        assertThat(taxon.getAuthorship(), Is.is("Berlese, 1923"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Holothyrus | expolitissimus"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("acari_6892348"));
        assertThat(taxon.getName(), Is.is("Holothyrus expolitissimus"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("GBIF"));

    }


    @Test
    public void indexIxodesTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonicalName\n" +
                ",,,,,,,,,\"Argas abdussalami Hoogstraal & Mccarthy, 1965\",,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,abdussalami,,species,,\"Hoogstraal & Mccarthy, 1965\",,,,,,Argas abdussalami\n" +
                ",,,,,,,,,\"Argas africolumbae Hoogstraal, Kaiser, Walker & Ledger\",,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,africolumbae,,species,,\"Hoogstraal, Kaiser, Walker & Ledger\",,,,,,Argas africolumbae\n" +
                ",,,,,,,,,\"Argas arboreus Kaiser, Hoogstraal & Kohls, 1964\",,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,arboreus,,species,,\"Kaiser, Hoogstraal & Kohls, 1964\",,,,,,Argas arboreus\n" +
                ",,,,,,,,,Argas argas Kohls & Hoogstraal,,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,argas,,species,,Kohls & Hoogstraal,,,,,,Argas argas\n" +
                ",,,,,,,,,\"Argas australiensis Kohls & Hoogstral, 1962\",,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,australiensis,,species,,\"Kohls & Hoogstral, 1962\",,,,,,Argas australiensis\n" +
                ",,,,,,,,,Argas boueti Roubaud & Colas-Belcour,,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,boueti,,species,,Roubaud & Colas-Belcour,,,,,,Argas boueti\n" +
                ",,,,,,,,,Argas brevipes Banks,,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,brevipes,,species,,Banks,,,,,,Argas brevipes\n" +
                ",,,,,,,,,Argas brumpti Neumann,,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,brumpti,,species,,Neumann,,,,,,Argas brumpti\n" +
                ",,,,,,,,,Argas bubalornis ,,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,bubalornis,,species,,,,,,,,Argas bubalornis\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation1 = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        Taxon taxon = nameRelation1.getLeft();

        assertNull(taxon.getId());
        assertThat(taxon.getName(), Is.is("Argas abdussalami"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getAuthorship(), Is.is("Hoogstraal & Mccarthy, 1965"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Arachnida | Acari | Parasitiformes | Ixodida | Ixodoidea | Argasidae | Argas | abdussalami"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | subclass | superorder | order | superfamily | family | genus | specificEpithet"));
        assertNull(taxon.getNameSource());

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        taxon = nameRelation.getLeft();

        assertThat(taxon.getAuthorship(), Is.is("Hoogstraal, Kaiser, Walker & Ledger"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Arachnida | Acari | Parasitiformes | Ixodida | Ixodoidea | Argasidae | Argas | africolumbae"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | subclass | superorder | order | superfamily | family | genus | specificEpithet"));
        assertNull(taxon.getId());
        assertThat(taxon.getName(), Is.is("Argas africolumbae"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertNull(taxon.getNameSource());


    }

    @Test
    public void indexSiphonapteraTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonicalName\n" +
                "\"Lewis, CoL, GBIF, NMNH\",224,,,,,,,,\"Ancistropsylla nepalensis Lewis, 1968\",,Ancistropsylla,,,,1968,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ancistropsyllidae,,,,Ancistropsylla,,nepalensis,,species,,\"Lewis, 1968\",,ICZN,accepted,,,Ancistropsylla nepalensis\n" +
                "\"Lewis, CoL, GBIF\",225,,,,,,,,\"Ancistropsylla roubaudi Toumanoff & Fuller, 1947\",,Ancistropsylla,,,,1947,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ancistropsyllidae,,,,Ancistropsylla,,roubaudi,,species,,\"Toumanoff & Fuller, 1947\",,ICZN,accepted,,,Ancistropsylla roubaudi\n" +
                "\"Lewis, CoL, GBIF\",226,,,,,,,,\"Ancistropsylla siamensis Smit & Toumanoff, 1952\",,Ancistropsylla,,,,1952,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ancistropsyllidae,,,,Ancistropsylla,,siamensis,,species,,\"Smit & Toumanoff, 1952\",,ICZN,accepted,,,Ancistropsylla siamensis\n" +
                "\"Lewis, CoL, GBIF\",3499,,,,,,,,\"Ancistropsylla Toumanoff & Fuller, 1947\",,Ancistropsyllidae,,,,1947,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ancistropsyllidae,,,,Ancistropsylla,,,,genus,,\"Toumanoff & Fuller, 1947\",,ICZN,accepted,,,Ancistropsylla\n" +
                "\"Lewis, CoL, GBIF\",3751,,,,,,,,Ancistropsyllidae,,Siphonaptera,,,,,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ancistropsyllidae,,,,,,,,family,,,,ICZN,accepted,,,Ancistropsyllidae\n" +
                "\"Lewis, GBIF\",1812,,3607,,,,,,\"Aceratophyllus Ewing, 19299\",Macrostylophora,Ceratophyllinae,,,,19299,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ceratophyllidae,Ceratophyllinae,,,Aceratophyllus,,,,genus,,\"Ewing, 19299\",,ICZN,synonym,,,Aceratophyllus\n" +
                "\"Lewis, CoL, GBIF\",58,,,,,,,,\"Aenigmopsylla grodekovi Sychevsky, 1950\",,Aenigmopsylla,,,,1950,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ceratophyllidae,Ceratophyllinae,,,Aenigmopsylla,,grodekovi,,species,,\"Sychevsky, 1950\",,ICZN,accepted,,,Aenigmopsylla grodekovi\n" +
                "\"Lewis, CoL, GBIF\",3488,,,,,,,,\"Aenigmopsylla Ioff, 1950\",,Ceratophyllinae,,,,1950,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ceratophyllidae,Ceratophyllinae,,,Aenigmopsylla,,,,genus,,\"Ioff, 1950\",,ICZN,accepted,,,Aenigmopsylla\n" +
                "\"Lewis, CoL, GBIF, NMNH\",60,,,,,,,,\"Aetheca thamba (Jordan, 1929)\",,Aetheca,,,,1929,,Animalia,Arthropoda,Insecta,,,Siphonaptera,,,,,,Ceratophyllidae,Ceratophyllinae,,,Aetheca,,thamba,,species,,\"(Jordan, 1929)\",,ICZN,accepted,,,Aetheca thamba\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation1 = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        Taxon taxon = nameRelation1.getLeft();

        assertThat(taxon.getAuthorship(), Is.is("Lewis, 1968"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Siphonaptera | Ancistropsyllidae | Ancistropsylla | nepalensis"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("224"));
        assertThat(taxon.getName(), Is.is("Ancistropsylla nepalensis"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("Lewis, CoL, GBIF, NMNH"));

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        taxon = nameRelation.getLeft();

        assertThat(taxon.getAuthorship(), Is.is("Toumanoff & Fuller, 1947"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Siphonaptera | Ancistropsyllidae | Ancistropsylla | roubaudi"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("225"));
        assertThat(taxon.getName(), Is.is("Ancistropsylla roubaudi"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("Lewis, CoL, GBIF"));


    }


    @Test
    public void indexMammaliaTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonicalName\n" +
                "ASM,100,,,,,,,,Afrosoricida,,Mammalia,,,,,,Animalia,Chordata,Mammalia,,,Afrosoricida,,,,,,,,,,,,,,order,,,,,,,100, \n" +
                "ASM,220,,,,,,,,Antilocapra americana,,Antilocapra,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Antilocapridae,,,,Antilocapra,,americana,,species,,,,,,,220,Antilocapra americana\n" +
                "ASM,180,,,,,,,,Antilocapra,,Antilocapridae,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Antilocapridae,,,,Antilocapra,,,,genus,,,,,,,180,Antilocapra \n" +
                "ASM,140,,,,,,,,Antilocapridae,,Artiodactyla,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Antilocapridae,,,,,,,,family,,,,,,,140, \n" +
                "ASM,220,,,,,,,,Balaena mysticetus,,Balaena,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Balaenidae,,,,Balaena,,mysticetus,,species,,,,,,,220,Balaena mysticetus\n" +
                "ASM,180,,,,,,,,Balaena,,Balaenidae,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Balaenidae,,,,Balaena,,,,genus,,,,,,,180,Balaena \n" +
                "ASM,220,,,,,,,,Eubalaena australis,,Eubalaena,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Balaenidae,,,,Eubalaena,,australis,,species,,,,,,,220,Eubalaena australis\n" +
                "ASM,220,,,,,,,,Eubalaena glacialis,,Eubalaena,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Balaenidae,,,,Eubalaena,,glacialis,,species,,,,,,,220,Eubalaena glacialis\n" +
                "ASM,220,,,,,,,,Eubalaena japonica,,Eubalaena,,,,,,Animalia,Chordata,Mammalia,,,Artiodactyla,,,,,,Balaenidae,,,,Eubalaena,,japonica,,species,,,,,,,220,Eubalaena japonica\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation1 = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        Taxon taxon = nameRelation1.getLeft();

        assertThat(taxon.getId(), Is.is("100"));
        assertThat(taxon.getName(), Is.is("Afrosoricida"));
        assertThat(taxon.getPath(), Is.is("Animalia | Chordata | Mammalia | Afrosoricida"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order"));
        assertNull(taxon.getAuthorship());
        assertThat(taxon.getRank(), Is.is("order"));
        assertThat(taxon.getNameSource(), Is.is("ASM"));

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        taxon = nameRelation.getLeft();

        assertNull(taxon.getAuthorship());
        assertThat(taxon.getPath(), Is.is("Animalia | Chordata | Mammalia | Artiodactyla | Antilocapridae | Antilocapra | americana"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("220"));
        assertThat(taxon.getName(), Is.is("Antilocapra americana"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("ASM"));


    }

    @Test
    public void indexAvesTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonicalName\n" +
                "IOC,,,,,,,,,Abeillia,,Trochilidae,,,,,,Animalia,Chordata,Aves,,,Caprimulgiformes,,,,,,Trochilidae,,,,Abeillia,,,,genus,,,,,accepted,180,,Abeillia\n" +
                "IOC,,,,,,,,,Abeillia abeillei,,Abeillia,,,,,,Animalia,Chordata,Aves,,,Caprimulgiformes,,,,,,Trochilidae,,,,Abeillia,,abeillei,,species,,,,,accepted,220,,Abeillia abeillei\n" +
                "IOC,,,,,,,,,Abeillia abeillei abeillei,,abeillei,,,,,,Animalia,Chordata,Aves,,,Caprimulgiformes,,,,,,Trochilidae,,,,Abeillia,,abeillei,abeillei,subspecies,,,,,accepted,230,,Abeillia abeillei abeillei\n" +
                "IOC,,,,,,,,,Abeillia abeillei aurea,,abeillei,,,,,,Animalia,Chordata,Aves,,,Caprimulgiformes,,,,,,Trochilidae,,,,Abeillia,,abeillei,aurea,subspecies,,,,,accepted,230,,Abeillia abeillei aurea\n" +
                "IOC,,,,,,,,,Abroscopus,,Scotocercidae,,,,,,Animalia,Chordata,Aves,,,Passeriformes,,,,,,Scotocercidae,,,,Abroscopus,,,,genus,,,,,accepted,180,,Abroscopus\n" +
                "IOC,,,,,,,,,Abroscopus albogularis,,Abroscopus,,,,,,Animalia,Chordata,Aves,,,Passeriformes,,,,,,Scotocercidae,,,,Abroscopus,,albogularis,,species,,,,,accepted,220,,Abroscopus albogularis\n" +
                "IOC,,,,,,,,,Abroscopus schisticeps,,Abroscopus,,,,,,Animalia,Chordata,Aves,,,Passeriformes,,,,,,Scotocercidae,,,,Abroscopus,,schisticeps,,species,,,,,accepted,220,,Abroscopus schisticeps\n" +
                "IOC,,,,,,,,,Abroscopus superciliaris,,Abroscopus,,,,,,Animalia,Chordata,Aves,,,Passeriformes,,,,,,Scotocercidae,,,,Abroscopus,,superciliaris,,species,,,,,accepted,220,,Abroscopus superciliaris\n" +
                "IOC,,,,,,,,,Abroscopus albogularis albogularis,,albogularis,,,,,,Animalia,Chordata,Aves,,,Passeriformes,,,,,,Scotocercidae,,,,Abroscopus,,albogularis,albogularis,subspecies,,,,,accepted,230,,Abroscopus albogularis albogularis\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation1 = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        Taxon taxon = nameRelation1.getLeft();

        assertThat(taxon.getPath(), Is.is("Animalia | Chordata | Aves | Caprimulgiformes | Trochilidae | Abeillia"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus"));
        assertNull(taxon.getAuthorship());
        assertNull(taxon.getId());
        assertThat(taxon.getName(), Is.is("Abeillia"));
        assertThat(taxon.getRank(), Is.is("genus"));
        assertThat(taxon.getNameSource(), Is.is("IOC"));

        labeledCSVParser.getLine();

        Triple<Taxon, NameType, Taxon> nameRelation = TabularTaxonUtil.parseNameRelations(labeledCSVParser);
        taxon = nameRelation.getLeft();

        assertNull(taxon.getId());
        assertThat(taxon.getName(), Is.is("Abeillia abeillei"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("IOC"));
        assertNull(taxon.getAuthorship());
        assertThat(taxon.getPath(), Is.is("Animalia | Chordata | Aves | Caprimulgiformes | Trochilidae | Abeillia | abeillei"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | specificEpithet"));

    }

}