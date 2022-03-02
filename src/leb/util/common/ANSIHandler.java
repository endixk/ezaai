/* 
 * ANSI color escape mapper and wrapper
 */

package leb.util.common;

import leb.util.config.GenericConfig;

public class ANSIHandler {
	/* ANSI escape codes */
	public static final String RESET 	= "\u001B[0m";		// length 4
	public static final String BLACK 	= "\u001B[30m";		// length 5
	public static final String RED 		= "\u001B[31m";
	public static final String GREEN 	= "\u001B[32m";
	public static final String YELLOW 	= "\u001B[33m";
	public static final String BLUE 	= "\u001B[34m";
	public static final String PURPLE 	= "\u001B[35m";
	public static final String CYAN 	= "\u001B[36m";
	public static final String WHITE 	= "\u001B[37m";
	public static final String BBLACK	= "\u001B[30;1m";	// length 7
	public static final String BRED		= "\u001B[31;1m";
	public static final String BGREEN	= "\u001B[32;1m";
	public static final String BYELLOW  = "\u001B[33;1m";	
	public static final String BBLUE	= "\u001B[34;1m";
	public static final String BCYAN	= "\u001B[36;1m";
	
	
	
	private static String csMapper(char code) {
		if(GenericConfig.NOCOLOR) return "";
		switch(code) {		
		case 'x': return RESET;
		case 'k': return BLACK;
		case 'r': return RED;
		case 'g': return GREEN;
		case 'y': return YELLOW;
		case 'b': return BLUE;
		case 'p': return PURPLE;
		case 'c': return CYAN;
		case 'w': return WHITE;
		case 'K': return BBLACK;
		case 'R': return BRED;
		case 'G': return BGREEN;
		case 'Y': return BYELLOW;
		case 'B': return BBLUE;
		case 'C': return BCYAN;
		default : return null;
		}
	}
	
	public static String wrapper(String str, char code) {
		return String.format("%s%s%s", csMapper(code), str, csMapper('x'));
	}
	public static String wrapper(Object obj, char code) {
		return String.format("%s%s%s", csMapper(code), obj.toString(), csMapper('x'));
	}
}
