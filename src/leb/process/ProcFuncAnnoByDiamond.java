package leb.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import leb.util.seq.Blast6FormatHitDomain;
import leb.util.common.Prompt;
import leb.util.config.GenericConfig;
import leb.wrapper.DiamondWrapper;


public class ProcFuncAnnoByDiamond {
	
	private String outDir = "/tmp";
	
	private String diamondPath = "diamond";
	
	public static final int BLASTX = 0;
	public static final int BLASTP = 1;
	
	private int method = 0;
	
	private int noHits = 1;
	private double evalue = 1e-5;
	private double score = 0;
	
	private double identity = 0;
	private double qcov = 0;
	
	private boolean sensitive = false;
	
	public ProcFuncAnnoByDiamond(int method){
		this.method = method;
	}
	
	public String getOutDir() {
		return outDir;
	}

	public void setOutDir(String outDir) {
		this.outDir = outDir;
	}

	public String getDiamondPath() {
		return diamondPath;
	}

	public void setDiamondPath(String diamondPath) {
		this.diamondPath = diamondPath;
	}

	public int getNoHits() {
		return noHits;
	}

	public void setNoHits(int noHits) {
		this.noHits = noHits;
	}

	public double getEvalue() {
		return evalue;
	}

	public void setEvalue(double evalue) {
		this.evalue = evalue;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getIdentity() {
		return identity;
	}

	public void setIdentity(double identity) {
		this.identity = identity;
	}
	
	public double getQcov() {
		return qcov;
	}
	
	public void setQcov(double qcov) {
		this.qcov = qcov;
	}

	public boolean isSensitive() {
		return sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}
	
	public void executeMakeDB(String inFileName, String dbFileName, int threads) throws IOException{
		File dir = new File(outDir);
		if(dir.exists() == false){
			dir.mkdir();
		}
		if(GenericConfig.VERB) Prompt.print("Execution of Diamond makedb will be started.");

		DiamondWrapper diamond = new DiamondWrapper(diamondPath, DiamondWrapper.MAKEDB);
		diamond.setThreads(threads);
		diamond.setInputFastaForDb(inFileName);
		diamond.setDbFile(dbFileName);
		diamond.run();

	}
	
	public List<Blast6FormatHitDomain> execute(String queryFileName, String dbFileName, int threads) throws IOException{
		
		File dir = new File(outDir);
		if(dir.exists() == false){
			dir.mkdir();
		}

		if(GenericConfig.VERB) Prompt.print("Execution of Diamond makedb will be started.");
		DiamondWrapper diamond = new DiamondWrapper(diamondPath, method);//blastx option
		
		diamond.setDbFile(dbFileName);
		diamond.setQueryFileName(queryFileName);
		
		diamond.setNoHits(noHits);
	
		diamond.setIdentity(identity); 
		diamond.setQueryCover(qcov);
		
		
		if(score !=  0){
			diamond.setScore(score);
		}else{
			diamond.setEvalue(evalue);
		}
		
		if(sensitive == true){
			diamond.setSensitive();
		}
		
		diamond.setThreads(threads);
		
		String outFileName = outDir + File.separator + "DiamondAnno.txt";
		diamond.setOutFileName(outFileName);
		diamond.run();
		
		if(GenericConfig.VERB) Prompt.print("Execution of diamond finished.");
		List<Blast6FormatHitDomain> diamondList = parseOutFile(outFileName);
		
		//f.deleteOnExit();
		
		return diamondList;
	}//method end
	
	public List<Blast6FormatHitDomain> parseOutFile(String outFileName) throws IOException{
		
		BufferedReader br = new BufferedReader(new FileReader(new File(outFileName)));
		
		List<Blast6FormatHitDomain> diamondList = new ArrayList<Blast6FormatHitDomain>();
		
		String line = null;
		while((line = br.readLine()) != null){
			
			String[] sline = line.split("\t");
			
			Blast6FormatHitDomain diamond = new Blast6FormatHitDomain();
			
			diamond.setQuery(sline[0]);
			diamond.setTarget(sline[1]);//  EXAMPLE: hsa:00001|K00001|K00002 
			
			diamond.setIdentity(Double.parseDouble(sline[2]));
			diamond.setAlignmentLength(Integer.valueOf(sline[3]));
			
			diamond.setMismatch(Integer.valueOf(sline[4]));
			diamond.setGap(Integer.valueOf(sline[5]));
			
			diamond.setStartInQuery(Integer.valueOf(sline[6]));
			diamond.setEndInQuery(Integer.valueOf(sline[7]));
	
			diamond.setStartInTarget(Integer.valueOf(sline[8]));
			diamond.setEndInTarget(Integer.valueOf(sline[9]));
			
			diamond.setEvalue(Double.parseDouble(sline[10]));
			diamond.setBitScore(Double.parseDouble(sline[11]));
			
			diamondList.add(diamond);
			
		}//while end
		
		br.close();
		
		return diamondList;
		
	}//method end
	
}
