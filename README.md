# EzAAI: A Pipeline for High Throughput Calculation of Prokaryotic Average Amino Acid Identity
## Introduction
EzAAI is a suite of workflows for improved AAI calculation performance along with the novel module that provides hierarchical clustering analysis and dendrogram representation.

 * [Homepage](http://leb.snu.ac.kr/ezaai)
 * [Publication](https://doi.org/10.1007/s12275-021-1154-0)


## Quick start with conda
~~~bash
conda create -n ezaai -c bioconda -c conda-forge prodigal mmseqs2 openjdk=8
conda activate ezaai
wget -O EzAAI.jar http://leb.snu.ac.kr/ezaai/download/jar
java -jar EzAAI.jar
~~~

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
