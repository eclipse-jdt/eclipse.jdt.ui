/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.IRangeComparator;

/**
 * A range comparator for Java tokens.
 */
class JavaTokenComparator implements ITokenComparator {
		
	private String fText;
	private boolean fShouldEscape= true;
	private int fCount;
	private int[] fStarts;
	private int[] fLengths;

	/**
	 * Creates a TokenComparator for the given string 
	 */
	public JavaTokenComparator(String s, boolean shouldEscape) {
		if (s != null)
			fText= s;
		else
			fText= "";
		fShouldEscape= shouldEscape;

		fStarts= new int[fText.length()];
		fLengths= new int[fText.length()];
		fCount= 0;
		
		for (int ix= 0; ix < fText.length();) {
			
			fStarts[fCount]= ix;
			
			try {
				char c= fText.charAt(ix++);
				switch (c) {
				
				case '/':
					c= fText.charAt(ix++);
					if (c != '/' && c != '*')
						ix--;
					break;
					
				case '=':
					c= fText.charAt(ix++);
					if (c != '=')
						ix--;
					break;
					
				case '!':
					c= fText.charAt(ix++);
					if (c != '=')
						ix--;
					break;
					
				case '<':
					c= fText.charAt(ix++);
					if (c != '=' && c != '<')
						ix--;
					break;
					
				case '>':
					c= fText.charAt(ix++);
					if (c != '=' && c != '>')
						ix--;
					break;
					
				case '&':
					c= fText.charAt(ix++);
					if (c != '=' && c != '&')
						ix--;
					break;
					
				case '|':
					c= fText.charAt(ix++);
					if (c != '=' && c != '|')
						ix--;
					break;
													
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					do {
						c= c= fText.charAt(ix++);
					} while(Character.isDigit((char)c));
					ix--;
					break;
	
				default:
					if (Character.isWhitespace(c) && (c != '\n')) {
						do {
							c= c= fText.charAt(ix++);
						} while (Character.isWhitespace(c) && (c != '\n'));
						ix--;
					} else if (Character.isJavaIdentifierStart((char)c)) {
						do {
							c= c= fText.charAt(ix++);
						} while (Character.isJavaIdentifierPart((char)c));
						ix--;
					} else {
						// single character token
					}
					break;
				}
			} catch (StringIndexOutOfBoundsException ex) {
			}
			
			if (ix > fStarts[fCount]) {
				fLengths[fCount]= ix-fStarts[fCount];
				fCount++;
			}
		}
	}	

	/**
	 * Returns the number of token in the string.
	 *
	 * @return number of token in the string
	 */
	public int getRangeCount() {
		return fCount;
	}

	/* (non Javadoc)
	 * see ITokenComparator.getTokenStart
	 */
	public int getTokenStart(int index) {
		if (index < fCount)
			return fStarts[index];
		return fText.length();
	}

	/* (non Javadoc)
	 * see ITokenComparator.getTokenLength
	 */
	public int getTokenLength(int index) {
		if (index < fCount)
			return fLengths[index];
		return 0;
	}
	
	/**
	 * Returns <code>true</code> if a token given by the first index
	 * matches a token specified by the other <code>IRangeComparator</code> and index.
	 *
	 * @param thisIndex	the number of the token within this range comparator
	 * @param other the range comparator to compare this with
	 * @param otherIndex the number of the token within the other comparator
	 * @return <code>true</code> if the token are equal
	 */
	public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
		if (other != null && getClass() == other.getClass()) {
			JavaTokenComparator tc= (JavaTokenComparator) other;
			int thisLen= getTokenLength(thisIndex);
			int otherLen= tc.getTokenLength(otherIndex);
			if (thisLen == otherLen)
				return fText.regionMatches(false, getTokenStart(thisIndex), tc.fText, tc.getTokenStart(otherIndex), thisLen);
		}
		return false;
	}

	/**
	 * Aborts the comparison if the number of tokens is too large.
	 *
	 * @return <code>true</code> to abort a token comparison
	 */
	public boolean skipRangeComparison(int length, int max, IRangeComparator other) {

		if (!fShouldEscape)
			return false;

		if (getRangeCount() < 50 || other.getRangeCount() < 50)
			return false;

		if (max < 100)
			return false;

		if (length < 100)
			return false;

		if (max > 800)
			return true;

		if (length < max / 4)
			return false;

		return true;
	}
	
//	public static void main(String args[]) {
//		//String in= "private static boolean isWhitespace(char c) {";
//		String in= "for (int j= 0; j < l-1; j++) {";
//		//String in= "abc";
//		ITokenComparator tc= new JavaTokenComparator(in, false);
//		
//		System.out.println("n: " + tc.getRangeCount());
//		System.out.println(in);
//		
//		int p= 0;
//		for (int i= 0; i < tc.getRangeCount(); i++) {
//			int l= tc.getTokenLength(i);
//			System.out.print("<");
//			
//			for (int j= 0; j < l-1; j++) {
//				System.out.print(" ");
//			}
//		}
//		System.out.println();		
//	}
}
