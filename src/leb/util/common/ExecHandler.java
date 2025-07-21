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
		if(this.args == null) this.args = new ArrayList<>();
		if(this.argMap == null) this.argMap = new HashMap<>();
	}
	
	protected void addArgument(String opt, Object val) {
		args.add(opt);
		argMap.put(opt, " " + val.toString());
	}
	protected void addArgument(String val) {
		args.add(val);
		argMap.put(val, "");
	}
	
	public String getCommandLine() {
		StringBuilder cmd = new StringBuilder(job);
		for(String arg : args) {
			cmd.append(" ").append(arg).append(argMap.get(arg));
		}
		return cmd.toString();
	}
	
	protected void exec() {
		Shell.exec(getCommandLine());
	}
}
