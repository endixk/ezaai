package leb.wrapper;

import leb.util.common.ExecHandler;

public class DiamondWrapper extends ExecHandler {
	
	static public int MAKEDB 	= -1;
	static public int BLASTX 	= 0;
	static public int BLASTP 	= 1;
	
	public DiamondWrapper(String diamondPath){
		super.init(diamondPath);
	}
		
	public DiamondWrapper(String diamondPath, int type){
		super.init(diamondPath);
		if(type == MAKEDB) {
			addArgument("makedb");
		}
		else if(type == BLASTX){
			addArgument("blastx");
		}else if(type == BLASTP){
			addArgument("blastp");
		}
		
	}
	
	public void setQueryFileName(String queryFileName) {
		addArgument("--query", queryFileName);
	}


	public void setOutFileName(String outFileName) {
    	addArgument("--out", outFileName);
	}


	public void setInputFastaForDb(String inFileName) {
		addArgument("--in", inFileName);
	}

	public void setDbFile(String dbFileName){
		addArgument("--db", dbFileName);
	}
	
	public void setSamOutFile(String samFileName){
		addArgument("--sam", samFileName);
	}
	
	public void setEvalue(double evalue){
		addArgument("--evalue", evalue);
	}
	
	public void setScore(double score){
		addArgument("--min-score", score);
	}
	
	public void setThreads(int threads){
		addArgument("--threads", threads);
	}
	
	public void setNoHits(int hits){
		addArgument("--max-target-seqs", hits);
	}
	
	public void setSensitive(){
		addArgument("--sensitive");
	}
	
	public void setIdentity(double identity){
		addArgument("--id", identity);
	}
	
	public void setQueryCover(double queryCover) {
		addArgument("--query-cover", queryCover);
	}
	
	public void run() {
		super.exec();
	}
}
