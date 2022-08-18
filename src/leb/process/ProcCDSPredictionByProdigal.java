package leb.process;

import java.io.File;
import java.io.IOException;

import leb.wrapper.ProdigalWrapper;

public class ProcCDSPredictionByProdigal {

	private String outDir = "/tmp";
	
	private String prodigalPath = "prodigal";
	
    boolean meta = true;//metagenome option or single option
    //"metagenomic" means in this case is that Prodigal uses 
    //the best of 30 pre-generated training files rather than doing any training of its own.

    private boolean allowPartialGene = true;
    
    private int transl_table = 11;
    
    private String gffOutFileName = outDir + File.separator + "prodigal.gff";
    
    private String faaOutFileName = outDir + File.separator + "prodigal.faa";
    
    private String ffnOutFileName = outDir + File.separator + "prodigal.ffn";
    
	public String getOutDir() {
		return outDir;
	}

	public void setOutDir(String outDir) {
		this.outDir = outDir;
	}

	public String getProdigalPath() {
		return prodigalPath;
	}

	public void setProdigalPath(String prodigalPath) {
		this.prodigalPath = prodigalPath;
	}

	public boolean isMeta() {
		return meta;
	}

	public void setMeta(boolean meta) {
		this.meta = meta;
	}

	public boolean isAllowPartialGene() {
		return allowPartialGene;
	}

	public void setAllowPartialGene(boolean allowPartialGene) {
		this.allowPartialGene = allowPartialGene;
	}

	public int getTransl_table() {
		return transl_table;
	}

	public void setTransl_table(int transl_table) {
		this.transl_table = transl_table;
	}

	public String getGffOutFileName() {
		return gffOutFileName;
	}

	public void setGffOutFileName(String gffOutFileName) {
		this.gffOutFileName = gffOutFileName;
	}

	public String getFaaOutFileName() {
		return faaOutFileName;
	}

	public void setFaaOutFileName(String faaOutFileName) {
		this.faaOutFileName = faaOutFileName;
	}

	public String getFfnOutFileName() {
		return ffnOutFileName;
	}

	public void setFfnOutFileName(String ffnOutFileName) {
		this.ffnOutFileName = ffnOutFileName;
	}
	
	public void execute(String seqFileName) throws IOException{
		execute(seqFileName, true);
	}
	public void execute(String seqFileName, boolean verbose) throws IOException{
		
		File dir = new File(outDir);
		if(dir.exists() == false){
			dir.mkdir();
		}

//	    gffOutFileName = outDir + File.separator + "prodigal.gff";
//	    faaOutFileName = outDir + File.separator + "prodigal.faa";	    
//	    ffnOutFileName = outDir + File.separator + "prodigal.ffn";
		
		//System.out.println("Execution of Prodigal will be started");
		ProdigalWrapper prodigal = new ProdigalWrapper(prodigalPath);
		
		prodigal.setInputFileName(seqFileName);
		prodigal.setOutputFormat("gff");
		
		File prodigal_gff = new File(gffOutFileName);
		File prodigal_faa = new File(faaOutFileName);
		File prodigal_ffn = new File(ffnOutFileName);
		
		prodigal.setOutputFileName(prodigal_gff.getAbsolutePath());
		prodigal.setProteinFileName(prodigal_faa.getAbsolutePath());
		prodigal.setNuclFileName(prodigal_ffn.getAbsolutePath());
		prodigal.setQuietRun();//recommend, for faster running
		
		if(meta == true){
			prodigal.setMetagenome();
		}
		if(allowPartialGene == false){
			prodigal.setClosedEnds();
		}
		prodigal.setTranslationTable(transl_table);
		prodigal.run();
		
	}//method end
}
