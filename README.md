# nomer
[![Java CI](https://github.com/globalbioticinteractions/nomer/workflows/Java%20CI/badge.svg)](https://github.com/globalbioticinteractions/nomer/actions?query=workflow%3A%22Java+CI%22) [![standard-readme compliant](https://img.shields.io/badge/standard--readme-OK-green.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
[![DOI](https://zenodo.org/badge/117019305.svg)](https://zenodo.org/badge/latestdoi/117019305)

Maps identifiers and names to other identifiers and names. Nomer [[1]](#1) is a GloBI [[2]](#2) software tool, and relies on the Nomer Corpus of Taxonomic Resources [[3]](#3).

```
$ echo -e "\tHomo sapiens" | nomer append itis
	Homo sapiens	SAME_AS	ITIS:180092	Homo sapiens	species		Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens	ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180089 | ITIS:943773 | ITIS:943778 | ITIS:943782 | ITIS:180090 | ITIS:943805 | ITIS:180091 | ITIS:180092	kingdom | subkingdom | infrakingdom | phylum | subphylum | infraphylum | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species	http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=180092	
```

Standard out (stdout) is used for results, and standard error (stderr) is used for logging (e.g., progress reporting). Designed to work with [*nix pipes](https://en.wikipedia.org/wiki/Pipeline_%28Unix%29) or as simple commandline tool. 

```Nomer``` expects tab separated input in form of ```[term id]\t[term name]```. To change this default behavior, you can select the columns to be used for id/name selection by defining an alternate ```nomer.schema.*``` properties. See ```properties``` command to list available properties.

Different kind of matchers can be select to do the term matching. Offline matching is supported by some matchers like ```col```, and ```itis```. Note that offline-enabled matchers will download versioned taxonomic resources initially (e.g., an ITIS data dump as included in [Nomer's Corpus of Taxonomic Resources*](https://doi.org/10.5281/zenodo.12695629)), and re-uses the indexes until the cache is cleaned up. The cache itself can be archived so that results can be reproduced in a different environment without need to rebuild the term match index. For prebuilt indexes, please inspect the release assets at https://github.com/globalbioticinteractions/nomer/releases/0.5.15 (e.g., [Catalogue of Life: prebuilt index Nomer v0.5.15](https://github.com/globalbioticinteractions/nomer/releases/download/0.5.15/col_mapdb.zip)). Note, however, that indexes may take some time to build from scratch, and, when built, they can take hundreds of megabytes of disk space. 

Matchers can be added by writing some java code that implements an interface.

Note that a python wrapper was made available by [nleguillarme](https://github.com/nleguillarme) at [https://github.com/nleguillarme/pynomer](https://github.com/nleguillarme/pynomer). 

Note that a NodeJS wrapper was made available by [zedomel](https://github.com/zedomel) at [https://github.com/zedomel/nodejs-nomer](https://github.com/zedomel/nodejs-nomer). 

*Poelen, J. H. (ed . ) . (2024). Nomer Corpus of Taxonomic Resources hash://sha256/b60c0d25a16ae77b24305782017b1a270b79b5d1746f832650f2027ba536e276 hash://md5/17f1363a277ee0e4ecaf1b91c665e47e (0.27) [Data set]. Zenodo. https://doi.org/10.5281/zenodo.12695629

## Table of Contents

- [Prerequisites](#prerequisites)
- [Install](#install)
- [Documentation](docs/nomer.adoc)
- [Usage](#usage)
- [Examples](#examples)
  - [`show version`](#show-version)
  - [`show supported matchers`](#show-supported-matchers)
  - [`match by (taxon) id`](#match-term-by-id-with-default-matcher)
    - [`ITIS API`](#itis)
    - [`NCBI API`](#ncbi)
    - [`json output`](#match-term-by-id-with-json-output)
  - [`match by (taxon) name`](#match-term-by-name-with-default-matcher)
    - [`match by name using specific matcher`](#match-term-by-name-with-selected-matcher)
  - [`replacing term matches`](#replacing-term-matches)
  - [`validation`](#validate-taxoncache-and-taxonmap)
- [Building](#building)
- [Contribute](#contribute)
- [License](#license)

## Prerequisites

Nomer needs Java 8+, and is developed and tested on OpenJDK 8. [Why OpenJDK 8?](https://adoptopenjdk.net/support.html). Because OpenJDK 8 is Long Term Supported (LTS), and (at time of writing Nov 2021) supported until at least May 2026, longer than any of the newer OpenJDK versions.

Please see [https://github.com/nleguillarme/pynomer](https://github.com/nleguillarme/pynomer) for a python wrapper.

## Install

Nomer is a stand-alone java application, packaged in a jarfile. You can build you own (see [building](#building)) or download a prebuilt jar at [releases](https://github.com/globalbioticinteractions/nomer/releases).

On linux and mac, you can use the following script to install nomer:
```console
sudo sh -c '(curl -L https://github.com/globalbioticinteractions/nomer/releases/download/0.5.15/nomer.jar) > /usr/local/bin/nomer && chmod +x /usr/local/bin/nomer && nomer install-manpage' && nomer clean && nomer version
```
:warning: Please review the script before running it.

With this, you can now run things like ```nomer version``` instead of ```java -jar [some long dir path]/nomer.jar version``` . 

Note that a debian package (Debian, Ubuntu, etc) is also available for use with the [Advanced Package Tool (or apt)](https://en.wikipedia.org/wiki/APT_(software)) via:

```console
sudo apt update
sudo apt upgrade
curl -L https://github.com/globalbioticinteractions/nomer/releases/download/0.5.15/nomer.deb > nomer.deb
sudo apt install ./nomer.deb
```

To remove type ```sudo apt remove nomer```.


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

## Documentation

For documentation see [docs/nomer.adoc](docs/nomer.adoc) or type ```man nomer``` in the terminal after installing nomer.

## Usage
As generated using:

```bash
nomer help
```



```
Usage: nomer [-hV] [COMMAND]
maps identifiers and names to other identifiers and names
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  version                                      Show Version
  replace                                      Replace exact term matches in
                                                 row from stdin. The input
                                                 schema is used to select the
                                                 id and/or name to match to.
                                                 The output schema is used to
                                                 select the columns to write
                                                 into. If a term has multiple
                                                 matches, first match is used.
                                               For example:
                                               echo -e '\tHomo sapiens' | nomer
                                                 replace col
                                               has expected result:
                                               COL:6MB3T	Homo sapiens
  append                                       Append term match to row from
                                                 stdin using id and name
                                                 columns specified in input
                                                 schema. Multiple matches
                                                 result in multiple rows.
                                               For example:
                                               echo -e '\tHomo sapiens' | nomer
                                                 append col
                                               has expected result:
                                               	Homo sapiens	HAS_ACCEPTED_NAME	
                                                 COL:6MB3T	Homo sapiens	
                                                 Linnaeus, 1758	species		Biota
                                                 | Animalia | Chordata |
                                                 Vertebrata | Gnathostomata |
                                                 Osteichthyes | Sarcopterygii |
                                                 Tetrapoda | Amniota | Mammalia
                                                 | Theria | Eutheria | Primates
                                                 | Haplorrhini | Simiiformes |
                                                 Hominoidea | Hominidae |
                                                 Homininae | Homo | Homo
                                                 sapiens	COL:5T6MX | COL:N |
                                                 COL:CH2 | COL:8V4V3 | COL:
                                                 8V4V5 | COL:8VVWB | COL:8VSMX
                                                 | COL:9CK8W | COL:8VLBH | COL:
                                                 6224G | COL:924GT | COL:LG |
                                                 COL:8ZXYB | COL:4DT | COL:4PM
                                                 | COL:58L | COL:6256T | COL:
                                                 JPH | COL:636X2 | COL:6MB3T	
                                                 unranked | kingdom | phylum |
                                                 subphylum | infraphylum |
                                                 parvphylum | gigaclass |
                                                 megaclass | superclass | class
                                                 | subclass | infraclass |
                                                 order | suborder | infraorder
                                                 | superfamily | family |
                                                 subfamily | genus | species	|
                                                 |  |  |  |  |  |  |  |
                                                 Linnaeus, 1758 | Parker &
                                                 Haswell, 1897 | Gill, 1872 |
                                                 Linnaeus, 1758 | Pocock, 1918
                                                 | Haeckel, 1866 | Gray, 1825 |
                                                 Gray, 1825 | Gray, 1825 |
                                                 Linnaeus, 1758 | Linnaeus,
                                                 1758	https://www.
                                                 catalogueoflife.
                                                 org/data/taxon/6MB3T
  list, ls, dump, export                       Dumps all terms into the defined
                                                 output schema.
                                               For example:
                                               nomer ls col | head -n2
                                               has expected result:
                                               providedExternalId	providedName	
                                                 providedAuthorship	
                                                 relationName	
                                                 resolvedExternalId	
                                                 resolvedName	...
                                                 resolvedAuthorship	
                                                 resolvedRank	
                                                 resolvedCommonNames	
                                                 resolvedPath	resolvedPathIds	
                                                 resolvedPathNames	
                                                 resolvedPathAuthorships	
                                                 resolvedExternalUrlCOL:
                                                 001417c6-d3fc-4f42-aa3d-b1de3a5
                                                 92e58	Cheilostomatida incertae
                                                 sedis		HAS_ACCEPTED_NAME	COL:
                                                 001417c6-d3fc-4f42-aa3d-b1de3a5
                                                 92e58	Cheilostomatida incertae
                                                 sedis		suborder		Biota |
                                                 Animalia | Bryozoa |
                                                 Gymnolaemata | Cheilostomatida
                                                 | Cheilostomatida incertae
                                                 sedis	COL:5T6MX | COL:N | COL:
                                                 622CG | COL:8ZXG2 | COL:84JWL
                                                 | COL:
                                                 001417c6-d3fc-4f42-aa3d-b1de3a5
                                                 92e58	unranked | kingdom |
                                                 phylum | class | order |
                                                 suborder	|  |  | Allman, 1856
                                                 | Busk, 1852 |	https://www.
                                                 catalogueoflife.
                                                 org/data/taxon/001417c6-d3fc-4f
                                                 42-aa3d-b1de3a592e58
  matchers                                     Lists supported matcher and
                                                 (optionally) their
                                                 descriptions.
  properties                                   Lists configuration properties.
                                                 Can be used to make a local
                                                 copy and override default
                                                 settings using the
                                                 [--properties=[local copy]]
                                                 option.
  input-schema                                 Show input schema in JSON.
  output-schema                                Show output schema in JSON.
  validate-terms                               Validate terms.
  validate-term-link                           Validate term links.
  clean                                        Cleans term matcher cache.
  config-man, config-manpage, install-manpage  Installs/configures Nomer man
                                                 page, so you can type [man
                                                 nomer] on unix-like system to
                                                 learn more about Nomer.
  gen-manpage                                  Generates man pages for all
                                                 commands in the specified
                                                 directory.
  help                                         Displays help information about
                                                 the specified command
```

## Examples 

### Show version

```bash
nomer version
```

produces:

```
0.5.15
```

### Show supported matchers
```bash
nomer matchers -v
```
Result as of v0.5.15 (Feb 2025) is formatted as a table below:

| name | description |
| --- | --- |
| ala | Lookup taxon in Atlas of Living Australia by name or by id using ALATaxon:* prefix. |
| batnames | Lookup BatNames taxa by name, synonym using offline-enabled database dump |
| bold-web | Use BOLD webservice to lookup taxa by bin/taxon id using BOLD:* and BOLDTaxon:* prefixes. |
| col | Lookup Catalogue of Life taxon by name or COL:* prefixed ids using offline-enabled database dump |
| crossref-doi | uses api.crossref.org to resolve doi associated with human readable citation |
| discoverlife | Lookup DiscoverLife taxa by name, synonym using offline-enabled database dump |
| envo | Lookup envo terms by name or by id using ENVO:* prefix. |
| eol | Lookup EOL pages by id with EOL:* prefix using offline-enabled database dump |
| gbif | Lookup GBIF taxa by name, synonym or id using offline-enabled database dump |
| gbif-parse | Attempts extract canonical taxonomic name from name string using https://github.com/gbif/name-parser . |
| gbif-web | Web-based taxon id/name lookup using GBIF backbone API and GBIF:* prefix. |
| globalnames | Uses https://resolver.globalnames.org to match taxon names. Searches by name only (not id). |
| globi | Uses GloBI's Taxon Graph to lookup terms by id or name across many taxonomies / ontologies. Caches a copy locally on first use to allow for subsequent offline usage. Use properties [nomer.term.cache.url] and [nomer.term.map.url] to override default cache and map locations. See https://doi.org/10.5281/zenodo.755513 for more information. |
| globi-correct | Scrubs names using GloBI's (taxonomic) name scrubber. Scrubbing includes removing of stopwords (e.g., undefined), correcting common typos using a "crappy" names list, parse to canonical name using gnparser (see https://github.com/GlobalNamesArchitecture/gnparser), and more. |
| globi-enrich | Uses GloBI's taxon enricher to find first term match by id or name. Uses various web apis like Encyclopedia of Life, World Registry of Marine Species (WoRMS), Integrated Taxonomic Information System (ITIS), National Biodiversity Network (NBN) and more. |
| globi-rank | Finds taxonomic rank identifiers by rank commons name (e.g., species, order, soort). Uses Wikidata taxon rank items. Caches a copy locally on first usage to allow for subsequent offline usage. |
| globi-suggest | Scrubs names using GloBI's (taxonomic) name scrubber. Scrubbing includes removing of stopwords (e.g., undefined), correcting common typos using a "crappy" names list, parse to canonical name using gnparser (see https://github.com/GlobalNamesArchitecture/gnparser), and more. |
| gn-parse | Attempts extract canonical taxonomic name from name string using https://github.com/GlobalNamesArchitecture/gnparser . |
| gulfbase | Look up taxa of https://gulfbase.org by name or id with BioGoMx:* prefix. |
| inaturalist-id | Lookup taxon in iNaturalist by id with INAT_TAXON:* prefix. |
| indexfungorum | Lookup Index Fungorum taxon by name or id using offline-enabled database dump |
| itis | Lookup ITIS taxon by name or id using offline-enabled database dump |
| itis-web | Use itis webservice to lookup taxa by id using ITIS:* prefix. |
| mdd | Lookup Mammal Diversity Database (MDD) taxon by name or id using offline-enabled database dump |
| nbn | Lookup taxon of National Biodiversity Network by id with NBN:* prefix. |
| ncbi | Lookup NCBI taxa by name, synonym or id using offline-enabled database dump |
| ncbi-web | Lookup NCBI taxon by id with NCBI:* prefix using web apis. |
| nodc | Lookup taxon in the Taxonomic Code of the National Oceanographic Data Center (NODC) by id with prefix NODC: . Maps to ITIS terms if possible. |
| openbiodiv | uses openbiodiv sparql endpoint to resolve openbiodiv terms |
| orcid-web | Lookup ORCID by id with ORCID:* prefix. |
| ott | Lookup Open Tree of Life taxon by name or (OTT\|GBIF\|WORMS\|IF\|NCBI\|IRMNG)* prefixed ids using offline-enabled database dump |
| pbdb | Lookup Paleobio Database taxon by name or id using offline-enabled database dump |
| plazi | Lookup Plazi taxon treatment by name or id using offline-enabled database dump |
| pmid-doi | resolves pubmed id to doi using https://www.ncbi.nlm.nih.gov/pmc/pmctopmid/ |
| remove-stop-words | Removes stop words (e.g., undefined) using a stop word list specified by property [nomer.taxon.name.stopword.url] . |
| tpt | Lookup TPT taxon by name or id using offline-enabled database dump |
| translate-names | Translates incoming names using a two column csv file specified by property [nomer.taxon.name.correction.url] . |
| uksi-current-name | Use UK Species Inventory to find current taxonomic name. |
| wfo | Lookup World of Flora Online taxon by name or WFO:* prefixed ids using offline-enabled database dump |
| wikidata | Lookup Wikidata taxon by name or id using offline-enabled database dump |
| wikidata-web | uses wikidata to cross-walk taxon id across taxonomies |
| worms | Lookup World Register of Marine Species by name or WORMS:* prefixed ids using offline-enabled database dump |
| worms-web | Lookup taxon in WoRMS by name or by id with WORMS:* prefix. |




If you'd like to add new matchers, please open [a new issue](https://github.com/globalbioticinteractions/nomer/issues/new) and describe your desires.

### Match term by id

```bash
echo -e "NCBI:9606\t"\
 | nomer append ncbi-web\
 > matches.tsv
```

### Match term by name

```bash
echo -e "\tHomo sapiens"\
 | nomer append ncbi-web\
 > matches.tsv
```

matches.tsv should now include entries like

```bash
$ cat matches.tsv
NCBI:9606		SAME_AS	NCBI:9606	Homo sapiens		species	human @en	cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens	NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606	| superkingdom | clade | kingdom | clade | clade | clade | phylum | subphylum | clade | clade | clade | clade | superclass | clade | clade | clade | class | clade | clade | clade | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | species		https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606
```

### Match term by id with JSON output
Similarly, you can match terms by id and produce JSON output, instead of tab-separated values using:

```bash
echo -e "NCBI:9606\tHomo sapiens"\
 | nomer append ncbi-web -o json\
 > matches.json
```

Now matches.json looks something like:

```json
{
  "species": {
    "@id": "NCBITaxon:9606",
    "name": "Homo sapiens",
    "equivalent_to": {
      "@id": "NCBITaxon:9606",
      "name": "Homo sapiens"
    }
  },
  "norank": {
    "@id": "NCBITaxon:131567",
    "name": "cellular organisms"
  },
  "superkingdom": {
    "@id": "NCBITaxon:2759",
    "name": "Eukaryota"
  },
  "clade": {
    "@id": "NCBITaxon:33154",
    "name": "Opisthokonta"
  },
  "kingdom": {
    "@id": "NCBITaxon:33208",
    "name": "Metazoa"
  },
  "phylum": {
    "@id": "NCBITaxon:7711",
    "name": "Chordata"
  },
  "subphylum": {
    "@id": "NCBITaxon:89593",
    "name": "Craniata"
  },
  "superclass": {
    "@id": "NCBITaxon:8287",
    "name": "Sarcopterygii"
  },
  "class": {
    "@id": "NCBITaxon:40674",
    "name": "Mammalia"
  },
  "superorder": {
    "@id": "NCBITaxon:314146",
    "name": "Euarchontoglires"
  },
  "order": {
    "@id": "NCBITaxon:9443",
    "name": "Primates"
  },
  "suborder": {
    "@id": "NCBITaxon:376913",
    "name": "Haplorrhini"
  },
  "infraorder": {
    "@id": "NCBITaxon:314293",
    "name": "Simiiformes"
  },
  "parvorder": {
    "@id": "NCBITaxon:9526",
    "name": "Catarrhini"
  },
  "superfamily": {
    "@id": "NCBITaxon:314295",
    "name": "Hominoidea"
  },
  "family": {
    "@id": "NCBITaxon:9604",
    "name": "Hominidae"
  },
  "subfamily": {
    "@id": "NCBITaxon:207598",
    "name": "Homininae"
  },
  "genus": {
    "@id": "NCBITaxon:9605",
    "name": "Homo"
  },
  "path": {
    "names": [
      "cellular organisms",
      "Eukaryota",
      "Opisthokonta",
      "Metazoa",
      "Eumetazoa",
      "Bilateria",
      "Deuterostomia",
      "Chordata",
      "Craniata",
      "Vertebrata",
      "Gnathostomata",
      "Teleostomi",
      "Euteleostomi",
      "Sarcopterygii",
      "Dipnotetrapodomorpha",
      "Tetrapoda",
      "Amniota",
      "Mammalia",
      "Theria",
      "Eutheria",
      "Boreoeutheria",
      "Euarchontoglires",
      "Primates",
      "Haplorrhini",
      "Simiiformes",
      "Catarrhini",
      "Hominoidea",
      "Hominidae",
      "Homininae",
      "Homo",
      "Homo sapiens"
    ],
    "ids": [
      "NCBI:131567",
      "NCBI:2759",
      "NCBI:33154",
      "NCBI:33208",
      "NCBI:6072",
      "NCBI:33213",
      "NCBI:33511",
      "NCBI:7711",
      "NCBI:89593",
      "NCBI:7742",
      "NCBI:7776",
      "NCBI:117570",
      "NCBI:117571",
      "NCBI:8287",
      "NCBI:1338369",
      "NCBI:32523",
      "NCBI:32524",
      "NCBI:40674",
      "NCBI:32525",
      "NCBI:9347",
      "NCBI:1437010",
      "NCBI:314146",
      "NCBI:9443",
      "NCBI:376913",
      "NCBI:314293",
      "NCBI:9526",
      "NCBI:314295",
      "NCBI:9604",
      "NCBI:207598",
      "NCBI:9605",
      "NCBI:9606"
    ],
    "ranks": [
      "",
      "superkingdom",
      "clade",
      "kingdom",
      "clade",
      "clade",
      "clade",
      "phylum",
      "subphylum",
      "clade",
      "clade",
      "clade",
      "clade",
      "superclass",
      "clade",
      "clade",
      "clade",
      "class",
      "clade",
      "clade",
      "clade",
      "superorder",
      "order",
      "suborder",
      "infraorder",
      "parvorder",
      "superfamily",
      "family",
      "subfamily",
      "genus",
      "species"
    ]
  }
}
```

Using tools like [jq](https://stedolan.github.io/jq/), you can now do things like:

```
echo -e "NCBI:9606\tHomo sapiens"\
 | nomer append -o json ncbi-web\
 | jq .family
```
to list all the family taxa associated with the term.


### Match term by id with selected matcher

### ITIS

``` console
$ echo -e "ITIS:180547" | nomer append itis
ITIS:180547 SAME_AS ITIS:180547 Enhydra lutris  Species     Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Carnivora | Caniformia | Mustelidae | Lutrinae | Enhydra | Enhydra lutris ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180539 | ITIS:552303 | ITIS:180545 | ITIS:552326 | ITIS:180546 | ITIS:180547   Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Order | Suborder | Family | Subfamily | Genus | Species   http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=180547
```

### NCBI

``` console
$ echo -e "NCBI:9606" | nomer append ncbi```
NCBI:9606	SAME_AS	NCBI:9606	Homo sapiens	species	man @en | human @en	cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens	NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606	| superkingdom |  | kingdom |  |  |  | phylum | subphylum |  |  |  |  |  |  |  |  | class |  |  |  | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | specieshttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606
```
### Match term by name with selected matcher

``` console
$ echo -e "\tCanis lupus" | nomer append globalnames
	Canis lupus	SAME_AS	NCBI:9612	Canis lupus	species		| Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Laurasiatheria | Carnivora | Caniformia | Canidae | Canis | Canis lupus	NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314145 | NCBI:33554 | NCBI:379584 | NCBI:9608 | NCBI:9611 | NCBI:9612	| superkingdom |  | kingdom |  |  |  | phylum | subphylum |  |  |  |  |  |  |  |  | class |  |  |  | superorder | order | suborder | family | genus | species	https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9612
	Canis lupus	SAME_AS	OTT:247341	Canis lupus	species		|  | Eukaryota | Opisthokonta | Holozoa | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Laurasiatheria | Carnivora | Caniformia | Canidae | Canis | Canis lupus	OTT:805080 | OTT:93302 | OTT:304358 | OTT:332573 | OTT:5246131 | OTT:691846 | OTT:641038 | OTT:117569 | OTT:147604 | OTT:125642 | OTT:947318 | OTT:801601 | OTT:278114 | OTT:114656 | OTT:114654 | OTT:458402 | OTT:4940726 | OTT:229562 | OTT:229560 | OTT:244265 | OTT:229558 | OTT:683263 | OTT:5334778 | OTT:392223 | OTT:44565 | OTT:827263 | OTT:770319 | OTT:372706 | OTT:247341	no rank | no rank | domain | no rank | no rank | kingdom | no rank | no rank | no rank | phylum | subphylum | subphylum | superclass | no rank | no rank | class | no rank | superclass | no rank | class | subclass | no rank | no rank | superorder | order | suborder | family | genus | species	https://tree.opentreeoflife.org/opentree/ottol@247341
	Canis lupus	SAME_AS	INAT_TAXON:42048	Canis lupus	speciesAnimalia | Chordata | Mammalia | Carnivora | Canidae | Canis | Canis lupus	kingdom | phylum | class | order | family | genus | species	http://inaturalist.org/taxa/42048
	Canis lupus	SAME_AS	ITIS:180596	Canis lupus	Species		Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Carnivora | Caniformia | Canidae | Canis | Canis lupus	ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180539 | ITIS:552303 | ITIS:180594 | ITIS:180595 | ITIS:180596	Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Order | Suborder | Family | Genus | Species	http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=180596
	Canis lupus	SAME_AS	IRMNG:11407661	Canis lupus	species		Animalia | Chordata | Mammalia | Carnivora | Canidae | Canis | Canis lupus	IRMNG:11 | IRMNG:148 | IRMNG:1310 | IRMNG:12116 | IRMNG:104585 | IRMNG:1282727 | IRMNG:11407661	kingdom | phylum | class | order | family | genus | species	http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=11407661
	Canis lupus	SAME_AS	GBIF:5219173	Canis lupus	species		Animalia | Chordata | Mammalia | Carnivora | Canidae | Canis | Canis lupus	GBIF:1 | GBIF:44 | GBIF:359 | GBIF:732 | GBIF:9701 | GBIF:5219142 | GBIF:5219173kingdom | phylum | class | order | family | genus | species	http://www.gbif.org/species/5219173
```

The expected output includes tab separated lines like, where the first two columns are the input and the following columns are match results.


### Replacing term matches

In addition to appending the found matches to a provided input row, Nomer also supports replacing the matched values.

Looking up _Canis lupus_ using globalnames with the replace command would look like:

```bash
echo -e "\tCanis lupus"\
 | nomer replace globi-globalnames
```

which produces:

```
NCBI:9612	Canis lupus
```

If multiple matches for the term are available, the first match will be replaced.

The replace commands also supports pipe delimited paths, like:

``` console
$ echo -e "\tAnimalia | Mammalia | Canis lupus" | nomer replace globi-globalnames
ITIS:202423 | NCBI:40674 | NCBI:9612	Animalia | Mammalia | Canis lupus
```

Or when using a matcher that supports lookup by id:
```bash
echo -e "ITIS:202423 | NCBI:40674 | NCBI:9612\t"\
 | nomer replace globi-enrich
```

would produce:

```
ITIS:202423 | NCBI:40674 | NCBI:9612    Animalia | Mammalia | Canis lupus
```

If you have an existing tabular file where the id and name columns are not the first and second respectively, then, you can change the input/output schema. For instance, if you'd like to match on ids in the third (=2) column and write the matching id and name in the first (=0) and second (=1) column (= default), you can do something like:

```bash
echo -e "\t\tNCBI:9606"\
 | nomer replace --properties <(echo 'nomer.schema.input=[{\"column\":2,\"type\":\"externalId\"}]') ncbi-web
```

which would produce:

```
NCBI:9606	Homo sapiens	NCBI:9606
```

To avoid escaping of double quotes (i.e. ```"``` -> ```\"```), and to keep your commands relatively short, perhaps an easier way to change the input / output schema is the save the default properties to a file using ```nomer properties > my.properties```.
Now, edit the properties ```nomer.schema.input``` and ```nomer.schema.output``` to suit your needs. After you are done, you can use the properties by running someting like:

``` console
$ echo -e "\t\tNCBI:9606" | nomer replace --properties=my.properties ncbi-web
NCBI:9606	Homo sapiens	NCBI:9606
```
... to reproduce the results from the previous example.


### validate taxonCache and taxonMap

 To validate terms (aka TaxonCache) and term linkages (aka TaxonMap) to be used with the offline term matchers, you can use the ```validate-term``` and ```validate-term-link``` commands. 

For instance, if you'd like to validate the first 10 lines of the taxonCache as published in https://zenodo.org/record/1213465 do:

```curl -L "https://zenodo.org/record/1213465/files/taxonCacheFirst10.tsv" | nomer validate-term```

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

## References 

### 1
José Augusto Salim, & Jorrit Poelen. (2025). globalbioticinteractions/nomer: 0.5.15 (0.5.15). Zenodo. https://doi.org/10.5281/zenodo.14893840

### 2 
Jorrit H. Poelen, James D. Simons and Chris J. Mungall. (2014). Global Biotic Interactions: An open infrastructure to share and analyze species-interaction datasets. Ecological Informatics. https://doi.org/10.1016/j.ecoinf.2014.08.005.

### 3
Poelen, J. H. (ed . ) . (2024). Nomer Corpus of Taxonomic Resources hash://sha256/b60c0d25a16ae77b24305782017b1a270b79b5d1746f832650f2027ba536e276 hash://md5/17f1363a277ee0e4ecaf1b91c665e47e (0.27) [Data set]. Zenodo. https://doi.org/10.5281/zenodo.12695629
