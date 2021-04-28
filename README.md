# EzAAI: A Pipeline for High Throughput Calculation of Prokaryotic Average Amino Acid Identity
## Introduction
EzAAI is a suite of workflows for improved AAI calculation performance along with the novel module that provides hierarchical clustering analysis and dendrogram representation.

The user manual and tutorial are available in [http://leb.snu.ac.kr/ezaai](http://leb.snu.ac.kr/ezaai).

## Publication
[Kim, D., Park, S. & Chun, J. Introducing EzAAI: a pipeline for high throughput calculations of prokaryotic average amino acid identity. J Microbiol. 59, 476–480 (2021). https://doi.org/10.1007/s12275-021-1154-0](https://doi.org/10.1007/s12275-021-1154-0)

## Requirements
### Prodigal 

([Hyatt D, Chen G-L, LoCascio PF, Land ML, Larimer FW et al. Prodigal: prokaryotic gene recognition and translation initiation site identification. BMC bioinformatics 2010;11(1):119 doi: 10.1186/1471-2105-11-119](https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-119))

### MMseqs2 

([Steinegger M, Söding J. MMseqs2 enables sensitive protein sequence searching for the analysis of massive data sets. Nature biotechnology 2017;35(11):1026-1028 https://doi.org/10.1038/nbt.3988](https://www.nature.com/articles/nbt.3988))

## Available modules
## extract	
Extract profile DB from genome using Prodigal

#### USAGE: java -jar EzAAI.jar extract -i <IN_SEQ> -o <OUT_DB>

Argument
&nbsp;&nbsp;
Description

-i
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Input bacterial genome sequence

-o
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Output profile database

## convert	
Convert CDS FASTA file into profile DB

#### USAGE: java -jar EzAAI.jar convert -i <IN_CDS> -s <SEQ_TYPE> -o <OUT_DB>

Argument
&nbsp;&nbsp;
Description

-i
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Input CDS profile (FASTA format)

-s
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Sequence type of input file (nucl/prot)

-o
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Output profile DB

## calculate	
Calculate AAI value from profile databases using MMseqs2

####  USAGE: java -jar EzAAI.jar calculate -i <INPUT_1> -j <INPUT_2> -o \<OUTPUT>

Argument
&nbsp;&nbsp;
Description

-i
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
First input profile DB / directory with profile DBs

-j
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Second input profile DB / directory with profile DBs

-o
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Output result file

## cluster	
Hierarchical clustering of taxa with AAI values

####  USAGE: java -jar EzAAI.jar cluster -i <AAI_TABLE> -o \<OUTPUT>
EzAAI is a suite of workflows for improved AAI calculation performance along with the novel module that provides hierarchical clustering analysis and dendrogram representation.

Argument
&nbsp;&nbsp;
Description

-i
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Input EzAAI result file containing all-by-all pairwise AAI values

-o
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Output result file
