package leb.process;

import java.io.File;

import leb.wrapper.ProdigalWrapper;

public class ProcCDSPredictionByProdigal {

	private final String outDir = "/tmp";
	
	private String prodigalPath = "prodigal";
	
    boolean meta = true;//metagenome option or single option
    //"metagenomic" means in this case is that Prodigal uses 
    //the best of 30 pre-generated training files rather than doing any training of its own.

	private String gffOutFileName = outDir + File.separator + "prodigal.gff";
    
    private String faaOutFileName = outDir + File.separator + "prodigal.faa";
    
    private String ffnOutFileName = outDir + File.separator + "prodigal.ffn";

	public void setProdigalPath(String prodigalPath) {
		this.prodigalPath = prodigalPath;
	}

	public void setGffOutFileName(String gffOutFileName) {
		this.gffOutFileName = gffOutFileName;
	}

	public void setFaaOutFileName(String faaOutFileName) {
		this.faaOutFileName = faaOutFileName;
	}

	public void setFfnOutFileName(String ffnOutFileName) {
		this.ffnOutFileName = ffnOutFileName;
	}

	public void execute(String seqFileName, boolean ignoredVerbose) {
		
		File dir = new File(outDir);
		if(!dir.exists()) dir.mkdir();

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
		
		if(meta) prodigal.setMetagenome();
		boolean allowPartialGene = true;
		if(!allowPartialGene) prodigal.setClosedEnds();
		int transl_table = 11;
		prodigal.setTranslationTable(transl_table);
		prodigal.run();
		
	}//method end
}
