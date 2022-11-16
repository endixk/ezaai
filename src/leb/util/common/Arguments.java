package leb.util.common;

import java.util.Hashtable;

public class Arguments {
	public Hashtable<String,String> hash = new Hashtable<>();
	public String[] args;
	
	int process_next(int index)
	{
		if (index==args.length-1) {
			hash.put(args[index], "");// for last, just add
			return args.length;
		}
		if (args[index+1].startsWith("-"))  {
			
			hash.put(args[index], "");
			return index;
		}
		hash.put(args[index],args[index+1]);
		return index+1;
	}
	
	public Arguments(String[] args){
		this.args = args;
		for (int i=0; i<args.length; i++)
		{
			if (args[i].startsWith("-"))
			{
				i=process_next(i);
			}
		}
	}
	
	/**
	 * 
	 * @param arg - tag of argument
	 * @return return null if no exist matched tag, or 
	 * empty string if anything value is not assigned for the tag 
	 */
	public String get(String arg)
	{
		return hash.get(arg);
	}
	
}
