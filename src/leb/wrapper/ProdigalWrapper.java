package leb.wrapper;

import leb.util.common.ExecHandler;

public class ProdigalWrapper extends ExecHandler {
	private String inputFileName = null; // -i
    private String outputFileName = null; // -o
    
    public ProdigalWrapper(String prodigalPath) {
        super.init(prodigalPath);
    }
    
    public String getInputFileName() {
        return inputFileName;
    }
    public String getOutputFileName() {
        return outputFileName;
    }

    
    public void setInputFileName(String inputFileName) {
        this.inputFileName = inputFileName;
        addArgument("-i",this.inputFileName);
    }
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
        addArgument("-o",this.outputFileName);
    }
    
    public void setMode(String mode) {
        addArgument("-p",mode);
    }
    public void setOutputFormat(String format) {
        addArgument("-f",format);
    }
    
    public void setClosedEnds(){
    	addArgument("-c");
    }
    public void setSkipMaskedSequence(){
        addArgument("-m");
    }
    
    public void setProteinFileName(String proteinFileName) {
        addArgument("-a",proteinFileName);
    }
    public void setNuclFileName(String nuclFileName) {
        addArgument("-d",nuclFileName);
    }
    
    public void setStartFileName(String startFileName) {
        addArgument("-s",startFileName);
    }
    public void setTrainingFileName(String trainingFileName) {
        addArgument("-t",trainingFileName);
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
    public void setForceFullMotifScan(){
    	addArgument("-n");
    }
    
    public void setQuietRun(){
    	addArgument("-q");
    }
    
    public void run() {
    	super.exec();
    }
}
