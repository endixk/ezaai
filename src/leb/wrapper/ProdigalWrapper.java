package leb.wrapper;

import leb.util.common.ExecHandler;

public class ProdigalWrapper extends ExecHandler {
    public ProdigalWrapper(String prodigalPath) {
        super.init(prodigalPath);
    }

    
    public void setInputFileName(String inputFileName) {
        // -i
        addArgument("-i", inputFileName);
    }
    public void setOutputFileName(String outputFileName) {
        // -o
        addArgument("-o", outputFileName);
    }

    public void setOutputFormat(String format) {
        addArgument("-f",format);
    }
    
    public void setClosedEnds(){
    	addArgument("-c");
    }
    
    public void setProteinFileName(String proteinFileName) {
        addArgument("-a",proteinFileName);
    }
    public void setNuclFileName(String nuclFileName) {
        addArgument("-d",nuclFileName);
    }
    public void setMetagenome(){
    	//metagenome option or single option
        //"metagenomic" means in this case is that Prodigal uses the best of 30 pre-generated training files rather than doing any training of its own.
    	addArgument("-p", "meta");
    }
    
    public void setTranslationTable(int table) {//default = 11
    	//Genetic codes are listed in this link: http://www.ncbi.nlm.nih.gov/Taxonomy/Utils/wprintgc.cgi
    	addArgument("-g", table);
    }
    public void setQuietRun(){
    	addArgument("-q");
    }
    
    public void run() {
    	super.exec();
    }
}
