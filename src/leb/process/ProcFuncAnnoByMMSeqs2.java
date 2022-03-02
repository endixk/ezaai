package leb.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import leb.util.seq.Blast6FormatHitDomain;
import leb.wrapper.MMSeqs2Wrapper;

public class ProcFuncAnnoByMMSeqs2 {
	private String mmseqsPath		= "mmseqs";
	private String queryFileName 	= null;
	private String targetFileName 	= null;
	private String outDir 			= ".";
	private String tmpDir 			= "tmp";
	private int	   threads			= 72;
	private int    alignmentMode	= 2;
	
	public String getMmseqsPath()		{return mmseqsPath;}
	public String getQueryFileName() 	{return queryFileName;}
	public String getTargetFileName() 	{return targetFileName;}
	public String getOutDir() 			{return outDir;}
	public String getTmpDir() 			{return tmpDir;}
	public int    getThreads()			{return threads;}
	public int    getAlignmentMode()	{return alignmentMode;}
	
	public void setMmseqsPath		(String mmseqsPath)		{this.mmseqsPath = mmseqsPath;}
	public void setQueryFileName	(String queryFileName) 	{this.queryFileName = queryFileName;}
	public void setTargetFileName	(String targetFileName) {this.targetFileName = targetFileName;}
	public void setOutDir			(String outDir)			{this.outDir = outDir;}
	public void setTmpDir			(String tmpDir)			{this.tmpDir = tmpDir;}
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
		
		BufferedReader br = new BufferedReader(new FileReader(new File(outFileName)));
		
		List<Blast6FormatHitDomain> hitList = new ArrayList<Blast6FormatHitDomain>();
		
		String line = null;
		while((line = br.readLine()) != null){
			
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
			
		}
		
		br.close();
		
		return hitList;
		
	}
	
	public List<Blast6FormatHitDomain> execute() throws IOException {
		executeCreateDb(queryFileName, outDir + File.separator + "qry");
		executeCreateDb(targetFileName, outDir + File.separator + "tgt");
		executeSearch(outDir + File.separator + "qry", outDir + File.separator + "tgt", outDir + File.separator + "aln", tmpDir);
		executeConvertAlis(outDir + File.separator + "qry", outDir + File.separator + "tgt", outDir + File.separator + "aln", outDir + File.separator + "aln.m8");
		return parseOutFile(outDir + File.separator + "aln.m8");
	}
}
