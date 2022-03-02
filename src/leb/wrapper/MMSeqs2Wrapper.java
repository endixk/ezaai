package leb.wrapper;

import leb.util.common.ExecHandler;
import leb.util.common.Prompt;

public class MMSeqs2Wrapper extends ExecHandler {
	
	private static String PATH = "mmseqs";
	public static void setMmseqsPath(String mmseqsPath) {PATH = mmseqsPath;}
	
//	private Integer maxAccept = (1 << 16);
//	public void setMaxAccept(Integer maxAccept) {this.maxAccept = maxAccept;}
	private Integer threads = 72;
	public void setThreads(Integer threads) {this.threads = threads;}
	private Integer alignmentMode = 2;
	public void setAlignmentMode(Integer alignmentMode) {this.alignmentMode = alignmentMode;}
	
	
	public MMSeqs2Wrapper() {
		init(PATH);
	}
	
	// mmseqs createdb
	public void runCreateDb(String inFileName, String outDbName) {
		addArgument("createdb");
		addArgument(inFileName);
		addArgument(outDbName);
		Prompt.debug("COMMAND: "+super.getCommandLine());
		super.exec();
	}
	
	// mmseqs search
	public void runSearch(String queryDbName, String targetDbName, String outAlignName) {
		runSearch(queryDbName, targetDbName, outAlignName, "tmp");
	}
	public void runSearch(String queryDbName, String targetDbName, String outAlignName, String tmpDir) {
		addArgument("search");
		addArgument(queryDbName);
		addArgument(targetDbName);
		addArgument(outAlignName);
		addArgument(tmpDir);
		addArgument("--alignment-mode", alignmentMode);
//		addArgument("--max-accept", maxAccept);
		addArgument("--threads", threads);
		Prompt.debug("COMMAND: "+super.getCommandLine());
		super.exec();
	}
	
	// mmseqs rbh
	public void runRbh(String inDbName1, String inDbName2, String outAlignName) {
		runRbh(inDbName1, inDbName2, outAlignName, "tmp");
	}
	public void runRbh(String inDbName1, String inDbName2, String outAlignName, String tmpDir) {
		addArgument("rbh");
		addArgument(inDbName1);
		addArgument(inDbName2);
		addArgument(outAlignName);
		addArgument(tmpDir);
		addArgument("--threads", threads);
		Prompt.debug("COMMAND: "+super.getCommandLine());
		super.exec();
	}
	
	// mmseqs filterdb in out --extract-lines 1
	public void runFilterdb(String inDbName, String outDbName) {
		runFilterdb(inDbName, outDbName, "tmp");
	}
	public void runFilterdb(String inDbName, String outDbName, String tmpDir) {
		addArgument("filterdb");
		addArgument(inDbName);
		addArgument(outDbName);
		addArgument("--extract-lines", 1);
		addArgument("--threads", threads);
		Prompt.debug("COMMAND: "+super.getCommandLine());
		super.exec();
	}
	
	// mmseqs convertalis
	public void runConvertAlis(String inDbName1, String inDbName2, String inAlignName, String outFileName) {
		addArgument("convertalis");
		addArgument(inDbName1);
		addArgument(inDbName2);
		addArgument(inAlignName);
		addArgument(outFileName);
		addArgument("--threads", threads);
		Prompt.debug("COMMAND: "+super.getCommandLine());
		super.exec();
	}
	
	// mmseqs convert2fasta
	public void runConvert2Fasta(String inDbName, String outFileName) {
		addArgument("convert2fasta");
		addArgument(inDbName);
		addArgument(outFileName);
		Prompt.debug("COMMAND: "+super.getCommandLine());
		super.exec();
	}
}
