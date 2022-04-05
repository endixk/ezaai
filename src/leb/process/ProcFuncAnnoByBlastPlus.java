package leb.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import leb.util.seq.Blast6FormatHitDomain;
import leb.wrapper.BlastPlusWrapper;
import leb.wrapper.MakeBlastDbWrapper;

public class ProcFuncAnnoByBlastPlus {

	private String tmpDir = "/tmp";
	
	private int dbSize = 0;
	
	private int outFmt = 6;
	
	private double evalue = 0;
	
	private int maxTargetSeqs = 1;
	
	private int threads = 1;
	
	private int queryGenCode = 1;
	
	private int dbGenCode = 1;

	private double inclusionEvalue = 0;
	
	private String msaFileName = null;
	
	private String inPssmFileName = null;
	
	private String outPssmFileName = null;
	
	private String outFileName = tmpDir + File.separator + "out_tab.txt";
	
	private int stat = 2;
	
	private int iteration = 1;
	
	private boolean keepBlastOutput = false;
	
	private String programPath = null;
	
	private String blastdbPath = null;
	
	public String getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}

	public int getDbSize() {
		return dbSize;
	}

	public void setDbSize(int dbSize) {
		this.dbSize = dbSize;
	}

	public int getOutFmt() {
		return outFmt;
	}

	public void setOutFmt(int outFmt) {
		this.outFmt = outFmt;
	}

	public double getEvalue() {
		return evalue;
	}

	public void setEvalue(double evalue) {
		this.evalue = evalue;
	}

	public int getMaxTargetSeqs() {
		return maxTargetSeqs;
	}

	public void setMaxTargetSeqs(int maxTargetSeqs) {
		this.maxTargetSeqs = maxTargetSeqs;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public int getQueryGenCode() {
		return queryGenCode;
	}

	public void setQueryGenCode(int queryGenCode) {
		this.queryGenCode = queryGenCode;
	}

	public int getDbGenCode() {
		return dbGenCode;
	}

	public void setDbGenCode(int dbGenCode) {
		this.dbGenCode = dbGenCode;
	}

	public double getInclusionEvalue() {
		return inclusionEvalue;
	}

	public void setInclusionEvalue(double inclusionEvalue) {
		this.inclusionEvalue = inclusionEvalue;
	}

	public String getMsaFileName() {
		return msaFileName;
	}

	public void setMsaFileName(String msaFileName) {
		this.msaFileName = msaFileName;
	}

	public String getInPssmFileName() {
		return inPssmFileName;
	}

	public void setInPssmFileName(String inPssmFileName) {
		this.inPssmFileName = inPssmFileName;
	}

	public String getOutPssmFileName() {
		return outPssmFileName;
	}

	public void setOutPssmFileName(String outPssmFileName) {
		this.outPssmFileName = outPssmFileName;
	}

	public int getStat() {
		return stat;
	}

	public void setStat(int stat) {
		this.stat = stat;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
	
	public String getOutFileName() {
		return outFileName;
	}
	
	public void setOutFileName(String outFileName) {
		this.outFileName = outFileName;
	}
	
	public boolean getKeepBlastOutput() {
		return keepBlastOutput;
	}
	
	public void setKeepBlastOutput(boolean keepBlastOutput) {
		this.keepBlastOutput = keepBlastOutput;
	}
	
	public String getProgramPath() {
		return programPath;
	}
	
	public void setProgramPath(String programPath) {
		this.programPath = programPath;
	}
	
	public String getBlastdbPath() {
		return blastdbPath;
	}
	
	public void setBlastdbPath(String blastdbPath) {
		this.blastdbPath = blastdbPath;
	}
	
	public void executeMakeBlastDb(String inFileName, int dbType) throws IOException{
		executeMakeBlastDb(inFileName, dbType, true);
	}
	public void executeMakeBlastDb(String inFileName, int dbType, boolean verbose) throws IOException{
		MakeBlastDbWrapper blast = new MakeBlastDbWrapper(blastdbPath, inFileName, dbType);
		blast.setOutFileNames(inFileName);
		blast.run();
	
	}//method end
	
	public List<Blast6FormatHitDomain> execute(String dbFileName, String inFileName) throws IOException{
		return execute(dbFileName, inFileName, true);
	}
	public List<Blast6FormatHitDomain> execute(String dbFileName, String inFileName, boolean verbose) throws IOException{
		BlastPlusWrapper blast = new BlastPlusWrapper(programPath);//It should be changable
		
		blast.setProgramPath(programPath);
		
		blast.setOutFmt(outFmt);
		blast.setDbFileName(dbFileName);
		blast.setInFileName(inFileName);

		if(dbSize != 0){
			blast.setDbSize(dbSize);
		}
		
		File f_out = new File(outFileName);
		blast.setOutFileName(f_out.getAbsolutePath());
		blast.setFilterEvalue(evalue);
		
		
		blast.setMaxTargetSequence(maxTargetSeqs);
		blast.setCoreForMultiThread(threads);
		
		blast.run();
		
		List<Blast6FormatHitDomain> hitList = parseOutFile(f_out.getAbsolutePath());

		if(!keepBlastOutput) f_out.delete();
		
		return hitList;
		
	}//method end
	
	public List<Blast6FormatHitDomain> parseOutFile(String outFileName) throws IOException{
		
		BufferedReader br = new BufferedReader(new FileReader(new File(outFileName)));
		
		List<Blast6FormatHitDomain> hitList = new ArrayList<Blast6FormatHitDomain>();
		
		String line = null;
		while((line = br.readLine()) != null){
			
			//For psiblast
			if(line.length() < 1){
				continue;
			}else if(line.startsWith("Search has CONVERGED!")){
				continue;
			}
			
			String[] sline = line.split("\t");
			
			Blast6FormatHitDomain hit = new Blast6FormatHitDomain();
			
			hit.setQuery(sline[0]);
			hit.setTarget(sline[1]);//  EXAMPLE: hsa:00001|K00001|K00002 
			
			hit.setIdentity(Double.parseDouble(sline[2]));
			hit.setAlignmentLength(Integer.valueOf(sline[3]));
			
			hit.setMismatch(Integer.valueOf(sline[4]));
			hit.setGap(Integer.valueOf(sline[5]));
			
			hit.setStartInQuery(Integer.valueOf(sline[6]));
			hit.setEndInQuery(Integer.valueOf(sline[7]));
	
			hit.setStartInTarget(Integer.valueOf(sline[8]));
			hit.setEndInTarget(Integer.valueOf(sline[9]));
			
			if(sline[10].equals("*")== false){
				hit.setEvalue(Double.parseDouble(sline[10]));
			}
			
			if(sline[11].equals("*")== false){
				hit.setBitScore(Double.parseDouble(sline[11]));
			}
			
			hitList.add(hit);
			
		}//while end
		
		br.close();
		
		return hitList;
		
	}//method end 

}


