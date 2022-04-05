package leb.wrapper;

import java.io.File;

import leb.util.common.ExecHandler;
/**
 * Created by Administrator on 2014-08-28.
 */

public class MakeBlastDbWrapper extends ExecHandler {

    private String dbFileName=null;
    
    static public int TYPE_DNA = 0;
    static public int TYPE_PROTEIN = 1;
	
    public MakeBlastDbWrapper(String path, String dbFileName, int molecule_type) {
        init(path);
        this.dbFileName = dbFileName;
    	if (molecule_type == TYPE_PROTEIN) addArgument("-dbtype", "prot");
    	else addArgument("-dbtype", "nucl");
    }

    public void applyIndex(File indexFile) {
        addArgument("-in", indexFile.getAbsolutePath());
    }

    public void setOutFileNames(String fileName) {
    	addArgument("-out", fileName);
    }
    
    void setParameters() {
    	addArgument("-in",dbFileName);
    }
    
    public void run() {
    	setParameters();
    	super.exec();
    }

}