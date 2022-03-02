package leb.main;

import leb.process.ProcCalcPairwiseAAI;
import leb.process.ProcFuncAnnoByMMSeqs2;
import leb.process.ProcUPGMA;
import leb.process.ProcCDSPredictionByProdigal;
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
	public static final String VERSION  = "v1.2",
							   RELEASE  = "Mar. 2022",
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
					 PROGRAM_BLASTP		= 3;
	
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
	String path_prodigal = "prodigal"; // extract
	String label = null; // convert, extract
	String input2 = null, path_mmseqs = "mmseqs", path_diamond = "diamond", path_blastp = "blastp", mtxout = null; int thread = 10; double identity = 0.4, coverage = 0.5; // calculate
	int program = PROGRAM_MMSEQS; // calculate
	
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
			if(arg.get("-p") != null) path_prodigal = arg.get("-p");
			if(arg.get("-l") == null) label = input1;
			else label = arg.get("-l");
			if(arg.get("-m") != null) path_mmseqs = arg.get("-m");
		}
		if(module == MODULE_CALCULATE) {
			if(arg.get("-p") != null) {
				String pstr = arg.get("-p");
				if(pstr.equals("mmseqs")) program = PROGRAM_MMSEQS;
				else if(pstr.equals("diamond")) program = PROGRAM_DIAMOND;
				else if(pstr.equals("blastp")) program = PROGRAM_BLASTP;
				else {
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
			if(arg.get("-mmseqs") != null) path_mmseqs = arg.get("-mmseqs");
			if(arg.get("-diamond") != null) path_diamond = arg.get("-diamond");
			if(arg.get("-blastp") != null) path_blastp = arg.get("-blastp");
		}
		return 0;
	}	
	
//	@SuppressWarnings("unchecked")
	private int runConvert(String[] args) {
		Prompt.debug("EzAAI - convert module");
		if(parseArguments(args) < 0) return -1;
		
		Prompt.print("Converting given CDS file into profile database... ("+input1+" -> "+output+")");
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
			
			// create database
			ProcFuncAnnoByMMSeqs2 procMmseqs = new ProcFuncAnnoByMMSeqs2();
			procMmseqs.setMmseqsPath(path_mmseqs);
			procMmseqs.executeCreateDb(faaPath, "/tmp/mm");
			
			// create label info file
			Prompt.debug("Writing file /tmp/mm.label");
			bw = new BufferedWriter(new FileWriter("/tmp/mm.label"));
			bw.write(label + "\n");
			bw.close();
			
			String[] names = {"mm", "mm.dbtype", "mm.index", "mm.lookup", "mm.source", "mm_h", "mm_h.dbtype", "mm_h.index", "mm.label"};
			
			// create .db file
			String buf = "tar -c -z -f /tmp/" + hex + ".db";
			for(String name : names) buf += "/tmp/" + name + " ";
			Shell.exec(buf);
			
			// remove /tmp/mm.*
			buf = "rm ";
			for(String name : names) buf += "/tmp/" + name + " ";
			Shell.exec(buf);
			
			Shell.exec("mv /tmp/" + hex + ".db " + output);
			
			// tidy up
			(new File(faaPath)).delete();
			
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		Prompt.talk("EzAAI", "Conversion finished.");
		
		return 0;
	}
	
	private int runExtract(String[] args) {
		Prompt.debug("EzAAI - extract module");
		if(parseArguments(args) < 0) return -1;
		
		try {
			Prompt.print("Running prodigal on genome " + input1 + "...");
			ProcCDSPredictionByProdigal procProdigal = new ProcCDSPredictionByProdigal();
			procProdigal.setProdigalPath(path_prodigal);
			procProdigal.setGffOutFileName("/tmp/" + GenericConfig.SESSION_UID + ".gff");
			procProdigal.setFaaOutFileName(input1 + ".faa");
			procProdigal.setFfnOutFileName("/tmp/" + GenericConfig.SESSION_UID + ".ffn");
			procProdigal.execute(input1, GenericConfig.DEV);
			
			Prompt.talk("EzAAI", "Creating a submodule for converting .faa into .db profile...");
			EzAAI convertModule = new EzAAI("convert");
			String[] convertArgs = {"convert", "-i", procProdigal.getFaaOutFileName(), "-s", "prot", "-o", output, "-l", label, "-m", path_mmseqs};
			if(convertModule.run(convertArgs) < 0) return -1;
			
			(new File(procProdigal.getGffOutFileName())).delete();
			(new File(procProdigal.getFaaOutFileName())).delete();
			(new File(procProdigal.getFfnOutFileName())).delete();
			
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
	
	private int runCalculate(String[] args) {
		Prompt.debug("EzAAI - calculate module");
		if(parseArguments(args) < 0) return -1;
		
		try {
			// prepare profiles
			File ifile = new File(input1), jfile = new File(input2);
			String[] inames, jnames;
			if(ifile.isDirectory()) {
				String[] ls = ifile.list();
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
				String faaPath = faaDir + File.separator + "i" + String.valueOf(i) + ".faa";
				if(dbToFaa(inames[i], faaPath) < 0) return -1;
				ilist.add(faaPath);
				
				BufferedReader br = new BufferedReader(new FileReader("mm.label"));
				ilabs.add(br.readLine());
				br.close();
				(new File("mm.label")).delete();
			}
			for(int j = 0 ; j < jnames.length; j++) {
				String faaPath = faaDir + File.separator + "j" + String.valueOf(j) + ".faa";
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
	
	private int runCluster(String[] args) {
		Prompt.debug("EzAAI - cluster module");
		parseArguments(args);
		
		// parse input file
		Map<Integer, Integer> imap = new HashMap<Integer, Integer>();
		List<String> labels = new ArrayList<String>();
		List<String> bufs = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(input1));
			String buf = br.readLine(); // discard header
			while((buf = br.readLine()) != null) {
				bufs.add(buf);
				int id1 = Integer.parseInt(buf.split("\t")[0]), id2 = Integer.parseInt(buf.split("\t")[1]);
				String lab1 = buf.split("\t")[2], lab2 = buf.split("\t")[3];
				if(!imap.containsKey(id1)) {
					imap.put(id1, labels.size());
					labels.add(lab1);
				}
				if(!imap.containsKey(id2)) {
					imap.put(id2, labels.size());
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
			ProcUPGMA upgma = new ProcUPGMA(dmat, labels);
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
		switch(module) {
		case MODULE_CONVERT: 	return runConvert(args);
		case MODULE_EXTRACT: 	return runExtract(args);
		case MODULE_CALCULATE: 	return runCalculate(args);
		case MODULE_CLUSTER:	return runCluster(args);
		default: return -1;
		}
	}
	
	public static void main(String[] args) throws Exception {
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
		if(arg.get("-nt") != null) GenericConfig.TSTAMP = false;
		else GenericConfig.TSTAMP = true;
		
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
			return;
		}
	}
	
	private static void printHelp(int module) {
		if(module == MODULE_INVALID) {
			System.out.println(ANSIHandler.wrapper(String.format("\n EzAAI - %s [%s]", VERSION, RELEASE), 'G'));
			System.out.println(ANSIHandler.wrapper(" High Throughput Prokaryotic Average Amino acid Identity Calculator", 'g'));
			System.out.println("");
			
			System.out.println(ANSIHandler.wrapper("\n Please cite:\n", 'C') + CITATION);
			System.out.println("");
			
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar <module> [<args>]");
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n Available modules", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Module\t\tDescription", 'c'));
			System.out.println(String.format(" %s\t%s", "extract",   "Extract profile DB from genome using Prodigal"));
			System.out.println(String.format(" %s\t%s", "convert",   "Convert CDS FASTA file into profile DB"));
			System.out.println(String.format(" %s\t%s", "calculate", "Calculate AAI value from profile databases using MMSeqs2"));
			System.out.println(String.format(" %s\t%s", "cluster",   "Hierarchical clustering of taxa with AAI values"));
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n Miscellaneous", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-nc", "No-color mode"));
			System.out.println(String.format(" %s\t\t%s", "-nt", "No time stamps"));
			System.out.println(String.format(" %s\t\t%s", "-v",  "Go verbose"));
			System.out.println(String.format(" %s\t\t%s", "-h",  "Print help"));
			System.out.println("");
		}
		if(module == MODULE_EXTRACT) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - extract", 'G'));
			System.out.println(ANSIHandler.wrapper(" Extract profile DB from prokaryotic genome sequence using Prodigal", 'g'));
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar extract -i <IN_SEQ> -o <OUT_DB> [-l <LABEL> -p <PRODIGAL_PATH> -m <MMSEQS_PATH>]");
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-i", "Input prokaryotic genome sequence"));
			System.out.println(String.format(" %s\t\t%s", "-o", "Output profile database"));
			System.out.println("");
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-l", "Taxonomic label for phylogenetic tree"));
			System.out.println(String.format(" %s\t\t%s", "-p", "Custom path to prodigal binary"));
			System.out.println(String.format(" %s\t\t%s", "-m", "Custom path to MMSeqs2 binary"));
			System.out.println("");
		}
		if(module == MODULE_CONVERT) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - convert", 'G'));
			System.out.println(ANSIHandler.wrapper(" Convert CDS FASTA file into profile DB", 'g'));
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar convert -i <IN_CDS> -s <SEQ_TYPE> -o <OUT_DB> [-l <LABEL>]");
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-i", "Input CDS profile (FASTA format)"));
			System.out.println(String.format(" %s\t\t%s", "-s", "Sequence type of input file (nucl/prot)"));
			System.out.println(String.format(" %s\t\t%s", "-o", "Output profile DB"));
			System.out.println("");
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-l", "Taxonomic label for phylogenetic tree"));
			System.out.println(String.format(" %s\t\t%s", "-m", "Custom path to MMSeqs2 binary"));
			System.out.println("");
		}
		if(module == MODULE_CALCULATE) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - calculate", 'G'));
			System.out.println(ANSIHandler.wrapper(" Calculate AAI value from profile databases", 'g'));
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar calculate -i <INPUT_1> -j <INPUT_2> -o <OUTPUT> [-p <PROGRAM> -id <IDENTITY> -cov <COVERAGE> -mtx <MTX_OUTPUT> -t <THREAD> -bin <PATH>]");
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-i", "First input profile DB / directory with profile DBs"));
			System.out.println(String.format(" %s\t\t%s", "-j", "Second input profile DB / directory with profile DBs"));
			System.out.println(String.format(" %s\t\t%s", "-o",  "Output result file"));
			System.out.println("");
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-p", "Customize calculation program [mmseqs / diamond / blastp] (default: mmseqs)"));
			System.out.println(String.format(" %s\t\t%s", "-id", "Minimum identity threshold for AAI calculations [0 - 1.0] (default: 0.4)"));
			System.out.println(String.format(" %s\t\t%s", "-cov", "Minimum query coverage threshold for AAI calculations [0 - 1.0] (default: 0.5)"));
			System.out.println(String.format(" %s\t\t%s", "-mtx", "Matrix Market formatted output"));
			System.out.println(String.format(" %s\t\t%s", "-t", "Number of CPU threads to use (default: 10)"));
			System.out.println(String.format(" %s\t%s", "-mmseqs", "Custom path to MMSeqs2 binary (default: mmseqs)"));
			System.out.println(String.format(" %s\t%s", "-diamond", "Custom path to DIAMOND binary (default: diamond)"));
			System.out.println(String.format(" %s\t%s", "-blastp", "Custom path to BLASTp+ binary (default: blastp)"));
			System.out.println("");
		}
		if(module == MODULE_CLUSTER) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - cluster", 'G'));
			System.out.println(ANSIHandler.wrapper(" Hierarchical clustering of taxa with AAI values", 'g'));
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " java -jar EzAAI.jar cluster -i <AAI_TABLE> -o <OUTPUT>");
			System.out.println("");
		
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(" Argument\tDescription", 'c'));
			System.out.println(String.format(" %s\t\t%s", "-i", "Input EzAAI result file containing all-by-all pairwise AAI values"));
			System.out.println(String.format(" %s\t\t%s", "-o",  "Output result file"));
			System.out.println("");
		}
	}
}
