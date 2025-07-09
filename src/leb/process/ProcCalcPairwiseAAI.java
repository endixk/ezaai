/*
 * Pipeline calculating average amino acid identity (AAI) of two bacteria genome
 * Based on the methodology introduced in Nicholson et al., 2020 (doi: 10.1099/ijsem.0.003935)
 */

package leb.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import leb.util.seq.Blast6FormatHitDomain;
import leb.util.common.FileRemover;
import leb.util.common.Prompt;
import leb.util.config.GenericConfig;
import leb.wrapper.DiamondWrapper;

public class ProcCalcPairwiseAAI {
	public static final int 
		MODE_DEFAULT	= 3,
		MODE_BLASTP 	= 1,
//		MODE_USEARCH 	= 2,
		MODE_MMSEQS 	= 3,
		MODE_DIAMOND	= 4,
		MODE_DSENS		= 5;

	private String globaltmp = "/tmp/ezaai";
	public void setGlobaltmp(String globaltmp) {this.globaltmp = globaltmp;}
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
	private BufferedWriter maw = null;
	public void setMatchout(BufferedWriter maw) {
		this.maw = maw;
	}
	private String label1 = null, label2 = null;
	
	public ProcCalcPairwiseAAI() {}
	
	// returns [cds1, cds2, hit1, hit2, recHit, avglen, aai]
	public List<String> calculateProteomePairWithDetails(String label1, String label2, String faa1, String faa2) throws IOException {
		this.label1 = label1; this.label2 = label2;
		List<String> res = new ArrayList<>();
		// Switch mode
		switch(mode){
			case MODE_BLASTP: return pairwiseBlastp(faa1, faa2);
//			case MODE_USEARCH: break;
			case MODE_MMSEQS: return pairwiseMmseqs(faa1, faa2);
			case MODE_DIAMOND: return pairwiseDiamond(faa1, faa2, false);
			case MODE_DSENS: return pairwiseDiamond(faa1, faa2, true);
			default: break;
		}
		return res;
	}
	
	private void mapLength(BufferedReader br1, BufferedReader br2, Map<String, Integer> lmap,
						   Map<String, Integer> nmap1, Map<String, Integer> nmap2,
						   List<String> nlist1, List<String> nlist2) throws IOException {
		int n1 = 0, n2 = 0;

		String buf;
		while((buf = br1.readLine()) != null){
			if(!buf.startsWith(">")) continue;
			String id = buf.split(" ")[0].substring(1);
			int st = Integer.parseInt(buf.split(" ")[2]),
				ed = Integer.parseInt(buf.split(" ")[4]);

			lmap.put(id, (ed - st - 2) / 3);
			nmap1.put(id, n1++);
			nlist1.add(id);
		}
		while((buf = br2.readLine()) != null){
			if(!buf.startsWith(">")) continue;
			String id = buf.split(" ")[0].substring(1);
			int st = Integer.parseInt(buf.split(" ")[2]),
				ed = Integer.parseInt(buf.split(" ")[4]);

			lmap.put(id, (ed - st - 2) / 3);
			nmap2.put(id, n2++);
			nlist2.add(id);
		}
	}
	
	// calculate AAI from two-way hits
/*	private double calcIdentity(
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
			//	else if(viceMatrix[i][j] >= 40.0 || versaMatrix[i][j] >= 40.0) {
			//		Prompt.debug(String.format("Non-reciprocal hit : %.3f / %.3f", viceMatrix[i][j], versaMatrix[i][j]));
			//	}
			}
		}
		
		if(nval == 0) {
			Prompt.print("WARNING: No reciprocal hits found.");
			return Double.NaN;
		}
		Prompt.talk(String.format("%d reciprocal hits found. Estimated AAI : %.3f", nval, isum / (nval * 2)));
		return isum / (nval * 2);
	} */
	
	// returns [cds1, cds2, hit1, hit2, recHit, avglen, aai]
	private List<String> calcIdentityWithDetails(
			List<Blast6FormatHitDomain> hits_vice,
			List<Blast6FormatHitDomain> hits_versa,
			Map<String, Integer> lengthMap,
			Map<String, Integer> nameMap1,
			Map<String, Integer> nameMap2,
			List<String> nameList1,
			List<String> nameList2) {
		List<String> res = new ArrayList<>();
		int fac = (mode == MODE_MMSEQS ? 100 : 1);
		int n1 = nameMap1.size(), n2 = nameMap2.size();
		Prompt.debug(String.format("n1 = %d, n2 = %d, l1 = %d, l2= %d", n1, n2, nameList1.size(), nameList2.size()));
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
				if(viceMatrix[i][j] >= identity && versaMatrix[i][j] >= identity){
					nval++;
					isum += viceMatrix[i][j] + versaMatrix[i][j];
					lsum += viceLength[i][j] + versaLength[i][j];
					if(maw != null) {
						try {
							maw.write(String.format("%d\t%d\t%s\t%s\t%s\t%s\t%.3f\t%.3f\t%.3f\n",
									Math.abs(label1.hashCode()) % (1<<30), Math.abs(label2.hashCode()) % (1<<30),
									label1, label2, nameList1.get(j), nameList2.get(i),
									viceMatrix[i][j], versaMatrix[i][j], (viceMatrix[i][j] + versaMatrix[i][j]) / 2));
						} catch(IOException e) {
							Prompt.error("FATAL ERROR : Failed to write match output.");
							return null;
						}
					}
				}
				/* else if(viceMatrix[i][j] >= 40.0 || versaMatrix[i][j] >= 40.0) {
					Prompt.debug(String.format("Non-reciprocal hit : %.3f / %.3f", viceMatrix[i][j], versaMatrix[i][j]));
				} */
			}
		}
		
		if(nval == 0) {
			Prompt.print("WARNING: No reciprocal hits found.");
			res.add("0");
			res.add("NaN");
			res.add("NaN");
			return res;
		}

		Prompt.talk(String.format("%d reciprocal hits found. Estimated AAI : %.3f", nval, isum / (nval * 2)));
		res.add(String.valueOf(nval));
		res.add(String.valueOf(lsum / (nval * 2)));
		res.add(String.valueOf(isum / (nval * 2)));
		return res;
	}
	
	private List<String> pairwiseBlastp(String faa1, String faa2) throws IOException {
		// Read sequence lengths
		BufferedReader 	br1 = new BufferedReader(new FileReader(faa1)),
						br2 = new BufferedReader(new FileReader(faa2));
		Map<String, Integer> lengthMap = new HashMap<>();
		Map<String, Integer> nameMap1  = new HashMap<>(),
							 nameMap2  = new HashMap<>();
		List<String> nameList1 = new ArrayList<>(),
					 nameList2 = new ArrayList<>();
		
		mapLength(br1, br2, lengthMap, nameMap1, nameMap2, nameList1, nameList2);
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
		procBlast.setOutFileName(globaltmp + File.separator + GenericConfig.TEMP_HEADER + "vice.out");
		List<Blast6FormatHitDomain> hits_vice  = procBlast.execute(faa1, faa2, GenericConfig.VERB);
		Prompt.print(String.format("Running BLASTp+... (%s vs. %s)", faa2, faa1));
		procBlast.setOutFileName(globaltmp + File.separator + GenericConfig.TEMP_HEADER + "versa.out");
		List<Blast6FormatHitDomain> hits_versa = procBlast.execute(faa2, faa1, GenericConfig.VERB);

		// Clean up stubs
		if(!GenericConfig.KEEP) {
			FileRemover.safeDelete(faa1 + ".pin");
			FileRemover.safeDelete(faa1 + ".phr");
			FileRemover.safeDelete(faa1 + ".psq");
			FileRemover.safeDelete(faa2 + ".pin");
			FileRemover.safeDelete(faa2 + ".phr");
			FileRemover.safeDelete(faa2 + ".psq");
		}
		
		// Collect pairs with reciprocal hits with id 40%+, q_cov 50%+
		return calcIdentityWithDetails(hits_vice, hits_versa, lengthMap, nameMap1, nameMap2, nameList1, nameList2);
	}

	private List<String> pairwiseMmseqs(String faa1, String faa2) throws IOException {
		// Read sequence lengths
		BufferedReader 	br1 = new BufferedReader(new FileReader(faa1)),
						br2 = new BufferedReader(new FileReader(faa2));
		Map<String, Integer> lengthMap = new HashMap<>();
		Map<String, Integer> nameMap1  = new HashMap<>(),
							 nameMap2  = new HashMap<>();
		List<String> nameList1 = new ArrayList<>(),
					 nameList2 = new ArrayList<>();
				
		mapLength(br1, br2, lengthMap, nameMap1, nameMap2, nameList1, nameList2);
		br1.close(); br2.close();
		
		// Run MMSeqs2 reciprocal best hit search
		Prompt.talk("Preparing to run reciprocal MMSeqs2 search...");
		ProcFuncAnnoByMMSeqs2 procMmseqs = new ProcFuncAnnoByMMSeqs2();
		if(path == null) path = "mmseqs";
		procMmseqs.setMmseqsPath(path);
		
		File mmout = new File(globaltmp + File.separator + GenericConfig.SESSION_UID + "_MM");
		if(!mmout.exists()) mmout.mkdir();
		else if(!mmout.isDirectory()) {
			Prompt.error("FATAL ERROR : MMSeqs2 output directory could not be created.");
			return null;
		}
		String outDir = mmout.getAbsolutePath();
		String tmpDir = globaltmp + File.separator + GenericConfig.SESSION_UID + "_tmp";
		
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
			FileRemover.safeDeleteDirectory(globaltmp + File.separator + GenericConfig.SESSION_UID + "_MM");
			FileRemover.safeDeleteDirectory(globaltmp + File.separator + GenericConfig.SESSION_UID + "_tmp");
		}
		
		// Collect pairs with reciprocal hits with id 40%+, q_cov 50%+
		return calcIdentityWithDetails(hits_vice, hits_versa, lengthMap, nameMap2, nameMap1, nameList1, nameList2);
	}
	
	private List<String> pairwiseDiamond(String faa1, String faa2, boolean sensitive) throws IOException {
		// Read sequence lengths
		BufferedReader 	br1 = new BufferedReader(new FileReader(faa1)),
						br2 = new BufferedReader(new FileReader(faa2));
		Map<String, Integer> lengthMap = new HashMap<>();
		Map<String, Integer> nameMap1  = new HashMap<>(),
							 nameMap2  = new HashMap<>();
		List<String> nameList1 = new ArrayList<>(),
					 nameList2 = new ArrayList<>();
				
		mapLength(br1, br2, lengthMap, nameMap1, nameMap2, nameList1, nameList2);
		br1.close(); br2.close();
		
		// Run MMSeqs2 reciprocal best hit search
		Prompt.talk("Preparing to run reciprocal Diamond search...");
		ProcFuncAnnoByDiamond procDiamond = new ProcFuncAnnoByDiamond(DiamondWrapper.BLASTP);
		if(path == null) path = "diamond";
		procDiamond.setDiamondPath(path);
		
		File dmout = new File(globaltmp + File.separator + GenericConfig.SESSION_UID + "_DM");
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
			FileRemover.safeDeleteDirectory(globaltmp + File.separator + GenericConfig.SESSION_UID + "_DM");
		}
		
		// Collect pairs with reciprocal hits with id 40%+, q_cov 50%+
		return calcIdentityWithDetails(hits_vice, hits_versa, lengthMap, nameMap2, nameMap1, nameList1, nameList2);
	}
}
