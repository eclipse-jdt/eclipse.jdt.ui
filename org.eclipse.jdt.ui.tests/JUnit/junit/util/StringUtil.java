package junit.util;

import java.text.NumberFormat;

/**
 * A class with junit related String utilities.
 */
public class StringUtil {
	protected StringUtil() {
	}
	public static String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}
	public static String extractClassName(String className) {
		if(className.startsWith("Default package for")) 
			return className.substring(className.lastIndexOf(".")+1);
		return className;
	}
	static public String truncate(String s, int length) {
		if (s.length() > length)
			s= s.substring(0, length)+"...";
		return s;
	}
}