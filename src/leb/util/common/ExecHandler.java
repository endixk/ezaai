package leb.util.common;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public abstract class ExecHandler {
	protected String job = null;
	protected List<String> args = null;
	protected Map<String, String> argMap = null;
	
	protected void init(String job) {
		this.job = job;
		if(this.args == null) this.args = new ArrayList<String>();
		if(this.argMap == null) this.argMap = new HashMap<String, String>();
	}
	
	protected void addArgument(String opt, Object val) {
		args.add(opt);
		argMap.put(opt, " " + val.toString());
	}
	protected void addArgument(String val) {
		args.add(val);
		argMap.put(val, "");
	}
	
	protected String getCommandLine() {
		String cmd = job;
		for(String arg : args) {
			cmd += " " + arg + argMap.get(arg);
		}
		return cmd;
	}
	
	protected String[] exec() {
		String[] carr = {"/bin/bash", "-c", getCommandLine()};
		return Shell.exec(carr);
	}
}
