
package org.eclipse.jdt.internal.ui.text.correction;

public class NameMatcher {
	
	public static boolean isSimilarName(String name1, String name2) {
		int i= 0;
		int name1len= name1.length();
		int name2len= name2.length();
		
		int len= Math.min(name1len, name2len);	
		while (i < len && isSimilarChar(name1.charAt(i), name2.charAt(i))) {
			i++;
		}
		
		int k= 0;
		while (k < len && isSimilarChar(name1.charAt(name1len - k - 1), name2.charAt(name2len - k - 1))) {
			k++;
		}
		
		int longer= Math.max(name1len, name2len);
		if (len > 2) {
			int minMatch= (longer / 2);
			if (k >= minMatch  || i >= minMatch) {
				return true;
			}
		}
		
		return (longer - k) - i <= 2;
	}
	
	private static boolean isSimilarChar(char ch1, char ch2) {
		return Character.toLowerCase(ch1) == Character.toLowerCase(ch2);
	}	

}
