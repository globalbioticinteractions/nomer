# nomer
[![Build Status](https://travis-ci.org/globalbioticinteractions/nomer.svg?branch=master)](https://travis-ci.org/globalbioticinteractions/nomer) [![standard-readme compliant](https://img.shields.io/badge/standard--readme-OK-green.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
[![DOI](https://zenodo.org/badge/117019305.svg)](https://zenodo.org/badge/latestdoi/117019305)

Maps identifiers and names to taxonomic names and ontological terms. 

Standard out (stdout) is used for results, and standard error (stderr) is used for logging (e.g., progress reporting). Designed to work with [*nix pipes](https://en.wikipedia.org/wiki/Pipeline_%28Unix%29) or as simple commandline tool.

```Nomer``` expects tab separated input in form of ```[term id]\t[term name]```. To change this default behavior, you can select the columns to be used for id/name selection by defining an alternate ```nomer.schema``` property. See ```properties``` command to list available properties.

Different kind of matchers can be select to do the term matching. Offline matching is supported by some matchers like ```globi-cache```. Note that ```globi-cache``` will download a taxon map/cache archive initially, and re-uses the indexes until the cache in cleaned up. The cache itself can be archived so that results can be reproduced in a different environment without need to rebuild the term match index.` 

Matchers can be added by writing some java code that implements an interface.

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Examples](#examples)
- [Building](#building)
- [Contribute](#contribute)
- [License](#license)

## Install

### Official releases

You can use this project by including `nomer.jar` from one of the [releases](https://github.com/globalbioticinteractions/nomer/releases).

### Maven, Gradle, SBT
Nomer is made available through a [maven](https://maven.apache.org) repository.

To include ```nomer``` in your project, add the following sections to your pom.xml (or equivalent for sbt, gradle etc):
```
  <repositories>
    <repository>
        <id>depot.globalbioticinteractions.org</id>
        <url>https://depot.globalbioticinteractions.org/release</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>org.globalbioticinteractions</groupId>
      <artifactId>nomer</artifactId>
      <version>0.0.1</version>
    </dependency>
  </dependencies>
```

### Building

Please use [maven](https://maven.apache.org) version 3.3+ , otherwise you might find issues like [this one](https://github.com/globalbioticinteractions/nomer/issues/3).

* Clone this repository
* Run tests using `mvn test` (optional).
* Run `mvn package -DskipTests` to build (standalone) jar
* Copy `nomer/target/nomer-[version]-jar-with-dependencies.jar` to ```[some dir]/nomer.jar```

## Usage

```
Usage: <main class> [command] [command options]
  Commands:
    version      Show Version
      Usage: version

    append      Append term matches to row
      Usage: append [options] [matcher]
        Options:
          --properties, -p
            point to properties file to override defaults.
            Default: <empty string>

    append-json      embeds term matches into json
      Usage: append-json [options] [matcher]
        Options:
          --properties, -p
            point to properties file to override defaults.
            Default: <empty string>

    validate-term      Validate terms
      Usage: validate-term [options]
        Options:
          --properties, -p
            point to properties file to override defaults.
            Default: <empty string>

    validate-term-link      Validate term links
      Usage: validate-term-link [options]
        Options:
          --properties, -p
            point to properties file to override defaults.
            Default: <empty string>

    matchers      Lists all or selected matcher configuration(s)
      Usage: matchers

    clean      Cleans term matcher cache.
      Usage: clean [options] [matcher]
        Options:
          --properties, -p
            point to properties file to override defaults.
            Default: <empty string>

    properties      Lists properties.
      Usage: properties [options]
        Options:
          --properties, -p
            point to properties file to override defaults.
            Default: <empty string>
```

## Examples 

### Show version

```java -jar nomer.jar version```

### Match term by id with default matcher

```echo -e "NCBI:9606\t" | java -Xmx4G -jar nomer.jar append > matches.tsv```

### Match term by name with default matcher

```echo -e "\tHomo sapiens" | java -Xmx4G -jar nomer.jar append > matches.tsv```

matches.tsv should now include entries like

```
	Homo sapiens	SAME_AS	EOL:327955	Homo sapiens	Species	إنسان @ar | Insan @az | човешки @bg | মানবীয় @bn | Ljudsko biće @bs | Humà @ca | Muž @cs | Menneske @da | Mensch @de | ανθρώπινο ον @el | Humans @en | Humano @es | Gizakiaren @eu | Ihminen @fi | Homme @fr | Mutum @ha | אנושי @he | մարդու @hy | Umano @it | ადამიანის @ka | Homo @la | žmogaus @lt | Om @mo | Mens @nl | Òme @oc | Om @ro | Человек разумный современный @ru | Qenie Njerëzore @sq | மனிதன் @ta | మానవుడు @te | Aadmi @ur | umuntu @zu |	Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens	EOL:1 | EOL:3014411 | EOL:8814528 | EOL:694 | EOL:2774383 | EOL:12094272 | EOL:4712200 | EOL:1642 | EOL:57446 | EOL:2844801 | EOL:1645 | EOL:10487985 | EOL:10509493 | EOL:4529848 | EOL:1653 | EOL:10551052 | EOL:42268 | EOL:327955	kingdom | subkingdom | infrakingdom | division | subdivision | infraphylum | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species	http://eol.org/pages/327955	http://media.eol.org/content/2014/08/07/23/02836_98_68.jpg
```
### Match term by id with selected matcher

### ITIS

```echo -e "ITIS:180547" | java -jar nomer.jar append globi-enrich```

where expected output includes:
```
ITIS:180547 SAME_AS ITIS:180547 Enhydra lutris  Species     Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Carnivora | Caniformia | Mustelidae | Lutrinae | Enhydra | Enhydra lutris ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180539 | ITIS:552303 | ITIS:180545 | ITIS:552326 | ITIS:180546 | ITIS:180547   Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Order | Suborder | Family | Subfamily | Genus | Species   http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=180547
```

### NCBI

```echo -e "NCBI:9606" | java -jar nomer.jar append globi-enrich```

expected output includes
```
NCBI:9606	SAME_AS	NCBI:9606	Homo sapiens	species	man @en | human @en	cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens	NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606	| superkingdom |  | kingdom |  |  |  | phylum | subphylum |  |  |  |  |  |  |  |  | class |  |  |  | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | specieshttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606
```
### Match term by name with selected matcher

```echo -e "\tCanis lupus" | java -jar nomer.jar append globi-globalnames```

expected output includes tab separated lines like, where the first two columns are the input and the following columns are match results

```
	Canis lupus	SAME_AS	NCBI:9612	Canis lupus	species		| Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Laurasiatheria | Carnivora | Caniformia | Canidae | Canis | Canis lupus	NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314145 | NCBI:33554 | NCBI:379584 | NCBI:9608 | NCBI:9611 | NCBI:9612	| superkingdom |  | kingdom |  |  |  | phylum | subphylum |  |  |  |  |  |  |  |  | class |  |  |  | superorder | order | suborder | family | genus | species	https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9612
	Canis lupus	SAME_AS	OTT:247341	Canis lupus	species		|  | Eukaryota | Opisthokonta | Holozoa | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Laurasiatheria | Carnivora | Caniformia | Canidae | Canis | Canis lupus	OTT:805080 | OTT:93302 | OTT:304358 | OTT:332573 | OTT:5246131 | OTT:691846 | OTT:641038 | OTT:117569 | OTT:147604 | OTT:125642 | OTT:947318 | OTT:801601 | OTT:278114 | OTT:114656 | OTT:114654 | OTT:458402 | OTT:4940726 | OTT:229562 | OTT:229560 | OTT:244265 | OTT:229558 | OTT:683263 | OTT:5334778 | OTT:392223 | OTT:44565 | OTT:827263 | OTT:770319 | OTT:372706 | OTT:247341	no rank | no rank | domain | no rank | no rank | kingdom | no rank | no rank | no rank | phylum | subphylum | subphylum | superclass | no rank | no rank | class | no rank | superclass | no rank | class | subclass | no rank | no rank | superorder | order | suborder | family | genus | species	https://tree.opentreeoflife.org/opentree/ottol@247341
	Canis lupus	SAME_AS	INAT_TAXON:42048	Canis lupus	speciesAnimalia | Chordata | Mammalia | Carnivora | Canidae | Canis | Canis lupus	kingdom | phylum | class | order | family | genus | species	http://inaturalist.org/taxa/42048
	Canis lupus	SAME_AS	ITIS:180596	Canis lupus	Species		Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Carnivora | Caniformia | Canidae | Canis | Canis lupus	ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180539 | ITIS:552303 | ITIS:180594 | ITIS:180595 | ITIS:180596	Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Order | Suborder | Family | Genus | Species	http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=180596
	Canis lupus	SAME_AS	IRMNG:11407661	Canis lupus	species		Animalia | Chordata | Mammalia | Carnivora | Canidae | Canis | Canis lupus	IRMNG:11 | IRMNG:148 | IRMNG:1310 | IRMNG:12116 | IRMNG:104585 | IRMNG:1282727 | IRMNG:11407661	kingdom | phylum | class | order | family | genus | species	http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=11407661
	Canis lupus	SAME_AS	GBIF:5219173	Canis lupus	species		Animalia | Chordata | Mammalia | Carnivora | Canidae | Canis | Canis lupus	GBIF:1 | GBIF:44 | GBIF:359 | GBIF:732 | GBIF:9701 | GBIF:5219142 | GBIF:5219173kingdom | phylum | class | order | family | genus | species	http://www.gbif.org/species/5219173
```

### validate taxonCache and taxonMap

 To validate terms (aka TaxonCache) and term linkages (aka TaxonMap) to be used with the offline term matchers, you can use the ```validate-term``` and ```validate-term-link``` commands. 

For instance, if you'd like to validate the first 10 lines of the taxonCache as published in https://zenodo.org/record/1213465 do:

```curl -L "https://zenodo.org/record/1213465/files/taxonCacheFirst10.tsv" | java -jar nomer.jar validate-term```

Expected result looks something like ```[FAIL|OK]\t[validation test]\t[...]``` where [...] is the validated line. Parts of the result of the above command includes:

```
OK	9 columns	4701dc84-660a-4c51-bd16-593997f2370b	Coelomomyces iliensis	species		Fungi | Chytridiomycota | Blastocladiomycetes | Blastocladiales | Coelomomycetaceae | Coelomomyces | Coelomomyces iliensis	urn:lsid:indexfungorum.org:names:90156 | urn:lsid:indexfungorum.org:names:90736 | urn:lsid:indexfungorum.org:names:90742 | urn:lsid:indexfungorum.org:names:90414 | urn:lsid:indexfungorum.org:names:80619 | urn:lsid:indexfungorum.org:names:20136 | 4701dc84-660a-4c51-bd16-593997f2370b	kingdom | phylum | class | order | family | genus | species
FAIL	supported id	4701dc84-660a-4c51-bd16-593997f2370b	Coelomomyces iliensis	species		Fungi | Chytridiomycota | Blastocladiomycetes | Blastocladiales | Coelomomycetaceae | Coelomomyces | Coelomomyces iliensis	urn:lsid:indexfungorum.org:names:90156 | urn:lsid:indexfungorum.org:names:90736 | urn:lsid:indexfungorum.org:names:90742 | urn:lsid:indexfungorum.org:names:90414 | urn:lsid:indexfungorum.org:names:80619 | urn:lsid:indexfungorum.org:names:20136 | 4701dc84-660a-4c51-bd16-593997f2370b	kingdom | phylum | class | order | family | genus | species
FAIL	prefixed id	4701dc84-660a-4c51-bd16-593997f2370b	Coelomomyces iliensis	species		Fungi | Chytridiomycota | Blastocladiomycetes | Blastocladiales | Coelomomycetaceae | Coelomomyces | Coelomomyces iliensis	urn:lsid:indexfungorum.org:names:90156 | urn:lsid:indexfungorum.org:names:90736 | urn:lsid:indexfungorum.org:names:90742 | urn:lsid:indexfungorum.org:names:90414 | urn:lsid:indexfungorum.org:names:80619 | urn:lsid:indexfungorum.org:names:20136 | 4701dc84-660a-4c51-bd16-593997f2370b	kingdom | phylum | class | order | family | genus | species
```

This validation report tell us that the line starting with ```4701dc84-660a-4c51-bd16-593997f2370b    Coelomomyces iliensis``` has (expected) 9 columns, but has an id that is not supported by nomer nor does the id conform to the ```[some namespace]:[some id]``` format. Note that the GloBI Taxon Graph publication at http://doi.org/10.5281/zenodo.1213465 prompted the development of the validation features. For more historic context, please see https://github.com/globalbioticinteractions/nomer/issues/5 .

A similar feature for term links (aka TaxonMap) are available through the command ```validate-term-link```. 

## Contribute

Feel free to join in. All welcome. Open an [issue](https://github.com/globalbioticinteractions/nomer/issues)!

## License

[GPL](LICENSE)
