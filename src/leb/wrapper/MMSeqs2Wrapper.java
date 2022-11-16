package leb.wrapper;

import leb.util.common.ExecHandler;

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
		super.exec();
	}
	
	// mmseqs search
	public void runSearch(String queryDbName, String targetDbName, String outAlignName, String tmpDir) {
		addArgument("search");
		addArgument(queryDbName);
		addArgument(targetDbName);
		addArgument(outAlignName);
		addArgument(tmpDir);
		addArgument("--alignment-mode", alignmentMode);
//		addArgument("--max-accept", maxAccept);
		addArgument("--threads", threads);
		super.exec();
	}
	
	// mmseqs filterdb in out --extract-lines 1
	public void runFilterdb(String inDbName, String outDbName, String ignoredTmpDir) {
		addArgument("filterdb");
		addArgument(inDbName);
		addArgument(outDbName);
		addArgument("--extract-lines", 1);
		addArgument("--threads", threads);
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
		super.exec();
	}
	
	// mmseqs convert2fasta
	public void runConvert2Fasta(String inDbName, String outFileName) {
		addArgument("convert2fasta");
		addArgument(inDbName);
		addArgument(outFileName);
		super.exec();
	}
}
