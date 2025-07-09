/* 
 * Prompt printing manager for pipelines
 */

package leb.util.common;

import leb.util.config.GenericConfig;

public class Prompt {
	private static String buildMessage(String head, String message, char color) {
		int hlen = head.length();
		StringBuilder headBuilder = new StringBuilder(head);
		for(int i = 0; i < GenericConfig.HLEN - hlen; i++) headBuilder.append(" ");
		head = headBuilder.toString();
		String header = ANSIHandler.wrapper(GenericConfig.TSTAMP ?
				String.format("[%s] %s |:", TimeKeeper.timeStamp(), head) :
				String.format("%s |:", head), color);
		return String.format("%s  %s", header, message);
	}
	
	// universal standard for prompt line print
	public static synchronized void print_univ(String head, String message, char color) {
		System.out.println(buildMessage(head, message, color));
	}
	
	public static void print(String head, String message){
		if(!GenericConfig.QUIET) print_univ(head, message, 'C');
	}
	public static void print(String message){
		print(GenericConfig.PHEAD, message);
	}
	public static void talk(String head, String message) {
		if(GenericConfig.VERB & !GenericConfig.QUIET) print_univ(head, message, 'c');
	}
	public static void talk(String message) {
		talk(GenericConfig.PHEAD, message);
	}
	public static void debug(String head, String message) {
		if(GenericConfig.DEV & !GenericConfig.QUIET) print_univ(head, message, 'G');
	}
	public static void debug(String message) { 
		debug("DEV", message);
	}
	public static void error(String message) {
		print_univ("ERROR", message, 'r');
	}
	public static void warning(String message) {
		print_univ("WARNING", message, 'y');
	}
}

