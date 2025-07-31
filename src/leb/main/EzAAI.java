package leb.main;

import leb.process.ProcCalcPairwiseAAI;
import leb.process.ProcFuncAnnoByMMSeqs2;
import leb.process.ProcUPGMA;
import leb.process.ProcCDSPredictionByProdigal;
import leb.process.ProcParallelProdigal;
import leb.util.common.ANSIHandler;
import leb.util.common.Arguments;
import leb.util.common.FileRemover;
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
import java.io.IOException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EzAAI {
	public static final String VERSION  = "v1.2.4",
							   RELEASE  = "Jul. 2025",
							   CITATION = " Kim, D., Park, S. & Chun, J.\n"
							   			+ " Introducing EzAAI: a pipeline for high throughput calculations of prokaryotic average amino acid identity.\n"
							   			+ " J Microbiol. 59, 476–480 (2021).\n"
							   			+ " DOI: 10.1007/s12275-021-1154-0";
	public static final boolean STABLE  = true;
	
	final static int MODULE_CONVERT 	= 1,
					 MODULE_EXTRACT 	= 2,
					 MODULE_CALCULATE 	= 3,
					 MODULE_CLUSTER		= 4,
					 MODULE_CONVERTDB	= 5,
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
		if(module.equals("convertdb"))	this.module = MODULE_CONVERTDB;
	}
	
	// Argument variables
	String input1 = null, output = null, tmp = "/tmp/ezaai"; // universal
	boolean outExists = false;
	boolean seqNucl = true; // convert
	boolean multithread = false, batchExtract = false; // extract

	// binary paths
	String  path_prodigal = "prodigal",
			path_mmseqs   = "mmseqs",
			path_diamond  = "diamond",
			path_blastp   = "blastp",
			path_blastdb  = "makeblastdb",
			path_ufasta   = "ufasta";
	
	String label = null; // convert, extract
	String input2 = null, matchout = null, mtxout = null; int thread = 10; double identity = 0.4, coverage = 0.5; // calculate
	boolean self = false; // calculate
	int program = PROGRAM_MMSEQS; // calculate
	boolean useid = false; // cluster
	
	private int parseArguments(String[] args) {
		String modstr = "";
		switch(module) {
		case MODULE_CONVERT: modstr = "convert"; break;
		case MODULE_EXTRACT: modstr = "extract"; break;
		case MODULE_CALCULATE: modstr = "calculate"; break;
		case MODULE_CLUSTER: modstr = "cluster"; break;
		case MODULE_CONVERTDB: modstr = "convertdb"; break;
		}
		
		Arguments arg = new Arguments(args);
		if(arg.get("-i") == null) {
			Prompt.error("No input file given. Run with \""+modstr+" -h\" argument to get manual on this module.");
			return -1;
		}
		else{
			input1 = arg.get("-i");
			File fileInput = new File(input1);
			if(!fileInput.exists() || (fileInput.isDirectory() && !(module == MODULE_EXTRACT || module == MODULE_CALCULATE))) {
				Prompt.error("Invalid input file given.");
				return -1;
			}
			if(fileInput.isDirectory() && module == MODULE_EXTRACT) {
				Prompt.talk("Input is a directory. Running in batch mode.");
				batchExtract = true;
			}
		}
		if(arg.get("-o") == null) {
			Prompt.error("No output file given. Run with \""+modstr+" -h\" argument to get manual on this module.");
			return -1;
		}
		else {
			output = arg.get("-o");
			if((new File(output)).exists()) {
				if((new File(output)).isDirectory()) {
					if(!batchExtract) {
						Prompt.error("Given output file exists and is a directory: " + output);
						return -1;
					}
				}
				outExists = true;
				if(module == MODULE_CALCULATE) Prompt.warning("Output file "+output+" exists. Results will be appended.");
				else if(!batchExtract) Prompt.warning("Output "+output+" file exists. Results will be overwritten.");
				else Prompt.warning("Output directory "+output+" exists. Results will be written to this directory.");
			} else if(batchExtract) {
				if(new File(output).mkdirs()) {
					Prompt.talk("Created output directory: " + output);
				} else {
					Prompt.error("Failed to create output directory: " + output);
					return -1;
				}
			}
			// check output directory is writable
			if((new File(output)).isDirectory()) {
				if(!(new File(output)).canWrite()) {
					Prompt.error("Output directory is not writable: " + output);
					return -1;
				}
			} else {
				String path = new File(output).getAbsolutePath();
				String parent = path.substring(0, path.lastIndexOf(File.separator));
				if(!(new File(parent)).exists()) {
					if(!(new File(parent)).mkdirs()) {
						Prompt.error("Failed to create parent directory for the output file: " + parent);
						return -1;
					} else {
						Prompt.talk("Created parent directory for the output file: " + parent);
					}
				}
				if(!(new File(parent)).canWrite()) {
					Prompt.error("Output file is allocated to a non-writable directory: " + parent);
					return -1;
				}
			}
		}
		if(arg.get("-tmp") != null) tmp = arg.get("-tmp");
		if((new File(tmp)).exists()) {
			if(!(new File(tmp)).isDirectory()) {
				Prompt.error("Invalid temporary directory given: " + tmp);
				return -1;
			}
			else Prompt.talk("Using existing temporary directory: " + tmp);
		}
		else {
			if((new File(tmp)).mkdirs()) Prompt.talk("Created temporary directory: " + tmp);
			else {
				Prompt.error("Failed to create temporary directory: " + tmp);
				return -1;
			}
		}
		if(!(new File(tmp)).canWrite()) {
			Prompt.error("Temporary directory is not writable: " + tmp);
			Prompt.error("Please provide a writable directory with -tmp argument.");
			return -1;
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
			// check if input is a directory, if so, set to extract in batch mode
			if(arg.get("-l") == null) label = batchExtract ? null : input1;
			else label = arg.get("-l");
			if(arg.get("-m") != null) path_mmseqs = arg.get("-m");
			if(arg.get("-t") != null) {
				thread = Integer.parseInt(arg.get("-t"));
				if(thread > 1 && !batchExtract) multithread = true;
			}
			else thread = batchExtract ? 10 : 1;
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
			if(arg.get("-self") != null) {
				self = Integer.parseInt(arg.get("-self")) != 0;
				if(self && !input1.equals(input2)) {
					Prompt.error("Self-comparison with different input files is not allowed.");
					return -1;
				}
			}
			if(arg.get("-id") != null) identity = Double.parseDouble(arg.get("-id"));
			if(arg.get("-cov") != null) coverage = Double.parseDouble(arg.get("-cov"));
			if(arg.get("-match") != null) matchout = arg.get("-match");
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
		case MODULE_CONVERT: case MODULE_CONVERTDB:
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
		String faaPath = tmp + File.separator + hex + ".faa";
		
		try {
			// copy input to temporary directory, translate if seq type is nucleotide
			if(seqNucl) Prompt.talk("Translating nucleotide sequences into protein sequences...");
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(faaPath));
			List<DnaSeqDomain> seqs = FastSeqLoader.importFileToDomainList(input1);
			for(DnaSeqDomain seq : seqs) {
				String title = seq.getTitle();
				String dna = seq.getSequence();
				String prot = seqNucl ? Seqtools.translate_CDS(dna, 11, true) : dna;
				bw.write(String.format(">ezaai_%s # %d # %d\n%s\n", title.split("\\s+")[0], 1, prot.length()*3 + 3, prot));
			}
			bw.close();
			
			// create databases
			String dir = tmp + File.separator + hex;
			if(!(new File(dir)).mkdirs()) {
				Prompt.error("Failed to create temporary directory: " + dir);
				return -1;
			}
			ProcFuncAnnoByMMSeqs2 procMmseqs = new ProcFuncAnnoByMMSeqs2();
			procMmseqs.setMmseqsPath(path_mmseqs);
			procMmseqs.executeCreateDb(faaPath, dir + File.separator + "mm");
			
			// create label info file
			Prompt.debug("Writing file mm.label");
			bw = new BufferedWriter(new FileWriter(dir + File.separator + "mm.label"));
			bw.write(label + "\n");
			bw.close();
			
			String[] names = {"mm", "mm.dbtype", "mm.index", "mm.lookup", "mm.source", "mm_h", "mm_h.dbtype", "mm_h.index", "mm.label"};
			
			// create .db file
			StringBuilder buf = new StringBuilder("tar -c -z -f " + "mm.tar.gz");
			for(String name : names) buf.append(" ").append(name);
			Shell.exec(buf.toString(), new File(dir));
			Shell.exec("mv " + dir + File.separator + "mm.tar.gz " + output);
			
			// remove files
			FileRemover.safeDeleteDirectory(dir);
			FileRemover.safeDelete(faaPath);
			
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		Prompt.talk("EzAAI", "Conversion finished.");
		
		return 0;
	}
	
	private int runExtract() {
		Prompt.debug("EzAAI - extract module");
		if(batchExtract) return runExtractBatch();

		String  gffFile = tmp + File.separator + GenericConfig.SESSION_UID + ".gff",
				faaFile = input1 + ".faa",
				ffnFile = tmp + File.separator + GenericConfig.SESSION_UID + ".ffn";
		
		try {
			Prompt.print("Running prodigal on genome " + input1 + "...");
			String command = null;
			if(multithread) {
				ProcParallelProdigal procProdigal = new ProcParallelProdigal(input1, faaFile, tmp + File.separator, path_ufasta, path_prodigal, thread);
				if(procProdigal.run() < 0) return -1;
			}
			else {
				ProcCDSPredictionByProdigal procProdigal = new ProcCDSPredictionByProdigal();
				procProdigal.setOutDir(tmp + File.separator);
				procProdigal.setProdigalPath(path_prodigal);
				procProdigal.setGffOutFileName(gffFile);
				procProdigal.setFaaOutFileName(faaFile);
				procProdigal.setFfnOutFileName(ffnFile);
				procProdigal.execute(input1, GenericConfig.DEV);
				command = procProdigal.getCommand();
			}

			// check prodigal output
			if(!(new File(faaFile)).exists()) {
				Prompt.error("Prodigal failed to produce output file: " + faaFile);
				if(command != null) Prompt.error("Failed command: " + command);
				Prompt.error("Please check if Prodigal is installed and the path is correct.");
				return -1;
			} else if((new File(faaFile)).length() == 0) {
				Prompt.error("Prodigal produced an empty output file: " + faaFile);
				if(command != null) Prompt.error("Failed command: " + command);
				return -1;
			} else {
				BufferedReader br = new BufferedReader(new FileReader(faaFile));
				String firstLine = br.readLine();
				br.close();
				if(!firstLine.startsWith(">")) {
					Prompt.error("Prodigal produced an invalid output file: " + faaFile);
					if(command != null) Prompt.error("Failed command: " + command);
					return -1;
				}
			}

			Prompt.talk("EzAAI", "Creating a submodule for converting .faa into .db...");
			EzAAI convertModule = new EzAAI("convert");
			String[] convertArgs = {"convert", "-i", faaFile, "-s", "prot", "-o", output, "-l", label, "-m", path_mmseqs, "-tmp", tmp};
			if(convertModule.run(convertArgs) < 0) return -1;

			if(!multithread) FileRemover.safeDelete(gffFile);
			FileRemover.safeDelete(faaFile);
			if(!multithread) FileRemover.safeDelete(ffnFile);
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		Prompt.print("Task finished.");
		return 0;
	}

	private int runExtractBatch() {
		class extractThread implements Callable<Integer> {
			private final String prodigal, input, output, tmp, label, session;
			public extractThread(String prodigal, String input, String output, String tmp, String label) {
				this.prodigal = prodigal;
				this.input = input;
				this.output = output;
				this.tmp = tmp;
				this.label = label;
				this.session = GenericConfig.SESSION_UID + "_" + new Random().nextInt(Integer.MAX_VALUE);
			}
			public Integer call() {
				String gffFile = tmp + File.separator + session + ".gff",
					   faaFile = tmp + File.separator + session + ".faa",
					   ffnFile = tmp + File.separator + session + ".ffn";

				// get id from the input file by trimming file extension
				String id = (new File(input)).getName();
				int lastDot = id.lastIndexOf('.');
				if(lastDot > 0) id = id.substring(0, lastDot);

				// run prodigal
				try {
					ProcCDSPredictionByProdigal procProdigal = new ProcCDSPredictionByProdigal();
					procProdigal.setOutDir(tmp + File.separator);
					procProdigal.setProdigalPath(prodigal);
					procProdigal.setGffOutFileName(gffFile);
					procProdigal.setFaaOutFileName(faaFile);
					procProdigal.setFfnOutFileName(ffnFile);
					procProdigal.execute(input, GenericConfig.DEV);

					// check prodigal output
					String command = procProdigal.getCommand();
					if(!(new File(faaFile)).exists()) {
						Prompt.error("Prodigal failed to produce output file: " + faaFile);
						if(command != null) Prompt.error("Failed command: " + command);
						Prompt.error("Please check if Prodigal is installed and the path is correct.");
						return -1;
					} else if((new File(faaFile)).length() == 0) {
						Prompt.error("Prodigal produced an empty output file: " + faaFile);
						if(command != null) Prompt.error("Failed command: " + command);
						return -1;
					} else {
						BufferedReader br = new BufferedReader(new FileReader(faaFile));
						String firstLine = br.readLine();
						br.close();
						if(!firstLine.startsWith(">")) {
							Prompt.error("Prodigal produced an invalid output file: " + faaFile);
							if(command != null) Prompt.error("Failed command: " + command);
							return -1;
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
					return -1;
				}

				// run convert submodule
				try {
					EzAAI convertModule = new EzAAI("convert");
					String[] convertArgs = {"convert", "-i", faaFile, "-s", "prot", "-o", output + File.separator + id + ".db",
							"-l", label, "-m", path_mmseqs, "-tmp", tmp};
					if(convertModule.run(convertArgs) < 0) return -1;
				} catch(Exception e) {
					e.printStackTrace();
					return -1;
				}

				// clean up
				FileRemover.safeDelete(gffFile);
				FileRemover.safeDelete(faaFile);
				FileRemover.safeDelete(ffnFile);

				// report
				Prompt.print_univ(GenericConfig.PHEAD, "Database extraction completed: " + output + File.separator + id + ".db", 'C');
				return 0;
			}
		}

		// check input directory
		File inputDir = new File(input1);
		if(inputDir.list() == null) {
			Prompt.error("Input directory is empty.");
			return -1;
		}

		// list files in input directory
		ArrayList<String> files = new ArrayList<>();
		for(String file : Objects.requireNonNull(inputDir.list())) {
			if(file.endsWith(".fa") || file.endsWith(".fna") || file.endsWith(".fasta")) {
				files.add(inputDir.getAbsolutePath() + File.separator + file);
			}
		}

		// parse label file
		HashMap<String, String> labelMap = new HashMap<>();
		if(label != null) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(label));
				String line;
				while((line = br.readLine()) != null) {
					String[] parts = line.split("\t");
					if(parts.length < 2) continue; // skip invalid lines
					int lastSep = parts[0].lastIndexOf(File.separator);
					String basename = lastSep < 0 ? parts[0] : parts[0].substring(lastSep + 1);
					labelMap.put(inputDir.getAbsolutePath() + File.separator + basename, parts[1]);
				}
				br.close();
			} catch(IOException e) {
				Prompt.warning("Failed to read label file: " + label + ". Using file names as labels.");
			}
		}

		// process files in parallel
		boolean quiet = GenericConfig.QUIET;
		GenericConfig.QUIET = true;
		ExecutorService executor = Executors.newFixedThreadPool(thread);
		List<Future<Integer>> futures = new ArrayList<>();
		for(String file : files) {
			String labelName = labelMap.getOrDefault(file, new File(file).getName());
			extractThread et = new extractThread(path_prodigal, file, output, tmp, labelName);
			futures.add(executor.submit(et));
		}
		executor.shutdown();
		try {
			for (Future<Integer> future : futures)
				if (future.get() < 0) {
					Prompt.error("An error occurred during batch extraction.");
					return -1;
				}
		} catch (InterruptedException | ExecutionException e) {
			Prompt.error("An error occurred during batch extraction: " + e.getMessage());
			return -1;
		}
		GenericConfig.QUIET = quiet;

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
			for(String str : struct) FileRemover.safeDelete(str);
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	private int runCalculate() {
		Prompt.debug("EzAAI - calculate module");
		
		try {
			// check output file format
			if(outExists) {
				BufferedReader br = new BufferedReader(new FileReader(output));
				String line = br.readLine();
				if(line == null) {
					Prompt.warning("Empty output file given.");
					outExists = false;
				} else if(!line.equals("ID 1\tID 2\tLabel 1\tLabel 2\tAAI\tCDS count 1\tCDS count 2\tMatched count\tProteome cov.\tID param.\tCov. param.")) {
					Prompt.error("Invalid output file given.");
					return -1;
				}
			}
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

			if(self && inames.length <= 1) {
				Prompt.error("Self-comparison requires directory with at least two files.");
				return -1;
			}
			
			// convert profiles into FASTA files
			List<String> ilist = new ArrayList<>(), jlist = new ArrayList<>();
			List<String> ilabs = new ArrayList<>(), jlabs = new ArrayList<>();
			File faaDir = new File(tmp + File.separator + GenericConfig.SESSION_UID + "_faa");
			if(!faaDir.exists()) faaDir.mkdirs();
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
				FileRemover.safeDelete("mm.label");
			}
			for(int j = 0 ; j < jnames.length; j++) {
				String faaPath = faaDir + File.separator + "j" + j + ".faa";
				if(dbToFaa(jnames[j], faaPath) < 0) return -1;
				jlist.add(faaPath);
				
				BufferedReader br = new BufferedReader(new FileReader("mm.label"));
				jlabs.add(br.readLine());
				br.close();
				FileRemover.safeDelete("mm.label");
			}

			// prepare match output
			BufferedWriter maw = null;
			if(matchout != null) {
				maw = new BufferedWriter(new FileWriter(matchout));
				maw.write("ID 1\tID 2\tLabel 1\tLabel 2\tCDS 1\tCDS 2\tForward\tBackward\tAverage\n");
			}

			// run MMSeqs for each fasta pairs
			Double[][] aaiTable = new Double[ilist.size()][jlist.size()];
			Integer[][] hitTable = new Integer[ilist.size()][jlist.size()];
			Integer[] ilens = new Integer[ilist.size()], jlens = new Integer[jlist.size()];

			int it = 0, sz = self ? (ilist.size() * (ilist.size() - 1) / 2) : (ilist.size() * jlist.size());
			for(int i = 0; i < ilist.size(); i++) {
				for(int j = 0; j < jlist.size(); j++) {
					if(self && i >= j) { continue; }
					Prompt.print(String.format("Calculating AAI... [Task %d/%d]", ++it, sz));
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
					procAAI.setGlobaltmp(tmp);
					procAAI.setNthread(thread);
					procAAI.setIdentity(identity);
					procAAI.setCoverage(coverage);
					if (maw != null) procAAI.setMatchout(maw);
					List<String> res = procAAI.calculateProteomePairWithDetails(ilabs.get(i), jlabs.get(j), ilist.get(i), jlist.get(j));
					hitTable[i][j] = Integer.parseInt(res.get(4));
					aaiTable[i][j] = Double.parseDouble(res.get(6));
					if(ilens[i] == null) ilens[i] = Integer.parseInt(res.get(0));
					if(jlens[j] == null) jlens[j] = Integer.parseInt(res.get(1));
				}
			}
			if(maw != null) maw.close();

			// if self, fill the remaining cells
			if(self) {
				for(int i = 0; i < ilist.size(); i++) {
					if(jlens[i] == null) jlens[i] = ilens[i];
					if(ilens[i] == null) ilens[i] = jlens[i];
					aaiTable[i][i] = 100.0;
					hitTable[i][i] = ilens[i];
					for(int j = i+1; j < jlist.size(); j++) {
						aaiTable[j][i] = aaiTable[i][j];
						hitTable[j][i] = hitTable[i][j];
					}
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
			FileRemover.safeDeleteDirectory(tmp + File.separator + GenericConfig.SESSION_UID + "_faa");
			
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

	private int runConvertDB() {
		int ret;
		if((ret = dbToFaa(input1, output)) == 0) {
			FileRemover.safeDelete("mm.label");
			Prompt.print("Task finished.");
		}
		return ret;
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
		case MODULE_CONVERTDB: 	return runConvertDB();
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
		if(arg.get("-q") != null) GenericConfig.QUIET = true;
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
		if(!STABLE) Prompt.warning("This is an unstable version. Please use with caution and report any bugs to the developer.");
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
			
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ezaai <module> [<args>]");
			System.out.println();

			String indent = String.valueOf(15);
			System.out.println(ANSIHandler.wrapper("\n Available modules", 'Y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Module", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "extract",   "Extract protein DB from genome using Prodigal");
			System.out.printf(" %-"+indent+"s%s%n", "convert",   "Convert CDS FASTA file into protein DB");
			System.out.printf(" %-"+indent+"s%s%n", "convertdb", "Convert protein DB into FASTA file");
			System.out.printf(" %-"+indent+"s%s%n", "calculate", "Calculate AAI value from protein databases using MMSeqs2");
			System.out.printf(" %-"+indent+"s%s%n", "cluster",   "Hierarchical clustering of taxa with AAI values");
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n Miscellaneous", 'Y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-nc", "No-color mode");
			System.out.printf(" %-"+indent+"s%s%n", "-nt", "No time stamps");
			System.out.printf(" %-"+indent+"s%s%n", "-v",  "Go verbose");
			System.out.printf(" %-"+indent+"s%s%n", "-q",  "Go quiet (no output except warnings/errors)");
			System.out.printf(" %-"+indent+"s%s%n", "-h",  "Print help");
			System.out.println();
		}
		if(module == MODULE_EXTRACT) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - extract", 'G'));
			System.out.println(ANSIHandler.wrapper(" Extract protein DB from prokaryotic genome sequence(s) using Prodigal", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ezaai extract -i <INPUT> -o <OUTPUT> [-l <LABEL> -t <THREAD>]");
			System.out.println();

			String indent = String.valueOf(15);
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.printf(" %-"+indent+"s%n", "SINGLE MODE");
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-i", "Input prokaryotic genome sequence");
			System.out.printf(" %-"+indent+"s%s%n", "-o", "Output protein database");
			System.out.println();

			System.out.printf(" %-"+indent+"s%n", "BATCH MODE");
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-i", "Input directory with prokaryotic genome sequences");
			System.out.printf(" %-"+indent+"s%s%n", "-o", "Output directory for protein databases");
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.printf(" %-"+indent+"s%n", "SINGLE MODE");
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-l", "Taxonomic label for phylogenetic tree");
			System.out.printf(" %-"+indent+"s%s%n", "-t", "Number of CPU threads - multi-threading requires ufasta (default: 1)");
			System.out.println();

			System.out.printf(" %-"+indent+"s%n", "BATCH MODE");
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-l", "Tab separated file with labels");
			System.out.printf(" %-"+indent+"s%s%n", "",   "(FILE_NAME <tab> LABEL, default: use file names as labels)");
			System.out.printf(" %-"+indent+"s%s%n", "-t", "Number of CPU threads to use (default: 10)");
			System.out.println();

			System.out.printf(" %-"+indent+"s%n", "COMMON");
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-tmp", "Custom temporary directory (default: /tmp/ezaai)");
			//System.out.println(String.format(" %s\t\t%s", "  ", "https://github.com/gmarcais/ufasta"));
			System.out.printf(" %-"+indent+"s%s%n", "-prodigal", "Custom path to prodigal binary (default: prodigal)");
			System.out.printf(" %-"+indent+"s%s%n", "-mmseqs", "Custom path to MMSeqs2 binary (default: mmseqs)");
			System.out.printf(" %-"+indent+"s%s%n", "-ufasta", "Custom path to ufasta binary (default: ufasta)");
			System.out.println();
		}
		if(module == MODULE_CONVERT) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - convert", 'G'));
			System.out.println(ANSIHandler.wrapper(" Convert CDS FASTA file into protein DB", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ezaai convert -i <IN_CDS> -s <SEQ_TYPE> -o <OUT_DB> [-l <LABEL>]");
			System.out.println();

			String indent = String.valueOf(15);
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-i", "Input CDS file (FASTA format)");
			System.out.printf(" %-"+indent+"s%s%n", "-s", "Sequence type of input file (nucl/prot)");
			System.out.printf(" %-"+indent+"s%s%n", "-o", "Output protein DB");
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-l", "Taxonomic label for phylogenetic tree");
			System.out.printf(" %-"+indent+"s%s%n", "-tmp", "Custom temporary directory (default: /tmp/ezaai)");
			System.out.printf(" %-"+indent+"s%s%n", "-mmseqs", "Custom path to MMSeqs2 binary (default: mmseqs)");
			System.out.println();
		}
		if(module == MODULE_CALCULATE) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - calculate", 'G'));
			System.out.println(ANSIHandler.wrapper(" Calculate AAI value from protein databases", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ezaai calculate -i <INPUT_1> -j <INPUT_2> -o <OUTPUT> [-p <PROGRAM> -t <THREAD> -id <IDENTITY> -cov <COVERAGE> -mtx <MTX_OUTPUT>]");
			System.out.println();

			String indent = String.valueOf(15);
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-i      ", "First input protein DB / directory with protein DBs");
			System.out.printf(" %-"+indent+"s%s%n", "-j      ", "Second input protein DB / directory with protein DBs");
			System.out.printf(" %-"+indent+"s%s%n", "-o      ",  "Output result file");
			System.out.println();
			
			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-p      ", "Customize calculation program [mmseqs / diamond / blastp] (default: mmseqs)");
			System.out.printf(" %-"+indent+"s%s%n", "-t      ", "Number of CPU threads to use (default: 10)");
			System.out.printf(" %-"+indent+"s%s%n", "-tmp    ", "Custom temporary directory (default: /tmp/ezaai)");
			System.out.printf(" %-"+indent+"s%s%n", "-self   ", "Assume self-comparison; -i and -j must be identical [0 / 1] (default: 0)");
			System.out.printf(" %-"+indent+"s%s%n", "-id     ", "Minimum identity threshold for AAI calculations [0 - 1.0] (default: 0.4)");
			System.out.printf(" %-"+indent+"s%s%n", "-cov    ", "Minimum query coverage threshold for AAI calculations [0 - 1.0] (default: 0.5)");
			System.out.printf(" %-"+indent+"s%s%n", "-match  ", "Path to write a result of matched CDS names");
			System.out.printf(" %-"+indent+"s%s%n", "-mtx    ", "Path to write a Matrix Market formatted output");
			System.out.printf(" %-"+indent+"s%s%n", "-mmseqs ", "Custom path to MMSeqs2 binary (default: mmseqs)");
			System.out.printf(" %-"+indent+"s%s%n", "-diamond", "Custom path to DIAMOND binary (default: diamond)");
			System.out.printf(" %-"+indent+"s%s%n", "-blastp ", "Custom path to BLASTp+ binary (default: blastp)");
			System.out.printf(" %-"+indent+"s%s%n", "-blastdb", "Custom path to makeblastdb binary (default: makeblastdb)");
			System.out.println();
		}
		if(module == MODULE_CLUSTER) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - cluster", 'G'));
			System.out.println(ANSIHandler.wrapper(" Hierarchical clustering of taxa with AAI values", 'g'));
			System.out.println();
		
			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ezaai cluster -i <AAI_TABLE> -o <OUTPUT>");
			System.out.println();

			String indent = String.valueOf(15);
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-i", "Input EzAAI result file containing all-by-all pairwise AAI values");
			System.out.printf(" %-"+indent+"s%s%n", "-o",  "Output result file");
			System.out.printf(" %-"+indent+"s%s%n", "-u",  "Use ID instead of label for tree");
			System.out.println();
		}
		if(module == MODULE_CONVERTDB) {
			System.out.println(ANSIHandler.wrapper("\n EzAAI - convertdb", 'G'));
			System.out.println(ANSIHandler.wrapper(" Convert protein DB into FASTA file", 'g'));
			System.out.println();

			System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ezaai convertdb -i <IN_DB> -o <OUT_FA>");
			System.out.println();

			String indent = String.valueOf(15);
			System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-i", "Input protein DB");
			System.out.printf(" %-"+indent+"s%s%n", "-o", "Output FASTA file");
			System.out.println();

			System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
			System.out.println(ANSIHandler.wrapper(String.format(" %-"+indent+"s%s", "Argument", "Description"), 'c'));
			System.out.printf(" %-"+indent+"s%s%n", "-tmp", "Custom temporary directory (default: /tmp/ezaai)");
			System.out.printf(" %-"+indent+"s%s%n", "-mmseqs", "Custom path to MMSeqs2 binary (default: mmseqs)");
			System.out.println();
		}
	}
}
