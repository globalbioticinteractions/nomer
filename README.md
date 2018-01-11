# nomer
[![Build Status](https://travis-ci.org/globalbioticinteractions/nomer.svg?branch=master)](https://travis-ci.org/globalbioticinteractions/nomer) [![standard-readme compliant](https://img.shields.io/badge/standard--readme-OK-green.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
[![DOI](https://zenodo.org/badge/117019305.svg)](https://zenodo.org/badge/latestdoi/117019305)

Maps identifiers and names to taxonomic names and ontological terms.

Used by [GloBI](https://globalbioticinteraction.org).

<a href="http://globalbioticinteractions.org/">
  <img src="http://www.globalbioticinteractions.org/assets/globi.svg" height="120">
</a>

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

* Clone this repository
* Run `mvn package`
* Copy `target/nomer-0.0.1-jar-with-dependencies.jar`
* Run tests using `mvn test`.

## Usage

```
Usage: <main class> [command] [command options]
  Commands:
    version      Show Version
      Usage: version
```

## Examples 

Show nomer version

```java -jar nomer.jar version```

## Contribute

Feel free to join in. All welcome. Open an [issue](https://github.com/globalbioticinteractions/nomer/issues)!

## License

[GPL](LICENSE)
