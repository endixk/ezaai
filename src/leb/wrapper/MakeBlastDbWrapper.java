package leb.wrapper;

import leb.util.common.ExecHandler;

public class MakeBlastDbWrapper extends ExecHandler {

    private final String dbFileName;

    static public int TYPE_PROTEIN = 1;
	
    public MakeBlastDbWrapper(String path, String dbFileName, int molecule_type) {
        init(path);
        this.dbFileName = dbFileName;
    	if (molecule_type == TYPE_PROTEIN) addArgument("-dbtype", "prot");
    	else addArgument("-dbtype", "nucl");
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