# EzAAI: High Throughput Prokaryotic AAI Calculator 

EzAAI is a suite of workflows for improved AAI calculation performance along with the novel module that provides hierarchical clustering analysis and dendrogram representation.

 * [Homepage](http://leb.snu.ac.kr/ezaai)
 * [Publication](https://doi.org/10.1007/s12275-021-1154-0)


## Quick start with conda

~~~bash
conda create -n ezaai -c bioconda ezaai
conda activate ezaai
EzAAI -h
~~~

## Available modules
### `extract`
 * Extract protein database from genome using Prodigal

~~~bash
EzAAI extract -i <IN_SEQ> -o <OUT_DB>
~~~

|Argument|Description|
|:-:|-----------------|
|`-i`|Input prokaryotic genome sequence|
|`-o`|Output database|

---

### `calculate`	
 * Calculate AAI value from protein databases using MMseqs2

~~~bash
EzAAI calculate -i <INPUT_1> -j <INPUT_2> -o \<OUTPUT>
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
EzAAI convert -i <IN_CDS> -s <SEQ_TYPE> -o <OUT_DB>
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
EzAAI cluster -i <AAI_TABLE> -o \<OUTPUT>
~~~

|Argument|Description|
|:-:|-----------------|
|`-i`|Input EzAAI result file containing all-by-all pairwise AAI values|
|`-o`|Output result file|
