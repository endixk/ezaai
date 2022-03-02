package leb.wrapper;

import leb.util.common.ExecHandler;

public class BlastPlusWrapper extends ExecHandler {
        
    public static final int OUTPUT_TAB 	  = 6; //tabular output
    public static final int OUTPUT_SIMPLE = 7; // simple format
    public static final int OUTPUT_TEXT   = 0; // Has alignment view

    public BlastPlusWrapper() {}
    
    public void setOutFmt(int outputFmt){
        switch (outputFmt) {
        	case OUTPUT_TAB:	 break;
            case OUTPUT_SIMPLE:  break;
            case OUTPUT_TEXT:    break;
            default:
                throw new IllegalArgumentException();
        } 
        setAlignedView(outputFmt);
    }
    
    public void setProgramPath(String path) {
        super.init(path);
    }
    
    public void setInFileName(String inFileName) {
        addArgument("-query",inFileName);
    }

    public void setDbFileName(String dbFileName) {
        addArgument("-db",dbFileName);
    }
    public void setOutFileName(String outFileName) {
        addArgument("-out", outFileName);
    }
    public void setFilterEvalue(double filterEvalue) {
        addArgument("-evalue",filterEvalue);
    }

    public void setInclusionEvalue(double evalue){
    	addArgument("-inclusion_ethresh", evalue);
    }
    
    private void setAlignedView(int alignedView) {
        addArgument("-outfmt",alignedView);
    }
    
    public void setMaxTargetSequence(int maxTarget) {
        
        addArgument("-max_target_seqs", maxTarget);
        
    }
    
    public void setNumDescriptions(int numHits){ //set numbers of hits between query and db
    	addArgument("-num_descriptions", numHits);
    }
    
    public void setNumAlignments(int numAlign){ //set numbers of sequence alignment that are shown in the result
    	addArgument("-num_alignments", numAlign);
    }
    
    public void setDbSize(int dbSize){
    	addArgument("-dbsize", dbSize);
    }
    public void setDbSize(long dbSize){
    	addArgument("-dbsize", dbSize);
    }
    
    public void setDbGenCode(int db_gen_code) {
    	addArgument("-db_gencode", db_gen_code);
    }
    
    public void setQueryGenCode(int query_genetic_code) {
    	addArgument("-query_gencode", query_genetic_code);
    }

    public void setInMsa(String msaFileName){
    	addArgument("-in_msa", msaFileName);
    }
    
    public void setInPssm(String pssmFileName){
    	addArgument("-in_pssm", pssmFileName);
    }
    
    public void setOutPssm(String pssmFileName){
    	addArgument("-out_pssm", pssmFileName);
    }
    
    public void setCompBasedStats(int stat){
    	addArgument("-comp_based_stats", stat);
    	/*
    	 *  -comp_based_stats <String>
   Use composition-based statistics:
       D or d: default (equivalent to 2 )
       0 or F or f: No composition-based statistics
       1: Composition-based statistics as in NAR 29:2994-3005, 2001
       2 or T or t : Composition-based score adjustment as in Bioinformatics
   21:902-911,
       2005, conditioned on sequence properties
       3: Composition-based score adjustment as in Bioinformatics 21:902-911,
       2005, unconditionally
   Default = `2'

    	 */
    }
    
    public void setNumIterations(int iteration){//setting num. of iteration in psiblast
    	addArgument("-num_iterations", iteration);
    }
  
    public void setFilterQuerySeq(String filterQuerySeq) {  // T or F//
    	addArgument("-seg",filterQuerySeq);
    }
    
    public void setCoreForMultiThread(int noCore) {
        addArgument("-num_threads",noCore);
    }

    public void setAniOption() {
        setFilterEvalue(1e-15);
        setFilterQuerySeq("no");
        addArgument("-xdrop_gap", 150);
        addArgument("-penalty", -1);
        addArgument("-reward", 1);
        
    }
    
    public void setOrthoAniOption(){
        
        addArgument("-task", "blastn");
        setFilterQuerySeq("no");
        setCoreForMultiThread(4);
        setMaxTargetSequence(1);
        setFilterEvalue(1e-15);
        addArgument("-xdrop_gap", 150);
        addArgument("-penalty", -1);
        addArgument("-reward", 1);
        addArgument("-outfmt", "6 qseqid sseqid pident length nident mismatch qstart qend sstart send");
        
    }
   
    public void run() {
        super.exec();
    }
}
