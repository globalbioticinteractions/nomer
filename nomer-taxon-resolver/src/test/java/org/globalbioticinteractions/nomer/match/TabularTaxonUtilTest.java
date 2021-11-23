package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.CSVTSVUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class TabularTaxonUtilTest {

    @Test
    public void indexPhthirapteraTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonical\n" +
                "TPT_v1,1,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,acuticeps,NA,,,\"Ferris, 1933\",,,,,,Haematopinus acuticeps\n" +
                "TPT_v1,2,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,apri,NA,,,\"Goureau, 1866\",,,,,,Haematopinus apri\n" +
                "TPT_v1,3,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,asini,NA,,,\"(Linnaeus, 1758)\",,,,,,Haematopinus asini\n" +
                "TPT_v1,4,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,breviculus,NA,,,\"Fahrenholz, 1939\",,,,,,Haematopinus breviculus\n" +
                "TPT_v1,5,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,bufali,NA,,,\"(de Geer, 1778)\",,,,,,Haematopinus bufali\n" +
                "TPT_v1,6,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,channabasavannai,NA,,,\"Krishna Rao, Khuddus & Kuppuswamy, 1977\",,,,,,Haematopinus channabasavannai\n" +
                "TPT_v1,7,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,eurysternus,NA,,,\"Denny, 1842\",,,,,,Haematopinus eurysternus\n" +
                "TPT_v1,8,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,gorgonis,NA,,,\"Werneck, 1952\",,,,,,Haematopinus gorgonis\n" +
                "TPT_v1,9,,0,,,,,,,,,,,,,,,,,,,Psocodea,Troctomorpha,Nanopsocetae,Phthiraptera,Anoplura,NA,Haematopinidae,,,,Haematopinus,NA,jeannereti,NA,,,\"Paulian & Pajot, 1966\",,,,,,Haematopinus jeannereti\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Taxon taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertThat(taxon.getAuthorship(), Is.is("Ferris, 1933"));
        assertThat(taxon.getPath(), Is.is("Psocodea | Troctomorpha | Nanopsocetae | Phthiraptera | Anoplura | Haematopinidae | Haematopinus | acuticeps"));
        assertThat(taxon.getPathNames(), Is.is("order | suborder | infraorder | parvorder | nanorder | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("TPT:1"));
        assertThat(taxon.getName(), Is.is("Haematopinus acuticeps"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("TPT_v1"));

        labeledCSVParser.getLine();

        taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertThat(taxon.getAuthorship(), Is.is("Goureau, 1866"));
        assertThat(taxon.getPath(), Is.is("Psocodea | Troctomorpha | Nanopsocetae | Phthiraptera | Anoplura | Haematopinidae | Haematopinus | apri"));
        assertThat(taxon.getPathNames(), Is.is("order | suborder | infraorder | parvorder | nanorder | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("TPT:2"));
        assertThat(taxon.getName(), Is.is("Haematopinus apri"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("TPT_v1"));


    }

    @Test
    public void indexAcariTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonicalName\n" +
                "TPT,6995,,,,,,,,Allothyridae,,,,,,,,Animalia,Arthropoda,Arachnida,Acari,Parasitiformes,Holothyrida,,,,,Holothyroidea,Allothyridae,,,,,,,,family,,,,,,,,Allothyridae\n" +
                "GBIF,10766101,,,10682343,,,,,\"Dicrogonatus gardineri (Warburton, 1912)\",,,,,,,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Dicrogonatus,,gardineri,,species,,\"(Warburton, 1912)\",,,accepted,,,Dicrogonatus gardineri\n" +
                "GBIF,10857865,,,10682343,,,,,\"Dicrogonatus niger (Thon, 1906)\",,,,,,,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Dicrogonatus,,niger,,species,,\"(Thon, 1906)\",,,accepted,,,Dicrogonatus niger\n" +
                "GBIF,6892347,,,4663001,6892348,,,,\"Haplothyrus expolitissimus (Berlese, 1923)\",,,,,,,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Haplothyrus,,expolitissimus,,species,,\"(Berlese, 1923)\",,,accepted,,,Haplothyrus expolitissimus\n" +
                "GBIF,6892348,,6892347,4663001,,,,,\"Holothyrus expolitissimus Berlese, 1923\",Haplothyrus expolitissimus,,,,,,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Haplothyrus,,expolitissimus,,species,,\"Berlese, 1923\",,,synonym,,,Holothyrus expolitissimus\n" +
                "GBIF,4664671,,,4664666,,,,,\"Holothyrus australasiae Womersley, 1935\",,,,,\"Womersley, Herbert. 1935. A species of Acarina of the genus Holothyrus from Australia and New Zealand. Annals and Magazine of Natural History, Including Zoology, Botany and Geology, Being a Continuation of the 'Magazine of Botany and Zoology', and of Louden and Charlesworth's 'Magazine of Natural History', Series 10 16(91): 154-157, plate VIII.\",,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Holothyrus,,australasiae,,species,,\"Womersley, 1935\",,,accepted,,,Holothyrus australasiae\n" +
                "GBIF,6892351,,,4664666,,,,,\"Holothyrus coccinella Gervais, 1842\",,,,,\"Gervais, F. L. P. 1842. Annales de la Soci̩t̩ Entomologique de France, S̩ries 1-6 11: xlv���xlviii.\",,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Holothyrus,,coccinella,,species,,\"Gervais, 1842\",,,accepted,,,Holothyrus coccinella\n" +
                "GBIF,4664673,,,4664666,,,,,\"Holothyrus constrictus Domrow, 1955\",,,,,\"Domrow, Robert. 1955. A second species of Holothyrus (Acarina: Holothyroidea) from Australia. Proceedings of the Linnean Society of New South Wales 79(375-376): 159-162.\",,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Holothyrus,,constrictus,,species,,\"Domrow, 1955\",,,accepted,,,Holothyrus constrictus\n" +
                "GBIF,6892349,,,4660398,,,,,\"Lindothyrus rubellus Lehtinen, 1995\",,,,,,,,Animalia,Arthropoda,,,,Holothyrida,,,,,,Holothyridae,,,,Lindothyrus,,rubellus,,species,,\"Lehtinen, 1995\",,,accepted,,,Lindothyrus rubellus\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Taxon taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertNull(taxon.getAuthorship());
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Arachnida | Acari | Parasitiformes | Holothyrida | Holothyroidea | Allothyridae"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | subclass | superorder | order | superfamily | family"));
        assertThat(taxon.getId(), Is.is("TPT:6995"));
        assertThat(taxon.getName(), Is.is("Allothyridae"));
        assertThat(taxon.getRank(), Is.is("family"));
        assertThat(taxon.getNameSource(), Is.is("TPT"));

        labeledCSVParser.getLine();

        taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertThat(taxon.getAuthorship(), Is.is("(Warburton, 1912)"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Holothyrida | Holothyridae | Dicrogonatus | gardineri"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | order | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("GBIF:10766101"));
        assertThat(taxon.getName(), Is.is("Dicrogonatus gardineri"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("GBIF"));


    }


    @Test
    public void indexIxodesTable() throws IOException {
        String table = "source,taxonID,scientificNameID,acceptedNameUsageID,parentNameUsageID,originalNameUsageID,nameAccordingToID,namePublishedInID,taxonConceptID,scientificName,acceptedNameUsage,parentNameUsage,originalNameUsage,nameAccordingTo,namePublishedIn,namePublishedInYear,higherClassification,kingdom,phylum,class,subclass,superorder,order,suborder,infraorder,parvorder,nanorder,superfamily,family,subfamily,tribe,subtribe,genus,infragenericEpithet,specificEpithet,infraspecificEpithet,taxonRank,verbatimTaxonRank,scientificNameAuthorship,vernacularName,nomenclaturalCode,taxonomicStatus,nomenclaturalStatus,taxonRemarks,canonical\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,abdussalami,,,,HOOGSTRAAL  MCCARTHY 1965,,,,,,Argas abdussalami\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,africolumbae,,,,HOOGSTRAAL  KAISER  WALKER  LEDGER,,,,,,Argas africolumbae\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,arboreus,,,,KAISER  HOOGSTRAAL  KOHLS 1964,,,,,,Argas arboreus\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,argas,,,,KOHLS  HOOGSTRAAL,,,,,,Argas argas\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,australiensis,,,,KOHLS HOOGSTRAL 1962,,,,,,Argas australiensis\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,boueti,,,,ROUBAUD  COLAS-BELCOUR,,,,,,Argas boueti\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,brevipes,,,,BANKS,,,,,,Argas brevipes\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,brumpti,,,,NEUMANN,,,,,,Argas brumpti\n" +
                ",,,,,,,,,,,,,,,,,,,Arachnida,Acari,Parasitiformes,Ixodida,,,,,Ixodoidea,Argasidae,,,,Argas,,bubalornis,,,,,,,,,,Argas bubalornis\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Taxon taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertThat(taxon.getAuthorship(), Is.is("HOOGSTRAAL  MCCARTHY 1965"));
        assertThat(taxon.getPath(), Is.is("Arachnida | Acari | Parasitiformes | Ixodida | Ixodoidea | Argasidae | Argas | abdussalami"));
        assertThat(taxon.getPathNames(), Is.is("class | subclass | superorder | order | superfamily | family | genus | specificEpithet"));
        assertNull(taxon.getId());
        assertThat(taxon.getName(), Is.is("Argas abdussalami"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertNull(taxon.getNameSource());

        labeledCSVParser.getLine();

        taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertThat(taxon.getAuthorship(), Is.is("HOOGSTRAAL  KAISER  WALKER  LEDGER"));
        assertThat(taxon.getPath(), Is.is("Arachnida | Acari | Parasitiformes | Ixodida | Ixodoidea | Argasidae | Argas | africolumbae"));
        assertThat(taxon.getPathNames(), Is.is("class | subclass | superorder | order | superfamily | family | genus | specificEpithet"));
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

        Taxon taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertThat(taxon.getAuthorship(), Is.is("Lewis, 1968"));
        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Siphonaptera | Ancistropsyllidae | Ancistropsylla | nepalensis"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | specificEpithet"));
        assertThat(taxon.getId(), Is.is("224"));
        assertThat(taxon.getName(), Is.is("Ancistropsylla nepalensis"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getNameSource(), Is.is("Lewis, CoL, GBIF, NMNH"));

        labeledCSVParser.getLine();

        taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

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
                ",,,,,,,,,Abrocomidae,,Rodentia,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,,,,,,,,,,,,140, \n" +
                ",,,,,,,,,Abrocoma bennettii,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,bennettii,,,,,,,,,220,Abrocoma bennettii\n" +
                ",,,,,,,,,Abrocoma boliviensis,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,boliviensis,,,,,,,,,220,Abrocoma boliviensis\n" +
                ",,,,,,,,,Abrocoma budini,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,budini,,,,,,,,,220,Abrocoma budini\n" +
                ",,,,,,,,,Abrocoma cinerea,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,cinerea,,,,,,,,,220,Abrocoma cinerea\n" +
                ",,,,,,,,,Abrocoma famatina,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,famatina,,,,,,,,,220,Abrocoma famatina\n" +
                ",,,,,,,,,Abrocoma schistacea,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,schistacea,,,,,,,,,220,Abrocoma schistacea\n" +
                ",,,,,,,,,Abrocoma uspallata,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,uspallata,,,,,,,,,220,Abrocoma uspallata\n" +
                ",,,,,,,,,Abrocoma vaccarum,,Abrocoma,,,,,,,,Mammalia,,,Rodentia,,,,,,Abrocomidae,,,,Abrocoma,,vaccarum,,,,,,,,,220,Abrocoma vaccarum\n";

        InputStream inputStream = IOUtils.toInputStream(table, StandardCharsets.UTF_8);
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(inputStream);

        labeledCSVParser.getLine();

        Taxon taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertThat(taxon.getPath(), Is.is("Mammalia | Rodentia | Abrocomidae"));
        assertThat(taxon.getPathNames(), Is.is("class | order | family"));
        assertNull(taxon.getAuthorship());
        assertNull(taxon.getId());
        assertThat(taxon.getName(), Is.is("Abrocomidae"));
        assertThat(taxon.getRank(), Is.is("family"));
        assertNull(taxon.getNameSource());

        labeledCSVParser.getLine();

        taxon = TabularTaxonUtil.parseLine(labeledCSVParser);

        assertNull(taxon.getAuthorship());
        assertThat(taxon.getPath(), Is.is("Mammalia | Rodentia | Abrocomidae | Abrocoma | bennettii"));
        assertThat(taxon.getPathNames(), Is.is("class | order | family | genus | specificEpithet"));
        assertNull(taxon.getId());
        assertThat(taxon.getName(), Is.is("Abrocoma bennettii"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertNull(taxon.getNameSource());


    }

}