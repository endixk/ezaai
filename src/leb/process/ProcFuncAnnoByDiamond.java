package leb.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import leb.util.seq.Blast6FormatHitDomain;
import leb.wrapper.DiamondWrapper;


public class ProcFuncAnnoByDiamond {
	
	private String outDir = "/tmp/ezaai";
	
	private String diamondPath = "diamond";
	
	private final int method;

	private double identity = 0;
	private double qcov = 0;
	
	private boolean sensitive = false;
	
	public ProcFuncAnnoByDiamond(int method){
		this.method = method;
	}

	public void setOutDir(String outDir) {
		this.outDir = outDir;
	}

	public void setDiamondPath(String diamondPath) {
		this.diamondPath = diamondPath;
	}

	public void setIdentity(double identity) {
		this.identity = identity;
	}

	public void setQcov(double qcov) {
		this.qcov = qcov;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}
	
	public void executeMakeDB(String inFileName, String dbFileName, int threads) {
		File dir = new File(outDir);
		if(!dir.exists()) dir.mkdir();

		DiamondWrapper diamond = new DiamondWrapper(diamondPath, DiamondWrapper.MAKEDB);
		diamond.setThreads(threads);
		diamond.setInputFastaForDb(inFileName);
		diamond.setDbFile(dbFileName);
		diamond.run();

	}
	
	public List<Blast6FormatHitDomain> execute(String queryFileName, String dbFileName, int threads) throws IOException{
		
		File dir = new File(outDir);
		if(!dir.exists()) dir.mkdir();
		DiamondWrapper diamond = new DiamondWrapper(diamondPath, method);//blastx option
		
		diamond.setDbFile(dbFileName);
		diamond.setQueryFileName(queryFileName);

		int noHits = 1;
		diamond.setNoHits(noHits);
	
		diamond.setIdentity(identity); 
		diamond.setQueryCover(qcov);
		diamond.setEvalue(1e-5);
		
		if(sensitive) diamond.setSensitive();
		
		diamond.setThreads(threads);
		
		String outFileName = outDir + File.separator + "DiamondAnno.txt";
		diamond.setOutFileName(outFileName);
		diamond.run();

		return parseOutFile(outFileName);
	}//method end
	
	public List<Blast6FormatHitDomain> parseOutFile(String outFileName) throws IOException{
		
		BufferedReader br = new BufferedReader(new FileReader(outFileName));
		
		List<Blast6FormatHitDomain> diamondList = new ArrayList<>();
		
		String line;
		while((line = br.readLine()) != null){
			
			String[] sline = line.split("\t");
			
			Blast6FormatHitDomain diamond = new Blast6FormatHitDomain();
			
			diamond.setQuery(sline[0]);
			diamond.setTarget(sline[1]);//  EXAMPLE: hsa:00001|K00001|K00002 
			
			diamond.setIdentity(Double.parseDouble(sline[2]));
			diamond.setAlignmentLength(Integer.parseInt(sline[3]));
			
			diamond.setMismatch(Integer.parseInt(sline[4]));
			diamond.setGap(Integer.parseInt(sline[5]));
			
			diamond.setStartInQuery(Integer.parseInt(sline[6]));
			diamond.setEndInQuery(Integer.parseInt(sline[7]));
	
			diamond.setStartInTarget(Integer.parseInt(sline[8]));
			diamond.setEndInTarget(Integer.parseInt(sline[9]));
			
			diamond.setEvalue(Double.parseDouble(sline[10]));
			diamond.setBitScore(Double.parseDouble(sline[11]));
			
			diamondList.add(diamond);
			
		}//while end
		
		br.close();
		
		return diamondList;
		
	}//method end
	
}
