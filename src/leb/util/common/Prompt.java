/* 
 * Prompt printing manager for pipelines
 */

package leb.util.common;

import leb.util.config.GenericConfig;

public class Prompt {
	private static String buildMessage(String head, String message, char color) {
		int hlen = head.length();
		for(int i = 0; i < GenericConfig.HLEN - hlen; i++) head += " ";
		String header = ANSIHandler.wrapper(GenericConfig.TSTAMP ?
				String.format("[%s] %s |:", TimeKeeper.timeStamp(), head) :
				String.format("%s |:", head), color);
		return String.format("%s  %s", header, message);
	}
	
	// universal standard for prompt line print
	public static void print_univ(String head, String message, char color) {
		System.out.println(buildMessage(head, message, color));
	}
	
	public static void print(String head, String message){
		print_univ(head, message, 'C');
	}
	public static void print(String message){
		print(GenericConfig.PHEAD, message);
	}
	public static void talk(String head, String message) {
		if(GenericConfig.VERB) print_univ(head, message, 'c');
	}
	public static void talk(String message) {
		talk("VERB", message);
	}
	public static void debug(String head, String message) {
		if(GenericConfig.DEV) print_univ(head, message, 'G');
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
	
	
	// print with no new-line character
	public static void print_nnc(String head, String message){
		print_nnc(head, message, 'C');
	}
	public static void print_nnc(String head, String message, char color) {
		System.out.print(buildMessage(head, message, color));
	}
	public static void print_nnc(String message){ print_nnc(GenericConfig.PHEAD, message); }
	public static void dynamicHeader(String message) { if(!GenericConfig.VERB) print_nnc(message);}
	public static void dynamic(String message) { if(!GenericConfig.VERB) System.out.print(message);}
	public static void erase(String msg, int sub) {
		for(int x = 0; x < msg.length() - sub; x++) System.out.print("\b");
		for(int x = 0; x < msg.length() - sub; x++) System.out.print(" ");
		System.out.flush();
		for(int x = 0; x < msg.length() - sub; x++) System.out.print("\b");
	}
	public static void erase(String msg) {erase(msg, 0);}
}

