package leb.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import leb.util.seq.Blast6FormatHitDomain;
import leb.wrapper.MMSeqs2Wrapper;

public class ProcFuncAnnoByMMSeqs2 {
	private String mmseqsPath		= "mmseqs";
	private int	   threads			= 72;
	private int    alignmentMode	= 2;
	
	public void setMmseqsPath		(String mmseqsPath)		{this.mmseqsPath = mmseqsPath;}
	public void setThreads			(int    threads)		{this.threads = threads;}
	public void setAlignmentMode	(int	alignmentMode)	{this.alignmentMode = alignmentMode;}
	
	public void executeCreateDb(String fileName, String dbName) {
		MMSeqs2Wrapper.setMmseqsPath(mmseqsPath);
		MMSeqs2Wrapper mmseqs = new MMSeqs2Wrapper();
		mmseqs.runCreateDb(fileName, dbName);
	}
	public void executeSearch(String queryDbName, String targetDbName, String alignName, String tmpDir) {
		MMSeqs2Wrapper.setMmseqsPath(mmseqsPath);
		MMSeqs2Wrapper mmseqs = new MMSeqs2Wrapper();
//		mmseqs.setMaxAccept(1);
		mmseqs.setThreads(threads);
		mmseqs.setAlignmentMode(alignmentMode);
		mmseqs.runSearch(queryDbName, targetDbName, alignName, tmpDir);
	}
	public void executeFilterdb(String inDbName, String outDbName, String tmpDir) {
		MMSeqs2Wrapper.setMmseqsPath(mmseqsPath);
		MMSeqs2Wrapper mmseqs = new MMSeqs2Wrapper();
		mmseqs.setThreads(threads);
		mmseqs.runFilterdb(inDbName, outDbName, tmpDir);
	}
	public void executeConvertAlis(String queryDbName, String targetDbName, String alignName, String outFileName) {
		MMSeqs2Wrapper.setMmseqsPath(mmseqsPath);
		MMSeqs2Wrapper mmseqs = new MMSeqs2Wrapper();
		mmseqs.setThreads(threads);
		mmseqs.runConvertAlis(queryDbName, targetDbName, alignName, outFileName);
	}
	public void executeConvert2Fasta(String inDbName, String outFileName) {
		MMSeqs2Wrapper.setMmseqsPath(mmseqsPath);
		MMSeqs2Wrapper mmseqs = new MMSeqs2Wrapper();
		mmseqs.setThreads(threads);
		mmseqs.runConvert2Fasta(inDbName, outFileName);
	}
	public List<Blast6FormatHitDomain> parseOutFile(String outFileName) throws IOException{
		
		BufferedReader br = new BufferedReader(new FileReader(outFileName));
		
		List<Blast6FormatHitDomain> hitList = new ArrayList<>();
		
		String line;
		while((line = br.readLine()) != null){
			
			String[] sline = line.split("\t");
			
			Blast6FormatHitDomain hit = new Blast6FormatHitDomain();
			
			hit.setQuery(sline[0]);
			hit.setTarget(sline[1]);//  EXAMPLE: hsa:00001|K00001|K00002 
			
			hit.setIdentity(Double.parseDouble(sline[2]));
			hit.setAlignmentLength(Integer.parseInt(sline[3]));
			
			hit.setMismatch(Integer.parseInt(sline[4]));
			hit.setGap(Integer.parseInt(sline[5]));
			
			hit.setStartInQuery(Integer.parseInt(sline[6]));
			hit.setEndInQuery(Integer.parseInt(sline[7]));
	
			hit.setStartInTarget(Integer.parseInt(sline[8]));
			hit.setEndInTarget(Integer.parseInt(sline[9]));
			
			if(!sline[10].equals("*")) hit.setEvalue(Double.parseDouble(sline[10]));
			if(!sline[11].equals("*")) hit.setBitScore(Double.parseDouble(sline[11]));
			
			hitList.add(hit);
			
		}
		
		br.close();
		
		return hitList;
		
	}
}
