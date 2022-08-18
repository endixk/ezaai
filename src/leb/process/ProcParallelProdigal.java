package leb.process;

import leb.util.common.Shell;
import leb.util.config.GenericConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;

import java.io.File;

public class ProcParallelProdigal {
	private String 	input = null,
					output = null,
					temp = null,
					ufasta = null,
					prodigal = null;
	private Integer thread = null;
	
	public ProcParallelProdigal(String input, String output, String temp, String ufasta, String prodigal, int thread) {
		this.input = input;
		this.output = output;
		this.temp = temp;
		this.ufasta = ufasta;
		this.prodigal = prodigal;
		this.thread = thread;
	}
	
	// build split command
	private String splitCommand() {
		String command = String.format("%s split -i %s", ufasta, input);
		for(int i = 0; i < thread; i++)
			command += String.format(" %s_%d.fa", temp + GenericConfig.SESSION_UID, i);
		return command;
	}
	
	// generate thread pool to run prodigal in parallel
	private void poolProdigal() throws ExecutionException, InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(thread);
		List<Future<Integer>> futures = new ArrayList<>();
		
		for(int i = 0; i < thread; i++) {
			ProdigalThread pt = new ProdigalThread(
					prodigal,
					String.format("%s_%d.fa", temp + GenericConfig.SESSION_UID, i),
					String.format("%s_%d.out.fa", temp + GenericConfig.SESSION_UID, i));
			futures.add(executor.submit(pt));
		}
		
		executor.shutdown();
		for(Future<Integer> future : futures) future.get();
		// while (!executor.awaitTermination(1, TimeUnit.SECONDS));
	}
	
	// build concatenation command
	private String catCommand() {
		String command = "cat ";
		for(int i = 0; i < thread; i++)
			command += String.format("%s_%d.out.fa ", temp + GenericConfig.SESSION_UID, i);
		command += String.format("> %s", output);
		return command;
	}
	
	private void clean() {
		for(int i = 0; i < thread; i++) {
			(new File(String.format("%s_%d.fa", temp + GenericConfig.SESSION_UID, i))).delete();
			(new File(String.format("%s_%d.out.fa", temp + GenericConfig.SESSION_UID, i))).delete();
			(new File(String.format("%s_%d.fa.log", temp + GenericConfig.SESSION_UID, i))).delete();
		}
	}
	
	public int run() {
		try {
			Shell.exec(splitCommand());
			poolProdigal();
			Shell.exec(catCommand());
			clean();
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
}

class ProdigalThread implements Callable<Integer> {
	String prodigal = null, in = null, out = null;
	public ProdigalThread(String prodigal, String in, String out) {
		this.prodigal = prodigal;
		this.in = in;
		this.out = out;
	}
	public Integer call() {
		String command = String.format("%s -i %s -a %s -q -p meta -g 11 > %s.log", prodigal, in, out, in);
		Shell.exec(command);
		return 0;
	}
}