package leb.util.common;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Shell {
	private Process process = null;
	private BufferedReader reader = null;

	public Shell(){}
	
	public void execute(String command) {
		Prompt.debug("exec: " + ANSIHandler.wrapper(command, 'B')); 
		try{
			process = Runtime.getRuntime().exec(command);
			process.waitFor();
		}
		catch(IOException ioe) {
			ioe.printStackTrace(); System.exit(1);
		}
		catch(InterruptedException ire) {
			ire.printStackTrace(); System.exit(1);
		}
		
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}
	public void execute(String[] cmdarray) {
		Prompt.debug("exec: " + ANSIHandler.wrapper(cmdarray[cmdarray.length - 1], 'B')); 
		try{
			process = Runtime.getRuntime().exec(cmdarray);
			process.waitFor();
		}
		catch(IOException ioe) {
			ioe.printStackTrace(); System.exit(1);
		}
		catch(InterruptedException ire) {
			ire.printStackTrace(); System.exit(1);
		}
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}

	// Print the result of the execution on a console
	public void print() throws IOException {
		if(reader == null){
			Prompt.print("Shell", "No command has been executed");
			return;
		}

		String line = reader.readLine();
		while(line != null){
			System.out.println(line);
			line = reader.readLine();
		}
	}

	// Return the result of the execution as an array of strings
	public String[] raw() throws IOException {
		if(reader == null){
			Prompt.print("Shell", "No command has been executed");
			return null;
		}

		ArrayList<String> alist = new ArrayList<String>();
		String line = reader.readLine();
		while(line != null){
			alist.add(line);
			line = reader.readLine();
		}

		String[] ls = new String[alist.size()];
		for(int i = 0; i < alist.size(); i++){
			ls[i] = alist.get(i);
		}

		return ls;
	}

	public void close() throws IOException {reader.close();}

	/*
	public static void exec(String cmd) {
		try{
			Shell sh = new Shell();
			sh.execute(cmd);
			sh.close();
		}
		catch(Exception e) {
			e.printStackTrace(); System.exit(1);
		}
		
	}
	public static void exec(String[] cmd) {
		try{
			Shell sh = new Shell();
			sh.execute(cmd);
			sh.close();
		}
		catch(Exception e) {
			e.printStackTrace(); System.exit(1);
		}
		
	}
	*/
	public static String[] exec(String cmd) {
		String[] raw = null;
		try{
			Shell sh = new Shell();
			sh.execute(cmd);
			raw = sh.raw();
			sh.close();
		}
		catch(Exception e) {
			e.printStackTrace(); System.exit(1);
		}
		return raw;
	}
	public static String[] exec(String[] cmd) {
		String[] raw = null;
		try{
			Shell sh = new Shell();
			sh.execute(cmd);
			raw = sh.raw();
			sh.close();
		}
		catch(Exception e) {
			e.printStackTrace(); System.exit(1);
		}
		return raw;
	}
}
