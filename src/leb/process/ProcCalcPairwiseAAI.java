/*
 * Pipeline calculating average amino acid identity (AAI) of two bacteria genome
 * Based on the methodology introduced in Nicholson et al., 2020 (doi: 10.1099/ijsem.0.003935)
 */

package leb.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import leb.util.seq.Blast6FormatHitDomain;
import leb.util.common.Prompt;
import leb.util.config.GenericConfig;
import leb.wrapper.DiamondWrapper;

public class ProcCalcPairwiseAAI {
	public static final String TMPDIR = "/tmp/";
	public static final int 
		MODE_DEFAULT	= 3,
		MODE_BLASTP 	= 1,
//		MODE_USEARCH 	= 2,
		MODE_MMSEQS 	= 3,
		MODE_DIAMOND	= 4,
		MODE_DSENS		= 5;
	
	private int mode = MODE_DEFAULT;
	public void setMode(int mode) {this.mode = mode;}
	private int nthread = 1;
	public void setNthread(int nthread) {this.nthread = nthread;}
	private double identity = 40.0, coverage = 0.5;
	private String path = null;
	public void setPath(String path) {this.path = path;}
	
	private String dbpath = null; // makeblastdb
	public void setDbpath(String dbpath) {this.dbpath = dbpath;}
	
	public void setIdentity(double identity) {
		this.identity = identity * 100;
	}
	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}
	
	public ProcCalcPairwiseAAI() {}
	
	// returns [cds1, cds2, hit1, hit2, recHit, avglen, aai]
	public List<String> calculateProteomePairWithDetails(String faa1, String faa2) throws IOException {
		List<String> res = new ArrayList<String>();
		// Switch mode
		switch(mode){
			case MODE_BLASTP: return pairwiseBlastp(faa1, faa2, true);
//			case MODE_USEARCH: break;
			case MODE_MMSEQS: return pairwiseMmseqs(faa1, faa2, true);
			case MODE_DIAMOND: return pairwiseDiamond(faa1, faa2, false, true);
			case MODE_DSENS: return pairwiseDiamond(faa1, faa2, true, true);
			default: break;
		}
		return res;
	}
	
	private void mapLength(BufferedReader br1, BufferedReader br2, 
			Map<String, Integer> lmap, 
			Map<String, Integer> nmap1, 
			Map<String, Integer> nmap2) throws IOException {
		int n1 = 0, n2 = 0;

		String buf;
		while((buf = br1.readLine()) != null){
			if(!buf.startsWith(">")) continue;
			String id = buf.split(" ")[0].substring(1);
			int st = Integer.parseInt(buf.split(" ")[2]),
				ed = Integer.parseInt(buf.split(" ")[4]);

			lmap.put(id, (ed - st - 2) / 3);
			nmap1.put(id, n1++);
		}
		while((buf = br2.readLine()) != null){
			if(!buf.startsWith(">")) continue;
			String id = buf.split(" ")[0].substring(1);
			int st = Integer.parseInt(buf.split(" ")[2]),
				ed = Integer.parseInt(buf.split(" ")[4]);

			lmap.put(id, (ed - st - 2) / 3);
			nmap2.put(id, n2++);
		}
	}
	
	// calculate AAI from two-way hits
	private double calcIdentity(
			List<Blast6FormatHitDomain> hits_vice,
			List<Blast6FormatHitDomain> hits_versa,
			Map<String, Integer> lengthMap,
			Map<String, Integer> nameMap1,
			Map<String, Integer> nameMap2) {
		int fac = (mode == MODE_MMSEQS ? 100 : 1);
		int n1 = nameMap1.size(), n2 = nameMap2.size();
		double[][] viceMatrix = new double[n1][n2], versaMatrix = new double[n1][n2];
		int hcnt = 0;
		for(Blast6FormatHitDomain hit : hits_vice){
			if(hit.getIdentity() * fac >= identity && ((double) (hit.getEndInQuery() - hit.getStartInQuery()) / lengthMap.get(hit.getQuery().split(" ")[0])) >= coverage){
				viceMatrix[nameMap1.get(hit.getTarget().split(" ")[0])][nameMap2.get(hit.getQuery().split(" ")[0])] = hit.getIdentity() * fac;
				hcnt++;
				if(hcnt < 10) Prompt.debug(String.format("Forward hit #%d : %s\tvs.%s\t= %.3f", hcnt, hit.getTarget().split(" ")[0], hit.getQuery().split(" ")[0], hit.getIdentity() * fac));
			}
		}
		Prompt.talk(String.format("%d forward hits found.", hcnt));

		hcnt = 0;
		for(Blast6FormatHitDomain hit : hits_versa){
			if(hit.getIdentity() * fac >= identity && ((double) (hit.getEndInQuery() - hit.getStartInQuery()) / lengthMap.get(hit.getQuery().split(" ")[0])) >= coverage){
				versaMatrix[nameMap1.get(hit.getQuery().split(" ")[0])][nameMap2.get(hit.getTarget().split(" ")[0])] = hit.getIdentity() * fac;
				hcnt++;
				if(hcnt < 10) Prompt.debug(String.format("Backward hit #%d : %s\tvs.%s\t= %.3f", hcnt, hit.getQuery().split(" ")[0], hit.getTarget().split(" ")[0], hit.getIdentity() * fac));
			}
		}
		Prompt.talk(String.format("%d backward hits found.", hcnt));

		// Calculate arithmetic mean of identities
		int nval = 0; double isum = .0;
		for(int i = 0; i < n1; i++){
			for(int j = 0; j < n2; j++){
				if(viceMatrix[i][j] >= 40.0 && versaMatrix[i][j] >= 40.0){
					nval++;
					isum += viceMatrix[i][j] + versaMatrix[i][j];
				}
				/* else if(viceMatrix[i][j] >= 40.0 || versaMatrix[i][j] >= 40.0) {
					Prompt.debug(String.format("Non-reciprocal hit : %.3f / %.3f", viceMatrix[i][j], versaMatrix[i][j]));
				} */
			}
		}

		Prompt.talk(String.format("%d reciprocal hits found. Estimated AAI : %.3f", nval, isum / (nval * 2)));
		return isum / (nval * 2);
	}
	
	// returns [cds1, cds2, hit1, hit2, recHit, avglen, aai]
	private List<String> calcIdentityWithDetails(
			List<Blast6FormatHitDomain> hits_vice,
			List<Blast6FormatHitDomain> hits_versa,
			Map<String, Integer> lengthMap,
			Map<String, Integer> nameMap1,
			Map<String, Integer> nameMap2) {
		List<String> res = new ArrayList<String>();
		int fac = (mode == MODE_MMSEQS ? 100 : 1);
		int n1 = nameMap1.size(), n2 = nameMap2.size();
		res.add(String.valueOf(n2));
		res.add(String.valueOf(n1));

		double[][] viceMatrix = new double[n1][n2], versaMatrix = new double[n1][n2];
		int[][] viceLength = new int[n1][n2], versaLength = new int[n1][n2];
		int hcnt = 0;
		for(Blast6FormatHitDomain hit : hits_vice){
			if(hit.getIdentity() * fac >= identity && ((double) (hit.getEndInQuery() - hit.getStartInQuery()) / lengthMap.get(hit.getQuery().split(" ")[0])) >= coverage){
				viceMatrix[nameMap1.get(hit.getTarget().split(" ")[0])][nameMap2.get(hit.getQuery().split(" ")[0])] = hit.getIdentity() * fac;
				viceLength[nameMap1.get(hit.getTarget().split(" ")[0])][nameMap2.get(hit.getQuery().split(" ")[0])] = hit.getAlignmentLength();
				hcnt++;
				if(hcnt < 10) Prompt.debug(String.format("Forward hit #%d : %s\tvs.%s\t= %.3f", hcnt, hit.getTarget().split(" ")[0], hit.getQuery().split(" ")[0], hit.getIdentity() * fac));
			}
		}
		Prompt.talk(String.format("%d forward hits found.", hcnt));
		res.add(String.valueOf(hcnt));
		
		hcnt = 0;
		for(Blast6FormatHitDomain hit : hits_versa){
			if(hit.getIdentity() * fac >= identity && ((double) (hit.getEndInQuery() - hit.getStartInQuery()) / lengthMap.get(hit.getQuery().split(" ")[0])) >= coverage){
				versaMatrix[nameMap1.get(hit.getQuery().split(" ")[0])][nameMap2.get(hit.getTarget().split(" ")[0])] = hit.getIdentity() * fac;
				versaLength[nameMap1.get(hit.getQuery().split(" ")[0])][nameMap2.get(hit.getTarget().split(" ")[0])] = hit.getAlignmentLength();
				hcnt++;
				if(hcnt < 10) Prompt.debug(String.format("Backward hit #%d : %s\tvs.%s\t= %.3f", hcnt, hit.getQuery().split(" ")[0], hit.getTarget().split(" ")[0], hit.getIdentity() * fac));
			}
		}
		Prompt.talk(String.format("%d backward hits found.", hcnt));
		res.add(String.valueOf(hcnt));
		
		// Calculate arithmetic mean of identities
		int nval = 0; double isum = .0; int lsum = 0;
		for(int i = 0; i < n1; i++){
			for(int j = 0; j < n2; j++){
				if(viceMatrix[i][j] >= 40.0 && versaMatrix[i][j] >= 40.0){
					nval++;
					isum += viceMatrix[i][j] + versaMatrix[i][j];
					lsum += viceLength[i][j] + versaLength[i][j];
				}
				/* else if(viceMatrix[i][j] >= 40.0 || versaMatrix[i][j] >= 40.0) {
					Prompt.debug(String.format("Non-reciprocal hit : %.3f / %.3f", viceMatrix[i][j], versaMatrix[i][j]));
				} */
			}
		}
		
		if(nval == 0) {
			Prompt.print("WARNING: No reciprocal hits found.");
			res.add("0");
			res.add("0.0");
			res.add("0.0");
		}

		Prompt.talk(String.format("%d reciprocal hits found. Estimated AAI : %.3f", nval, isum / (nval * 2)));
		res.add(String.valueOf(nval));
		res.add(String.valueOf(lsum / (nval * 2)));
		res.add(String.valueOf(isum / (nval * 2)));
		return res;
	}
	
	private List<String> pairwiseBlastp(String faa1, String faa2, boolean benchmark) throws IOException {
		// Read sequence lengths
		BufferedReader 	br1 = new BufferedReader(new FileReader(faa1)),
						br2 = new BufferedReader(new FileReader(faa2));
		Map<String, Integer> lengthMap = new HashMap<String, Integer>();
		Map<String, Integer> nameMap1  = new HashMap<String, Integer>(),
							 nameMap2  = new HashMap<String, Integer>();
		
		mapLength(br1, br2, lengthMap, nameMap1, nameMap2);
		br1.close(); br2.close();

		// Run pairwise BLASTp
		Prompt.print("Preparing to run reciprocal BLASTp+...");
		ProcFuncAnnoByBlastPlus procBlast = new ProcFuncAnnoByBlastPlus();
		if(path == null) path = "blastp";
		if(dbpath == null) dbpath = "makeblastdb";
		
		procBlast.setProgramPath(path);
		procBlast.setBlastdbPath(dbpath);
		procBlast.setEvalue(.1);
		procBlast.setThreads(nthread);
		procBlast.setKeepBlastOutput(GenericConfig.KEEP);
		
		procBlast.executeMakeBlastDb(faa1, 1, GenericConfig.VERB);
		procBlast.executeMakeBlastDb(faa2, 1, GenericConfig.VERB);
		Prompt.print(String.format("Running BLASTp+... (%s vs. %s)", faa1, faa2));
		procBlast.setOutFileName(TMPDIR + File.separator + GenericConfig.TEMP_HEADER + "vice.out");
		List<Blast6FormatHitDomain> hits_vice  = procBlast.execute(faa1, faa2, GenericConfig.VERB);
		Prompt.print(String.format("Running BLASTp+... (%s vs. %s)", faa2, faa1));
		procBlast.setOutFileName(TMPDIR + File.separator + GenericConfig.TEMP_HEADER + "versa.out");
		List<Blast6FormatHitDomain> hits_versa = procBlast.execute(faa2, faa1, GenericConfig.VERB);

		// Clean up stubs
		if(!GenericConfig.KEEP) {
			(new File(faa1 + ".pin")).delete();
			(new File(faa1 + ".phr")).delete();
			(new File(faa1 + ".psq")).delete();
			(new File(faa2 + ".pin")).delete();
			(new File(faa2 + ".phr")).delete();
			(new File(faa2 + ".psq")).delete();
		}
		
		// Collect pairs with reciprocal hits with id 40%+, q_cov 50%+
		if(benchmark) {
			return calcIdentityWithDetails(hits_vice, hits_versa, lengthMap, nameMap1, nameMap2);
		}
		else {
			List<String> res = new ArrayList<String>();
			res.add(String.valueOf(calcIdentity(hits_vice, hits_versa, lengthMap, nameMap1, nameMap2)));
			return res;
		}
	}
/*	
	private double pairwiseUsearch(String faa1, String faa2) throws IOException {
		// Read sequence lengths
		BufferedReader 	br1 = new BufferedReader(new FileReader(faa1)),
						br2 = new BufferedReader(new FileReader(faa2));
		Map<String, Integer> lengthMap = new HashMap<String, Integer>();
		Map<String, Integer> nameMap1  = new HashMap<String, Integer>(),
							 nameMap2  = new HashMap<String, Integer>();
				
		mapLength(br1, br2, lengthMap, nameMap1, nameMap2);
		br1.close(); br2.close();
		
		// Run pairwise USEARCH
		Prompt.print("Preparing to run reciprocal USEARCH...");
		ProcFuncAnnoByUSearch procUsearch = new ProcFuncAnnoByUSearch();
		procUsearch.setEvalue(.1);
		procUsearch.setThreads(nthread);
		procUsearch.setUsearchVersion(10);
		procUsearch.executeMakeUdbUsearch(faa1, faa1 + ".udb", GenericConfig.VERB);
		procUsearch.executeMakeUdbUsearch(faa2, faa2 + ".udb", GenericConfig.VERB);
		
		Prompt.print(String.format("Running USEARCH... (%s vs. %s)", faa1, faa2));
		procUsearch.setDbFileName(faa1 + ".udb");
		procUsearch.setOutFileName(faa1 + ".b6");
		procUsearch.setIdentity(.1);
		List<Blast6FormatHitDomain> hits_vice  = procUsearch.executeUsearchGlobal(faa2, GenericConfig.VERB);
		
		Prompt.print(String.format("Running USEARCH... (%s vs. %s)", faa2, faa1));
		procUsearch.setDbFileName(faa2 + ".udb");
		procUsearch.setOutFileName(faa2 + ".b6");
		procUsearch.setIdentity(.1);
		List<Blast6FormatHitDomain> hits_versa = procUsearch.executeUsearchGlobal(faa1, GenericConfig.VERB);
		
		// Clean up stubs
		if(!GenericConfig.KEEP) {
			if(!BENCH) (new File(faa1)).delete();
			(new File(faa1 + ".udb")).delete();
			(new File(faa1 + ".b6")).delete();
			if(!BENCH) (new File(faa2)).delete();
			(new File(faa2 + ".udb")).delete();
			(new File(faa2 + ".b6")).delete();
		}
		
		// Collect pairs with reciprocal hits with id 40%+, q_cov 50%+
		return calcIdentity(hits_vice, hits_versa, lengthMap, nameMap1, nameMap2);
	}
*/	
	private List<String> pairwiseMmseqs(String faa1, String faa2, boolean benchmark) throws IOException {
		// Read sequence lengths
		BufferedReader 	br1 = new BufferedReader(new FileReader(faa1)),
						br2 = new BufferedReader(new FileReader(faa2));
		Map<String, Integer> lengthMap = new HashMap<String, Integer>();
		Map<String, Integer> nameMap1  = new HashMap<String, Integer>(),
							 nameMap2  = new HashMap<String, Integer>();
				
		mapLength(br1, br2, lengthMap, nameMap1, nameMap2);
		br1.close(); br2.close();
		
		// Run MMSeqs2 reciprocal best hit search
		Prompt.talk("Preparing to run reciprocal MMSeqs2 search...");
		ProcFuncAnnoByMMSeqs2 procMmseqs = new ProcFuncAnnoByMMSeqs2();
		if(path == null) path = "mmseqs";
		procMmseqs.setMmseqsPath(path);
		
		File mmout = new File(TMPDIR + GenericConfig.SESSION_UID + "_MM");
		if(!mmout.exists()) mmout.mkdir();
		else if(!mmout.isDirectory()) {
			Prompt.error("FATAL ERROR : MMSeqs2 output directory could not be created.");
			return null;
		}
		String outDir = mmout.getAbsolutePath();
		String tmpDir = TMPDIR + GenericConfig.SESSION_UID + "_tmp";
		
		procMmseqs.setThreads(nthread);
		procMmseqs.setAlignmentMode(3);
		
		procMmseqs.executeCreateDb(faa1, outDir + File.separator + "db1");
		procMmseqs.executeCreateDb(faa2, outDir + File.separator + "db2");

		Prompt.talk(String.format("Running MMSeqs2 search... (%s vs. %s)", faa1, faa2));
		procMmseqs.executeSearch(
				outDir + File.separator + "db1",
				outDir + File.separator + "db2",
				outDir + File.separator + "vice", tmpDir);
		procMmseqs.executeFilterdb(
				outDir + File.separator + "vice",
				outDir + File.separator + "vice_filt", tmpDir);
		procMmseqs.executeConvertAlis(
				outDir + File.separator + "db1",
				outDir + File.separator + "db2",
				outDir + File.separator + "vice_filt",
				outDir + File.separator + "vice.m8");
		List<Blast6FormatHitDomain> hits_vice  = procMmseqs.parseOutFile(outDir + File.separator + "vice.m8");
		
		Prompt.talk(String.format("Running MMSeqs2 search... (%s vs. %s)", faa2, faa1));
		procMmseqs.executeSearch(
				outDir + File.separator + "db2",
				outDir + File.separator + "db1",
				outDir + File.separator + "versa", tmpDir);
		procMmseqs.executeFilterdb(
				outDir + File.separator + "versa",
				outDir + File.separator + "versa_filt", tmpDir);
		procMmseqs.executeConvertAlis(
				outDir + File.separator + "db2",
				outDir + File.separator + "db1",
				outDir + File.separator + "versa_filt",
				outDir + File.separator + "versa.m8");
		List<Blast6FormatHitDomain> hits_versa = procMmseqs.parseOutFile(outDir + File.separator + "versa.m8");
		
		// Clean up stubs
		if(!GenericConfig.KEEP) {
			FileUtils.deleteDirectory(new File(TMPDIR + GenericConfig.SESSION_UID + "_MM"));
			FileUtils.deleteDirectory(new File(TMPDIR + GenericConfig.SESSION_UID + "_tmp"));
		}
		
		// Collect pairs with reciprocal hits with id 40%+, q_cov 50%+
		if(benchmark) {
			return calcIdentityWithDetails(hits_vice, hits_versa, lengthMap, nameMap2, nameMap1);
		}
		else{
			List<String> res = new ArrayList<String>();
			res.add(String.valueOf(calcIdentity(hits_vice, hits_versa, lengthMap, nameMap2, nameMap1)));
			return res;
		}
	}
	
	private List<String> pairwiseDiamond(String faa1, String faa2, boolean sensitive, boolean benchmark) throws IOException {
		// Read sequence lengths
		BufferedReader 	br1 = new BufferedReader(new FileReader(faa1)),
						br2 = new BufferedReader(new FileReader(faa2));
		Map<String, Integer> lengthMap = new HashMap<String, Integer>();
		Map<String, Integer> nameMap1  = new HashMap<String, Integer>(),
							 nameMap2  = new HashMap<String, Integer>();
				
		mapLength(br1, br2, lengthMap, nameMap1, nameMap2);
		br1.close(); br2.close();
		
		// Run MMSeqs2 reciprocal best hit search
		Prompt.talk("Preparing to run reciprocal Diamond search...");
		ProcFuncAnnoByDiamond procDiamond = new ProcFuncAnnoByDiamond(DiamondWrapper.BLASTP);
		if(path == null) path = "diamond";
		procDiamond.setDiamondPath(path);
		
		File dmout = new File(TMPDIR + GenericConfig.SESSION_UID + "_DM");
		if(!dmout.exists()) dmout.mkdir();
		else if(!dmout.isDirectory()) {
			Prompt.error("FATAL ERROR : Diamond output directory could not be created.");
			return null;
		}
		String outDir = dmout.getAbsolutePath();
		// String tmpDir = TMPDIR + GenericConfig.SESSION_UID + "_tmp";
		
		procDiamond.setSensitive(sensitive);
		procDiamond.setOutDir(outDir);
		procDiamond.executeMakeDB(faa1, outDir + File.separator + "db1", nthread);
		procDiamond.executeMakeDB(faa2, outDir + File.separator + "db2", nthread);
		procDiamond.setIdentity(identity);
		procDiamond.setQcov(coverage * 100);
		
		Prompt.talk(String.format("Running Diamond search... (%s vs. %s)", faa1, faa2));
		List<Blast6FormatHitDomain> hits_vice  = procDiamond.execute(faa1, outDir + File.separator + "db2", nthread);
		Prompt.talk(String.format("Running Diamond search... (%s vs. %s)", faa2, faa1));
		List<Blast6FormatHitDomain> hits_versa = procDiamond.execute(faa2, outDir + File.separator + "db1", nthread);
		
		// Clean up stubs
		if(!GenericConfig.KEEP) {
			FileUtils.deleteDirectory(new File(TMPDIR + GenericConfig.SESSION_UID + "_DM"));
		//	FileUtils.deleteDirectory(TMPDIR + GenericConfig.SESSION_UID + "_tmp");
		}
		
		// Collect pairs with reciprocal hits with id 40%+, q_cov 50%+
		if(benchmark) {
			return calcIdentityWithDetails(hits_vice, hits_versa, lengthMap, nameMap2, nameMap1);
		}
		else{
			List<String> res = new ArrayList<String>();
			res.add(String.valueOf(calcIdentity(hits_vice, hits_versa, lengthMap, nameMap2, nameMap1)));
			return res;
		}
	}
}
