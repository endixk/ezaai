package leb.util.common;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

public class Shell {
	private ProcessBuilder processBuilder = null;
	private Process process = null;
	private BufferedReader reader = null;

	public Shell(){}
	
	public void execute(String command) {
		Prompt.debug("exec: " + ANSIHandler.wrapper(command, 'B')); 
		try{
			processBuilder = new ProcessBuilder();
			processBuilder.command("/bin/bash", "-c", command);
			processBuilder.redirectErrorStream(true);
			
			process = processBuilder.start();
			process.waitFor();
		}
		catch(IOException | InterruptedException e) {
			e.printStackTrace(); System.exit(1);
		}

		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}
	public void executeFrom(String command, File dir) {
		Prompt.debug("exec: " + ANSIHandler.wrapper(dir.getAbsolutePath(), 'g') + "$ " + ANSIHandler.wrapper(command, 'B')); 
		try{
			processBuilder = new ProcessBuilder();
			processBuilder.command("/bin/bash", "-c", command);
			processBuilder.directory(dir);
			processBuilder.redirectErrorStream(true);
			
			process = processBuilder.start();
			process.waitFor();
		}
		catch(IOException | InterruptedException e) {
			e.printStackTrace(); System.exit(1);
		}

		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}

	// Return the result of the execution as an array of strings
	public String[] raw() throws IOException {
		if(reader == null){
			Prompt.print("Shell", "No command has been executed");
			return null;
		}

		ArrayList<String> alist = new ArrayList<>();
		String line = reader.readLine();
		do {
			alist.add(line);
			line = reader.readLine();
		} while(line != null);
		alist.add(""); // buffer
		
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
	public static void exec(String cmd, File dir) {
		try{
			Shell sh = new Shell();
			sh.executeFrom(cmd, dir);
			sh.close();
		}
		catch(Exception e) {
			e.printStackTrace(); System.exit(1);
		}
	}
}
