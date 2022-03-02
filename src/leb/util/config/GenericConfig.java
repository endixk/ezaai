/* 
 * Generic runtime config handler for pipelines
 */

package leb.util.config;

import java.util.Random;

public class GenericConfig {
	/* Running project status */
	public static String PHEAD = ""; 		// Prompt header
	public static int HLEN = 0;				// Prompt maximum header length
	private static boolean custom_hlen = false;
	
	public static void setHeader(String header) {
		PHEAD = header;
		if(!custom_hlen) HLEN  = header.length();
	}
	public static void setHeaderLength(int len) {
		HLEN  = len;
		custom_hlen = true;
	}
	
	public static final String SESSION_UID = Long.toHexString(new Random().nextLong());
	public static String TEMP_HEADER = SESSION_UID + "_";
	
	public static boolean DEV = false;      // Developer mode
	public static boolean VERB = false; 	// Program verbosity
	public static boolean NOCOLOR = false;  // No color escapes
	public static boolean TSTAMP = false;   // Print timestamp
	public static boolean KEEP = false;		// Keep temporary files
}
