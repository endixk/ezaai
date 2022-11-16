package leb.wrapper;

import leb.util.common.ExecHandler;

public class BlastPlusWrapper extends ExecHandler {
        
    public static final int OUTPUT_TAB 	  = 6; //tabular output
    public static final int OUTPUT_SIMPLE = 7; // simple format
    public static final int OUTPUT_TEXT   = 0; // Has alignment view

    public BlastPlusWrapper(String path) {
    	super.init(path);
    }
    
    public void setOutFmt(int outputFmt){
        switch (outputFmt) {
        	case OUTPUT_TAB:
            case OUTPUT_SIMPLE:
            case OUTPUT_TEXT:
                break;
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

    
    private void setAlignedView(int alignedView) {
        addArgument("-outfmt",alignedView);
    }
    
    public void setMaxTargetSequence(int maxTarget) {
        addArgument("-max_target_seqs", maxTarget);
    }
    
    public void setCoreForMultiThread(int noCore) {
        addArgument("-num_threads",noCore);
    }
   
    public void run() {
        super.exec();
    }
}
