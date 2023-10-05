package leb.main;

import leb.process.ProcCalcPairwiseAAI;
import leb.process.ProcFuncAnnoByMMSeqs2;
import leb.process.ProcUPGMA;
import leb.process.ProcCDSPredictionByProdigal;
import leb.process.ProcParallelProdigal;
import leb.util.common.ANSIHandler;
import leb.util.common.Arguments;
import leb.util.common.Prompt;
import leb.util.common.Shell;
import leb.util.config.GenericConfig;
import leb.util.seq.DnaSeqDomain;
import leb.util.seq.Seqtools;
import leb.util.seq.FastSeqLoader;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import java.util.ArrayList;

public class EzAAI {
	public static final String VERSION  = "v1.2.2",
							   RELEASE  = "Aug. 2022",
							   CITATION = " Kim, D., Park, S. & Chun, J.\n"
							   			+ " Introducing EzAAI: a pipeline for high throughput calculations of prokaryotic average amino acid identity.\n"
							   			+ " J Microbiol. 59, 476–480 (2021).\n"
							   			+ " DOI: 10.1007/s12275-021-1154-0";
	
	final static int MODULE_CONVERT 	= 1,
					 MODULE_EXTRACT 	= 2,
					 MODULE_CALCULATE 	= 3,
					 MODULE_CLUSTER		= 4,
					 MODULE_INVALID 	= 0;
	
	final static int PROGRAM_MMSEQS		= 1,
					 PROGRAM_DIAMOND	= 2,
					 PROGRAM_BLASTP		= 3,
					 PROGRAM_PRODIGAL	= 4,
					 PROGRAM_BLASTDB	= 5,
					 PROGRAM_UFASTA		= 6;
	
	int module = MODULE_INVALID;
	public EzAAI(String module) {
		if(module.equals("convert")) 	this.module = MODULE_CONVERT;
		if(module.equals("extract")) 	this.module = MODULE_EXTRACT;
		if(module.equals("calculate")) 	this.module = MODULE_CALCULATE;
		if(module.equals("cluster"))	this.module = MODULE_CLUSTER;
	}
	
	// Argument variables
	String input1 = null, output = null; // universal
	boolean outExists = false;
	boolean seqNucl = true; // convert
	boolean multithread = false; // extract
	
	// binary paths
	String  path_prodigal = "prodigal",
			path_mmseqs   = "mmseqs",
			path_diamond  = "diamond",
			path_blastp   = "blastp",
			path_blastdb  = "makeblastdb",
			path_ufasta   = "ufasta";
	
	String label = null; // convert, extract
	String input2 = null, mtxout = null; int thread = 10; double identity = 0.4, coverage = 0.5; // calculate
	int program = PROGRAM_MMSEQS; // calculate
	boolean useid = false; // cluster
	
	private int parseArguments(String[] args) {
		String modstr = "";
		switch(module) {
		case MODULE_CONVERT: modstr = "convert"; break;
		case MODULE_EXTRACT: modstr = "extract"; break;
		case MODULE_CALCULATE: modstr = "calculate"; break;
		case MODULE_CLUSTER: modstr = "cluster"; break;
		}
		
		Arguments arg = new Arguments(args);
		if(arg.get("-i") == null) {
			Prompt.error("No input file given. Run with \""+modstr+" -h\" argument to get manual on this module.");
			return -1;
		}
		else{
			input1 = arg.get("-i");
			File fileInput = new File(input1);
			if(!fileInput.exists() || (fileInput.isDirectory() && module != MODULE_CALCULATE)) {
				Prompt.error("Invalid input file given.");
				return -1;
			}
		}
		if(arg.get("-o") == null) {
			Prompt.error("No output file given. Run with \""+modstr+" -h\" argument to get manual on this module.");
			return -1;
		}
		else {
			output = arg.get("-o");
			if((new File(output)).exists()) {
				outExists = true;
				if(module == MODULE_CALCULATE) Prompt.warning("Output file exists. Results will be appended.");
				else Prompt.warning("Output file exists. Results will be overwritten.");
			}
		}
		
		if(module == MODULE_CONVERT) {
			if(arg.get("-s") == null) {
				Prompt.error("No sequence type given. Run with \"convert -h\" argument to get manual on this module.");
				return -1;
			}
			else if(arg.get("-s").equals("prot")) seqNucl = false;
			else if(!arg.get("-s").equals("nucl")) {
				Prompt.error("Invalid sequence type given.");
				return -1;
			}
			if(arg.get("-l") == null) label = input1;
			else label = arg.get("-l");
			if(arg.get("-m") != null) path_mmseqs = arg.get("-m");
		}
		if(module == MODULE_EXTRACT) {
		//	if(arg.get("-p") != null) path_prodigal = arg.get("-p");
			if(arg.get("-l") == null) label = input1;
			else label = arg.get("-l");
			if(arg.get("-m") != null) path_mmseqs = arg.get("-m");
			if(arg.get("-t") != null) {
				thread = Integer.parseInt(arg.get("-t"));
				if(thread > 1) multithread = true;
			}
			else thread = 1;
		}
		if(module == MODULE_CALCULATE) {
			if(arg.get("-p") != null) {
				String pstr = arg.get("-p");
				switch (pstr) {
					case "mmseqs":
						program = PROGRAM_MMSEQS;
						break;
					case "diamond":
						program = PROGRAM_DIAMOND;
						break;
					case "blastp":
						program = PROGRAM_BLASTP;
						break;
					default:
						Prompt.error("Invalid program given.");
						return -1;
				}
			}
			if(arg.get("-j") == null) {
				Prompt.error("No secondary input file given. Run with \"calculate -h\" argument to get manual on this module.");
				return -1;
			}
			else{
				input2 = arg.get("-j");
				File fileInput = new File(input2);
				if(!fileInput.exists()) {
					Prompt.error("Invalid input file given.");
					return -1;
				}
			}
			if(arg.get("-id") != null) identity = Double.parseDouble(arg.get("-id"));
			if(arg.get("-cov") != null) coverage = Double.parseDouble(arg.get("-cov"));
			if(arg.get("-mtx") != null) mtxout = arg.get("-mtx");
			if(arg.get("-t") != null) thread = Integer.parseInt(arg.get("-t"));
		}
		if(module == MODULE_CLUSTER) {
			if(arg.get("-u") != null) useid = true;
		}
		
		if(arg.get("-prodigal") != null) path_prodigal = arg.get("-prodigal");
		if(arg.get("-mmseqs") != null) path_mmseqs = arg.get("-mmseqs");
		if(arg.get("-diamond") != null) path_diamond = arg.get("-diamond");
		if(arg.get("-blastp") != null) path_blastp = arg.get("-blastp");
		if(arg.get("-makeblastdb") != null) path_blastdb = arg.get("-makeblastdb");
		if(arg.get("-ufasta") != null) path_ufasta = arg.get("-ufasta");
		
		return 0;
	}
	
	private boolean checkProgram(int program) {
		boolean sane = true;
		
		switch(program) {
		case PROGRAM_MMSEQS:
			sane = Shell.exec(path_mmseqs + " -h")[0].contains("MMseqs2");
			break;
		case PROGRAM_DIAMOND:
			sane = Shell.exec(path_diamond + " help")[0].contains("diamond v");
			break;
		case PROGRAM_BLASTP:
			sane = Shell.exec(path_blastp + " -h")[1].contains("blastp");
			break;
		case PROGRAM_PRODIGAL:
			sane = Shell.exec(path_prodigal + " -h")[1].contains("prodigal");
			break;
		case PROGRAM_BLASTDB:
			sane = Shell.exec(path_blastdb + " -h")[1].contains("makeblastdb");
			break;
		case PROGRAM_UFASTA:
			sane = Shell.exec(path_ufasta + " -h")[0].contains("Usage");
		}

		return !sane;
	}
	private int checkDependency(int module, int program) {
		Prompt.talk("Checking dependencies...");
		
		switch(module) {
		case MODULE_CONVERT:
			if(checkProgram(PROGRAM_MMSEQS)) return PROGRAM_MMSEQS;
			break;
		case MODULE_EXTRACT:
			if(checkProgram(PROGRAM_PRODIGAL)) return PROGRAM_PRODIGAL;
			if(checkProgram(PROGRAM_MMSEQS)) return PROGRAM_MMSEQS;
			if(multithread) if(checkProgram(PROGRAM_UFASTA)) return PROGRAM_UFASTA;
			break;
		case MODULE_CALCULATE:
			if(program == PROGRAM_DIAMOND) if(checkProgram(PROGRAM_DIAMOND)) return PROGRAM_DIAMOND;
			if(program == PROGRAM_BLASTP) {
				if(checkProgram(PROGRAM_BLASTP)) return PROGRAM_BLASTP;
				if(checkProgram(PROGRAM_BLASTDB)) return PROGRAM_BLASTDB;
			}
			if(checkProgram(PROGRAM_MMSEQS)) return PROGRAM_MMSEQS;
			break;
		default:
			break;
		}
		
		return 0;
	}
	
//	@SuppressWarnings("unchecked")
	private int runConvert() {
		Prompt.debug("EzAAI - convert module");
		
		Prompt.print("Converting given CDS file into protein database... ("+input1+" -> "+output+")");
		String hex = Long.toHexString(new Random().nextLong());
		String faaPath = "/tmp/" + hex + ".faa";
		
		try {
			// copy input to temporary directory, translate if seq type is nucleotide
			if(seqNucl) Prompt.talk("Translating nucleotide sequences into protein sequences...");
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(faaPath));
			List<DnaSeqDomain> seqs = FastSeqLoader.importFileToDomainList(input1);
			for(DnaSeqDomain seq : seqs) {
				String title = seq.getTitle();
				String dna = seq.getSequence();
				String prot = seqNucl ? Seqtools.translate_CDS(dna, 11, true) : dna;
				bw.write(String.format(">%s # %d # %d\n%s\n", title.split("\\s+")[0], 1, prot.length()*3 + 3, prot));
			}
			bw.close();
			
			// create databases
			Shell.exec("mkdir /tmp/" + hex);
			ProcFuncAnnoByMMSeqs2 procMmseqs = new ProcFuncAnnoByMMSeqs2();
			procMmseqs.setMmseqsPath(path_mmseqs);
			procMmseqs.executeCreateDb(faaPath, "/tmp/" + hex + "/mm");
			
			// create label info file
			Prompt.debug("Writing file /tmp/" + hex + "/mm.label");
			bw = new BufferedWriter(new FileWriter("/tmp/" + hex + "/mm.label"));
			bw.write(label + "\n");
			bw.close();
			
			String[] names = {"mm", "mm.dbtype", "mm.index", "mm.lookup", "mm.source", "mm_h", "mm_h.dbtype", "mm_h.index", "mm.label"};
			
			// create .db file
			StringBuilder buf = new StringBuilder("tar -c -z -f " + "mm.tar.gz");
			for(String name : names) buf.append(" ").append(name);
			Shell.exec(buf.toString(), new File("/tmp/" + hex));
			Shell.exec("mv /tmp/" + hex + "/mm.tar.gz " + output);
			
			// remove temporary files
			for(String name : names) (new File("/tmp/" + hex + "/" + name)).delete();
			(new File("/tmp/" + hex)).delete();
			
			// tidy up
			(new File(faaPath)).delete();
			
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		Prompt.talk("EzAAI", "Conversion finished.");
		
		return 0;
	}
	
	private int runExtract() {
		Prompt.debug("EzAAI - extract module");
		String  gffFile = "/tmp/" + GenericConfig.SESSION_UID + ".gff",
				faaFile = input1 + ".faa",
				ffnFile = "/tmp/" + GenericConfig.SESSION_UID + ".ffn";
		
		try {
			Prompt.print("Running prodigal on genome " + input1 + "...");
			if(multithread) {
				ProcParallelProdigal procProdigal = new ProcParallelProdigal(input1, faaFile, "/tmp/", path_ufasta, path_prodigal, thread);
				if(procProdigal.run() < 0) return -1;
			}
			else {
				ProcCDSPredictionByProdigal procProdigal = new ProcCDSPredictionByProdigal();
				procProdigal.setProdigalPath(path_prodigal);
				procProdigal.setGffOutFileName(gffFile);
				procProdigal.setFaaOutFileName(faaFile);
				procProdigal.setFfnOutFileName(ffnFile);
				procProdigal.execute(input1, GenericConfig.DEV);
			}
			Prompt.talk("EzAAI", "Creating a submodule for converting .faa into .db...");
			EzAAI convertModule = new EzAAI("convert");
			String[] convertArgs = {"convert", "-i", faaFile, "-s", "prot", "-o", output, "-l", label, "-m", path_mmseqs};
			if(convertModule.run(convertArgs) < 0) return -1;
			
			(new File(gffFile)).delete();
			(new File(faaFile)).delete();
			(new File(ffnFile)).delete();
			
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		Prompt.print("Task finished.");
		return 0;
	}
	
	private int dbToFaa(String dbPath, String faaPath) {
		try {
			Shell.exec("tar -x -z -f " + dbPath);
			ProcFuncAnnoByMMSeqs2 procMmseqs = new ProcFuncAnnoByMMSeqs2();
			procMmseqs.setMmseqsPath(path_mmseqs);
			procMmseqs.executeConvert2Fasta("mm", faaPath);
			String[] struct = {"mm", "mm.dbtype", "mm.index", "mm.lookup", "mm.source", "mm_h", "mm_h.dbtype", "mm_h.index"};
			for(String str : struct) (new File(str)).delete();
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	private int runCalculate() {
		Prompt.debug("EzAAI - calculate module");
		
		try {
			// prepare profiles
			File ifile = new File(input1), jfile = new File(input2);
			String[] inames, jnames;
			if(ifile.isDirectory()) {
				String[] ls = ifile.list();
				assert ls != null;
				inames = new String[ls.length];
				for(int i = 0; i < ls.length; i++) {
					inames[i] = ifile.getAbsolutePath() + File.separator + ls[i];
				}
			}
			else {
				inames = new String[1];
				inames[0] = ifile.getAbsolutePath();
			}
			if(jfile.isDirectory()) {
				String[] ls = jfile.list();
				assert ls != null;
				jnames = new String[ls.length];
				for(int i = 0; i < ls.length; i++) {
					jnames[i] = jfile.getAbsolutePath() + File.separator + ls[i];
				}
			}
			else {
				jnames = new String[1];
				jnames[0] = jfile.getAbsolutePath();
			}
			
			// convert profiles into FASTA files
			List<String> ilist = new ArrayList<>(), jlist = new ArrayList<>();
			List<String> ilabs = new ArrayList<>(), jlabs = new ArrayList<>();
			File faaDir = new File("/tmp" + File.separator + GenericConfig.SESSION_UID + "_faa");
			if(!faaDir.exists()) faaDir.mkdir();
			else if(!faaDir.isDirectory()) {
				Prompt.error("Could not create temporary directory for FASTA files.");
				return -1;
			}
			
			for(int i = 0 ; i < inames.length; i++) {
				String faaPath = faaDir + File.separator + "i" + i + ".faa";
				if(dbToFaa(inames[i], faaPath) < 0) return -1;
				ilist.add(faaPath);
				
				BufferedReader br = new BufferedReader(new FileReader("mm.label"));
				ilabs.add(br.readLine());
				br.close();
				(new File("mm.label")).delete();
			}
			for(int j = 0 ; j < jnames.length; j++) {
				String faaPath = faaDir + File.separator + "j" + j + ".faa";
				if(dbToFaa(jnames[j], faaPath) < 0) return -1;
				jlist.add(faaPath);
				
				BufferedReader br = new BufferedReader(new FileReader("mm.label"));
				jlabs.add(br.readLine());
				br.close();
				(new File("mm.label")).delete();
			}
			
			// run MMSeqs for each fasta pairs
			Double[][] aaiTable = new Double[ilist.size()][jlist.size()];
			Integer[][] hitTable = new Integer[ilist.size()][jlist.size()];
			Integer[] ilens = new Integer[ilist.size()], jlens = new Integer[jlist.size()];
			
			for(int i = 0; i < ilist.size(); i++) {
				for(int j = 0; j < jlist.size(); j++) {
					Prompt.print(String.format("Calculating AAI... [Task %d/%d]", i*jlist.size()+j+1, ilist.size()*jlist.size()));
					ProcCalcPairwiseAAI procAAI = new ProcCalcPairwiseAAI();
					
					switch(program) {
					case PROGRAM_MMSEQS: 
						procAAI.setPath(path_mmseqs);
						procAAI.setMode(ProcCalcPairwiseAAI.MODE_MMSEQS);
						break;
					case PROGRAM_DIAMOND:
						procAAI.setPath(path_diamond);
						procAAI.setMode(ProcCalcPairwiseAAI.MODE_DSENS);
						break;
					case PROGRAM_BLASTP:
						procAAI.setPath(path_blastp);
						procAAI.setDbpath(path_blastdb);
						procAAI.setMode(ProcCalcPairwiseAAI.MODE_BLASTP);
						break;
					}
					procAAI.setNthread(thread);
					procAAI.setIdentity(identity);
					procAAI.setCoverage(coverage);
					List<String> res = procAAI.calculateProteomePairWithDetails(ilist.get(i), jlist.get(j));
					hitTable[i][j] = Integer.parseInt(res.get(4));
					aaiTable[i][j] = Double.parseDouble(res.get(6));
					if(j == 0) ilens[i] = Integer.parseInt(res.get(0));
					if(i == 0) jlens[j] = Integer.parseInt(res.get(1));
				}
			}
			
			// print result into output file
			BufferedWriter bw = new BufferedWriter(new FileWriter(output, outExists));
			if(!outExists) bw.write("ID 1\tID 2\tLabel 1\tLabel 2\tAAI\tCDS count 1\tCDS count 2\tMatched count\tProteome cov.\tID param.\tCov. param.\n");
			for(int i = 0; i < ilist.size(); i++) {
				for(int j = 0; j < jlist.size(); j++) {
					bw.write(String.format("%d\t%d\t%s\t%s\t%f\t%d\t%d\t%d\t%f\t%f\t%f\n",
							Math.abs(ilabs.get(i).hashCode()) % (1<<30), Math.abs(jlabs.get(j).hashCode()) % (1<<30), 
							ilabs.get(i), jlabs.get(j), aaiTable[i][j], ilens[i], jlens[j], hitTable[i][j], (double) hitTable[i][j] * 2 / (ilens[i] + jlens[j]), identity, coverage));
				}
			}
			bw.close();
			
			// print matrix output
			if(mtxout != null) {
				BufferedWriter mw = new BufferedWriter(new FileWriter(mtxout));
				mw.write("%%MatrixMarket matrix array real general\n");
				mw.write(String.format("%d %d\n", ilist.size(), jlist.size()));
				for(int j = 0; j < jlist.size(); j++) {
					for(int i = 0; i < ilist.size(); i++) {
						mw.write(String.format("%f\n", aaiTable[i][j]));
					}
				}
				mw.close();
			}
			// remove directory
			FileUtils.deleteDirectory(faaDir);
			
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		Prompt.print("Task finished.");
		return 0;
	}
	
	private int runCluster() {
		Prompt.debug("EzAAI - cluster module");
		
		// parse input file
		Map<Integer, Integer> imap = new HashMap<>();
		List<Integer> ids = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		List<String> bufs = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(input1));
			br.readLine(); // discard header
			String buf;
			while((buf = br.readLine()) != null) {
				bufs.add(buf);
				int id1 = Integer.parseInt(buf.split("\t")[0]), id2 = Integer.parseInt(buf.split("\t")[1]);
				String lab1 = buf.split("\t")[2], lab2 = buf.split("\t")[3];
				if(!imap.containsKey(id1)) {
					imap.put(id1, labels.size());
					ids.add(id1);
					labels.add(lab1);
				}
				if(!imap.containsKey(id2)) {
					imap.put(id2, labels.size());
					ids.add(id2);
					labels.add(lab2);
				}
			}
			br.close();
		} catch(Exception e) {
			Prompt.error("Given file is incompatible. Input file must be the output of EzAAI calculate module.");
			return -1;
		}
		
		try {
			// fill distance matrix in
			double[][] dmat = new double[labels.size()][labels.size()];
			for(int i = 0; i < labels.size(); i++) {
				for(int j = 0; j < labels.size(); j++) {
					if(i - j == 0) dmat[i][j] = 0.0;
					else dmat[i][j] = -1.0;
				}
			}
				
			for(String buf : bufs) {
				int i = imap.get(Integer.parseInt(buf.split("\t")[0])), j = imap.get(Integer.parseInt(buf.split("\t")[1]));
				dmat[i][j] = 100.0 - Double.parseDouble(buf.split("\t")[4]);
			}
			
			// check completeness
			boolean complete = true;
			for(int i = 0; i < labels.size()-1; i++) {
				if(dmat[i][i] > 0) {
					complete = false;
					Prompt.talk(String.format("INCOMPLETE DATA - Positive distance to iteslf: [%s]", labels.get(i)));
				}
				for(int j = i+1; j < labels.size(); j++) {
					if(dmat[i][j] < 0) {
						complete = false;
						Prompt.talk(String.format("INCOMPLETE DATA - AAI value missing: [%s] vs. [%s]", labels.get(i), labels.get(j)));
					}
					else if(dmat[j][i] < 0) dmat[j][i] = dmat[i][j];
				}
			}
			
			if(!complete) {
				Prompt.error("Given values are incomplete. File should contain all-by-all pairwise AAI values from a group of taxa.");
				Prompt.error("To see the detailed error log, run the identical script with -v option.");
				return -1;
			}
			
			Prompt.print("AAI matrix identified. Running hierarchical clustering with UPGMA method...");
			
			// produce UPGMA tree
			ProcUPGMA upgma = new ProcUPGMA(dmat, ids, labels, useid);
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			bw.write(upgma.getTree() + "\n");
			bw.close();
			
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		Prompt.print("Task finished.");
		return 0;
	}
	
	private int run(String[] args) {
		if(parseArguments(args) < 0) return -1;
		
		switch(checkDependency(module, program)) {
		case PROGRAM_MMSEQS:
			Prompt.error("Failed to resolve MMSeqs2 binary. Please check the given path: " + ANSIHandler.wrapper(path_mmseqs, 'g')); return -1;
		case PROGRAM_DIAMOND:
			Prompt.error("Failed to resolve DIAMOND binary. Please check the given path: " + ANSIHandler.wrapper(path_diamond, 'g')); return -1;
		case PROGRAM_BLASTP:
			Prompt.error("Failed to resolve BLASTp+ binary. Please check the given path: " + ANSIHandler.wrapper(path_blastp, 'g')); return -1;
		case PROGRAM_PRODIGAL:
			Prompt.error("Failed to resolve Prodigal binary. Please check the given path: " + ANSIHandler.wrapper(path_prodigal, 'g')); return -1;
		case PROGRAM_BLASTDB:
			Prompt.error("Failed to resolve makeblastdb binary. Please check the given path: " + ANSIHandler.wrapper(path_blastdb, 'g')); return -1;
		case PROGRAM_UFASTA:
			Prompt.error("Failed to resolve ufasta binary. Multi-thread extraction requires ufasta binary.");
			Prompt.error("ufasta is available at: https://github.com/gmarcais/ufasta"); return -1;
		default: break;
		}
		
		switch(module) {
		case MODULE_CONVERT: 	return runConvert();
		case MODULE_EXTRACT: 	return runExtract();
		case MODULE_CALCULATE: 	return runCalculate();
		case MODULE_CLUSTER:	return runCluster();
		default: return -1;
		}
	}
	
	public static void main(String[] args) {
		if(args.length == 0) {
			printHelp(0);
			return;
		}
		
		Arguments arg = new Arguments(args);
		if(arg.get("-v") != null) GenericConfig.VERB = true;
		if(arg.get("-dev") != null) {
			GenericConfig.VERB = true;
			GenericConfig.DEV = true;
		}
		if(arg.get("-nc") != null) GenericConfig.NOCOLOR = true;
		GenericConfig.TSTAMP = arg.get("-nt") == null;
		
		EzAAI ezAAI = new EzAAI(args[0]);
		if(arg.get("-h") != null) {
			printHelp(ezAAI.module);
			return;
		}
		else if(ezAAI.module == MODULE_INVALID) {
			Prompt.error("Invalid module given. Use -h option to get help.");
			return;
		}
		
		GenericConfig.setHeaderLength(7);
		GenericConfig.setHeader("EzAAI");
		Prompt.print(String.format("EzAAI - %s [%s]", VERSION, RELEASE));
		if(ezAAI.run(args) < 0) {
			Prompt.error("Program terminated with error.");
		}
	}
	
	private static void printHelp(int module) {
		if(module == MODULE_INVALID) {
			System.out.println(ANSIHandler.wrapper(String.format("\n EzAAI - %s [%s]", VERSION, RELEASE), 'G'));
			System.out.println(ANSIHandler.wrapper(" High Throughput Prokaryotic Average Amino acid Identity Calculator", 'g'));
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n Please cite:\n", 'C') + CITATION);
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar <module> [<args>]");
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n Available modules", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Module\t\tDescription", 'c'));
			System.out.printf(" %s\t%s%n", "extract",   "Extract protein DB from genome using Prodigal");
			System.out.printf(" %s\t%s%n", "convert",   "Convert CDS FASTA file into protein DB");
			System.out.printf(" %s\t%s%n", "calculate", "Calculate AAI value from protein databases using MMSeqs2");
			System.out.printf(" %s\t%s%n", "cluster",   "Hierarchical clustering of taxa with AAI values");
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n Miscellaneous", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-nc", "No-color mode");
			System.out.printf(" %s\t\t%s%n", "-nt", "No time stamps");
			System.out.printf(" %s\t\t%s%n", "-v",  "Go verbose");
			System.out.printf(" %s\t\t%s%n", "-h",  "Print help");
			System.out.println();
		}
		if(module == MODULE_EXTRACT) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - extract", 'G'));
			System.out.println(ANSIHandler.wrapper(" Extract protein DB from prokaryotic genome sequence using Prodigal", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar extract -i <IN_SEQ> -o <OUT_DB> [-l <LABEL> -t <THREAD>]");
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-i", "Input prokaryotic genome sequence");
			System.out.printf(" %s\t\t%s%n", "-o", "Output protein database");
			
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-l", "Taxonomic label for phylogenetic tree");
			System.out.printf(" %s\t\t%s%n", "-t", "Number of CPU threads - multi-threading requires ufasta (default: 1)");
			//System.out.println(String.format(" %s\t\t%s", "  ", "https://github.com/gmarcais/ufasta"));
			System.out.printf(" %s\t%s%n", "-prodigal", "Custom path to prodigal binary (default: prodigal)");
			System.out.printf(" %s\t%s%n", "-mmseqs", "Custom path to MMSeqs2 binary (default: mmseqs)");
			System.out.printf(" %s\t%s%n", "-ufasta", "Custom path to ufasta binary (default: ufasta)");
			System.out.println();
		}
		if(module == MODULE_CONVERT) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - convert", 'G'));
			System.out.println(ANSIHandler.wrapper(" Convert CDS FASTA file into protein DB", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar convert -i <IN_CDS> -s <SEQ_TYPE> -o <OUT_DB> [-l <LABEL>]");
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-i", "Input CDS file (FASTA format)");
			System.out.printf(" %s\t\t%s%n", "-s", "Sequence type of input file (nucl/prot)");
			System.out.printf(" %s\t\t%s%n", "-o", "Output protein DB");
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-l", "Taxonomic label for phylogenetic tree");
			System.out.printf(" %s\t%s%n", "-mmseqs", "Custom path to MMSeqs2 binary (default: mmseqs)");
			System.out.println();
		}
		if(module == MODULE_CALCULATE) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - calculate", 'G'));
			System.out.println(ANSIHandler.wrapper(" Calculate AAI value from protein databases", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar calculate -i <INPUT_1> -j <INPUT_2> -o <OUTPUT> [-p <PROGRAM> -t <THREAD> -id <IDENTITY> -cov <COVERAGE> -mtx <MTX_OUTPUT>]");
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-i", "First input protein DB / directory with protein DBs");
			System.out.printf(" %s\t\t%s%n", "-j", "Second input protein DB / directory with protein DBs");
			System.out.printf(" %s\t\t%s%n", "-o",  "Output result file");
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-p", "Customize calculation program [mmseqs / diamond / blastp] (default: mmseqs)");
			System.out.printf(" %s\t\t%s%n", "-t", "Number of CPU threads to use (default: 10)");
			System.out.printf(" %s\t\t%s%n", "-id", "Minimum identity threshold for AAI calculations [0 - 1.0] (default: 0.4)");
			System.out.printf(" %s\t\t%s%n", "-cov", "Minimum query coverage threshold for AAI calculations [0 - 1.0] (default: 0.5)");
			System.out.printf(" %s\t\t%s%n", "-mtx", "Matrix Market formatted output");
			System.out.printf(" %s\t%s%n", "-mmseqs", "Custom path to MMSeqs2 binary (default: mmseqs)");
			System.out.printf(" %s\t%s%n", "-diamond", "Custom path to DIAMOND binary (default: diamond)");
			System.out.printf(" %s\t%s%n", "-blastp", "Custom path to BLASTp+ binary (default: blastp)");
			System.out.printf(" %s\t%s%n", "-makeblastdb", "Custom path to makeblastdb binary (default: makeblastdb)");
			System.out.println();
		}
		if(module == MODULE_CLUSTER) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - cluster", 'G'));
			System.out.println(ANSIHandler.wrapper(" Hierarchical clustering of taxa with AAI values", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar cluster -i <AAI_TABLE> -o <OUTPUT>");
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.printf(" %s\t\t%s%n", "-i", "Input EzAAI result file containing all-by-all pairwise AAI values");
			System.out.printf(" %s\t\t%s%n", "-o",  "Output result file");
			System.out.printf(" %s\t\t%s%n", "-u",  "Use ID instead of label for tree");
			System.out.println();
		}
	}
}
