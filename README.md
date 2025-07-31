# EzAAI: High Throughput Prokaryotic AAI Calculator 
[![Build](https://img.shields.io/github/actions/workflow/status/endixk/ezaai/maven-build.yml)](https://github.com/endixk/ezaai/actions)
[![License](https://img.shields.io/github/license/endixk/ezaai)](https://github.com/endixk/ezaai/blob/main/LICENSE.md)
[![Bioconda](https://img.shields.io/conda/dn/bioconda/ezaai?logo=anaconda)](https://anaconda.org/bioconda/ezaai) 

EzAAI is a suite of workflows for improved AAI calculation performance along with the novel module that provides hierarchical clustering analysis and dendrogram representation.

 * [Homepage](http://endixk.github.io/ezaai)
 * [Publication](https://doi.org/10.1007/s12275-021-1154-0)


## Quick start with conda

~~~bash
conda install -c bioconda ezaai
ezaai -h
~~~

### Build from source
#### Requirements
   * Java 8+
   * Maven 3+
~~~bash
git clone https://github.com/endixk/ezaai.git
cd ezaai
mvn clean compile assembly:single
java -jar target/EzAAI-*.jar -h
~~~

## Available modules
### `extract`
 * Extract protein database(s) from genome using Prodigal

~~~bash
ezaai extract -i <IN_SEQ> -o <OUT_DB>
~~~

|Argument|Description|
|:-:|-----------------|
|`-i`|Input file or directory with prokaryotic genome sequence(s)|
|`-o`|Output file or directory|

---

### `calculate`	
 * Calculate AAI value from protein databases using MMseqs2

~~~bash
ezaai calculate -i <INPUT_1> -j <INPUT_2> -o <OUTPUT>
~~~

|Argument|Description|
|:-:|-----------------|
|`-i`|First input DB / directory with DBs|
|`-j`|Second input DB / directory with DBs|
|`-o`|Output result file|

---

### `convert`	
 * Convert CDS FASTA file into MMseqs2 database

~~~bash
ezaai convert -i <IN_CDS> -s <SEQ_TYPE> -o <OUT_DB>
~~~

|Argument|Description|
|:-:|-----------------|
|`-i`|Input CDS profile (FASTA format)|
|`-s`|Sequence type of input file (nucl/prot)|
|`-o`|Output database|

---

### `cluster`
 * Hierarchical clustering of taxa with AAI values

~~~bash
ezaai cluster -i <AAI_TABLE> -o <OUTPUT>
~~~

|Argument|Description|
|:-:|-----------------|
|`-i`|Input EzAAI result file containing all-by-all pairwise AAI values|
|`-o`|Output result file|
