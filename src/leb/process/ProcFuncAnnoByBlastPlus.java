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

	private final String tmpDir = "/tmp/ezaai";

	private double evalue = 0;

	private int threads = 1;
	
	private String outFileName = tmpDir + File.separator + "out_tab.txt";
	
	private boolean keepBlastOutput = false;
	
	private String programPath = null;
	
	private String blastdbPath = null;

	public void setEvalue(double evalue) {
		this.evalue = evalue;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}
	
	public void setOutFileName(String outFileName) {
		this.outFileName = outFileName;
	}
	
	public void setKeepBlastOutput(boolean keepBlastOutput) {
		this.keepBlastOutput = keepBlastOutput;
	}
	
	public void setProgramPath(String programPath) {
		this.programPath = programPath;
	}
	
	public void setBlastdbPath(String blastdbPath) {
		this.blastdbPath = blastdbPath;
	}

	public void executeMakeBlastDb(String inFileName, int dbType, boolean ignoredVerbose) {
		MakeBlastDbWrapper blast = new MakeBlastDbWrapper(blastdbPath, inFileName, dbType);
		blast.setOutFileNames(inFileName);
		blast.run();
	
	}

	public List<Blast6FormatHitDomain> execute(String dbFileName, String inFileName, boolean ignoredVerbose) throws IOException{
		BlastPlusWrapper blast = new BlastPlusWrapper(programPath);//It should be changable
		
		blast.setProgramPath(programPath);

		int outFmt = 6;
		blast.setOutFmt(outFmt);
		blast.setDbFileName(dbFileName);
		blast.setInFileName(inFileName);
		
		File f_out = new File(outFileName);
		blast.setOutFileName(f_out.getAbsolutePath());
		blast.setFilterEvalue(evalue);


		int maxTargetSeqs = 1;
		blast.setMaxTargetSequence(maxTargetSeqs);
		blast.setCoreForMultiThread(threads);
		
		blast.run();
		
		List<Blast6FormatHitDomain> hitList = parseOutFile(f_out.getAbsolutePath());

		if(!keepBlastOutput) f_out.delete();
		
		return hitList;
		
	}//method end
	
	public List<Blast6FormatHitDomain> parseOutFile(String outFileName) throws IOException{
		
		BufferedReader br = new BufferedReader(new FileReader(outFileName));
		
		List<Blast6FormatHitDomain> hitList = new ArrayList<>();
		
		String line;
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
			hit.setAlignmentLength(Integer.parseInt(sline[3]));
			
			hit.setMismatch(Integer.parseInt(sline[4]));
			hit.setGap(	Integer.parseInt(sline[5]));
			
			hit.setStartInQuery(Integer.parseInt(sline[6]));
			hit.setEndInQuery(Integer.parseInt(sline[7]));
	
			hit.setStartInTarget(Integer.parseInt(sline[8]));
			hit.setEndInTarget(Integer.parseInt(sline[9]));
			
			if(!sline[10].equals("*")) hit.setEvalue(Double.parseDouble(sline[10]));
			if(!sline[11].equals("*")) hit.setBitScore(Double.parseDouble(sline[11]));
			
			hitList.add(hit);
			
		}//while end
		
		br.close();
		
		return hitList;
		
	}//method end 

}


