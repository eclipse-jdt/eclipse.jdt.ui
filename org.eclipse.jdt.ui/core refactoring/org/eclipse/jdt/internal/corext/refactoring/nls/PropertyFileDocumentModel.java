/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.ibm.icu.text.Collator;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

public class PropertyFileDocumentModel {

	private static final char[] HEX_DIGITS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    private List fKeyValuePairs;
    private String fLineDelimiter;

    public PropertyFileDocumentModel(IDocument document) {
        parsePropertyDocument(document);
        fLineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
    }

    /**
	 * @return the line delimiter used by the document described by this model
	 */
    public String getLineDelimiter() {
		return fLineDelimiter;
	}

    /**
	 * Return the key value pair in this model with the key <code>key</code>
	 *
	 * @param key the key of the pair
	 * @return the pair with the key or <b>null</b> if no such pair.
	 */
    public KeyValuePair getKeyValuePair(String key) {
    	for (int i= 0; i < fKeyValuePairs.size(); i++) {
            KeyValuePairModell keyValuePair = (KeyValuePairModell) fKeyValuePairs.get(i);
            if (keyValuePair.getKey().equals(key)) {
            	return keyValuePair;
            }
    	}
    	return null;
    }

    private InsertEdit insert(KeyValuePair keyValuePair) {
        KeyValuePairModell keyValuePairModell = new KeyValuePairModell(keyValuePair);
        int index = findInsertPosition(keyValuePairModell);
        KeyValuePairModell insertHere = (KeyValuePairModell) fKeyValuePairs.get(index);
        int offset = insertHere.fOffset;

        String extra= ""; //$NON-NLS-1$
        if (insertHere instanceof LastKeyValuePair && ((LastKeyValuePair)insertHere).needsNewLine()) {
        	extra= fLineDelimiter;
        	((LastKeyValuePair)insertHere).resetNeedsNewLine();
        	offset-= insertHere.fLeadingWhiteSpaces;
        } else if (index > 0) {
        	String beforeKey= ((KeyValuePair) fKeyValuePairs.get(index - 1)).fKey;
			String afterKey= insertHere.fKey;
			String key= keyValuePair.fKey;
			int distBefore= NLSUtil.invertDistance(key, beforeKey);
			int distAfter= NLSUtil.invertDistance(key, afterKey);
			if (distBefore > distAfter) {
				offset-= insertHere.fLeadingWhiteSpaces;
			} else if (distBefore == distAfter && Collator.getInstance().compare(beforeKey, afterKey) < 0) {
				offset-= insertHere.fLeadingWhiteSpaces;
			} else {
				//insert it before afterKey -> move the leading white spaces to the inserted pair
				keyValuePairModell.fLeadingWhiteSpaces= insertHere.fLeadingWhiteSpaces;
				insertHere.fLeadingWhiteSpaces= 0;
			}
        }

        keyValuePairModell.fOffset= offset;
        fKeyValuePairs.add(index, keyValuePairModell);
        return new InsertEdit(offset, extra + keyValuePairModell.getKeyValueText());
    }

    /**
     * Inserts the given key value pairs into this model at appropriate
     * positions. Records all required text changes in the given change
     *
     * @param keyValuePairs the key value pairs to insert
     * @param change the change to use to record text changes
     */
    public void insert(KeyValuePair[] keyValuePairs, TextChange change) {

        ArrayList sorted= new ArrayList(Arrays.asList(keyValuePairs));
        Collections.sort(sorted, new Comparator() {
			public int compare(Object o1, Object o2) {
				KeyValuePair p1= (KeyValuePair) o1;
				KeyValuePair p2= (KeyValuePair) o2;
				return Collator.getInstance().compare(p1.fKey, p2.fKey);
			}
        });

        for (int i = 0; i < sorted.size(); i++) {
            KeyValuePair curr= (KeyValuePair) sorted.get(i);
			InsertEdit insertEdit= insert(curr);

            String message= Messages.format(NLSMessages.NLSPropertyFileModifier_add_entry, BasicElementLabels.getJavaElementName(curr.getKey()));
			TextChangeCompatibility.addTextEdit(change, message, insertEdit);
        }
    }

    public DeleteEdit remove(String key) {
    	for (Iterator iter = fKeyValuePairs.iterator(); iter.hasNext();) {
            KeyValuePairModell keyValuePair = (KeyValuePairModell) iter.next();
            if (keyValuePair.fKey.equals(key)) {
            	KeyValuePairModell next = (KeyValuePairModell) iter.next();
            	return new DeleteEdit(keyValuePair.fOffset, next.fOffset - keyValuePair.fOffset);
            }
        }
        return null;
    }

    public ReplaceEdit replace(KeyValuePair toReplace, KeyValuePair replaceWith) {
        for (Iterator iter = fKeyValuePairs.iterator(); iter.hasNext();) {
            KeyValuePairModell keyValuePair = (KeyValuePairModell) iter.next();
            if (keyValuePair.fKey.equals(toReplace.getKey())) {
                String newText= new KeyValuePairModell(replaceWith).getKeyValueText();
                KeyValuePairModell next = (KeyValuePairModell) iter.next();
                int range = next.fOffset - keyValuePair.fOffset;
            	return new ReplaceEdit(keyValuePair.fOffset, range, newText);
            }
        }
        return null;
    }

    private int findInsertPosition(KeyValuePairModell keyValuePair) {
    	ArrayList keys= new ArrayList();
        for (int i= 0; i < fKeyValuePairs.size(); i++) {
            KeyValuePairModell element = (KeyValuePairModell) fKeyValuePairs.get(i);
            if (! (element instanceof LastKeyValuePair))
            	keys.add(element.getKey());
        }
        int insertIndex= NLSUtil.getInsertionPosition(keyValuePair.getKey(), keys);

        if (insertIndex < fKeyValuePairs.size() - 1) {
            insertIndex++;
        }

        return insertIndex;
    }

    private void parsePropertyDocument(IDocument document) {
        fKeyValuePairs = new ArrayList();

        SimpleLineReader reader = new SimpleLineReader(document);
        int offset = 0;
        String line = reader.readLine();
        int leadingWhiteSpaces = 0;
        while (line != null) {
            if (!SimpleLineReader.isCommentOrWhiteSpace(line)) {
                int idx = getIndexOfSeparationCharacter(line);
                if (idx != -1) {
                	String key= line.substring(0, idx);
                	String value= line.substring(idx + 1);
                    fKeyValuePairs.add(new KeyValuePairModell(key, value, offset, leadingWhiteSpaces));
                    leadingWhiteSpaces = 0;
                }
            } else {
                leadingWhiteSpaces += line.length();
            }
            offset += line.length();
            line = reader.readLine();
        }
        int lastLine= document.getNumberOfLines() - 1;
        boolean needsNewLine= false;
        try {
        	needsNewLine= !(document.getLineLength(lastLine) == 0);
		} catch (BadLocationException ignore) {
			// treat last line having no new line
		}
        LastKeyValuePair lastKeyValuePair = new LastKeyValuePair(offset, needsNewLine);
		fKeyValuePairs.add(lastKeyValuePair);
    }

    private int getIndexOfSeparationCharacter(String line) {
        int minIndex = -1;
        int indexOfEven = line.indexOf('=');
        int indexOfColumn = line.indexOf(':');
        int indexOfBlank = line.indexOf(' ');

        if ((indexOfEven != -1) && (indexOfColumn != -1)) {
            minIndex = Math.min(indexOfEven, indexOfColumn);
        } else {
            minIndex = Math.max(indexOfEven, indexOfColumn);
        }

        if ((minIndex != -1) && (indexOfBlank != -1)) {
            minIndex = Math.min(minIndex, indexOfBlank);
        } else {
            minIndex = Math.max(minIndex, indexOfBlank);
        }

        return minIndex;
    }

    public static String unwindEscapeChars(String s){
		StringBuffer sb= new StringBuffer(s.length());
		int length= s.length();
		for (int i= 0; i < length; i++){
			char c= s.charAt(i);
			sb.append(getUnwoundString(c));
		}
		return sb.toString();
	}

	public static String unwindValue(String value) {
		return escapeLeadingWhiteSpaces(escapeCommentChars(unwindEscapeChars(value)));
	}

	private static String getUnwoundString(char c){
	        	switch(c){
	        		case '\b' :
	        			return "\\b";//$NON-NLS-1$
	        		case '\t' :
	        			return "\\t";//$NON-NLS-1$
	        		case '\n' :
	        			return "\\n";//$NON-NLS-1$
	        		case '\f' :
	        			return "\\f";//$NON-NLS-1$
	        		case '\r' :
	        			return "\\r";//$NON-NLS-1$

//      			These can be used unescaped in properties file:
//      			case '\"' :
//      			return "\\\"";//$NON-NLS-1$
//      			case '\'' :
//      			return "\\\'";//$NON-NLS-1$

	        		case '\\' :
	        			return "\\\\";//$NON-NLS-1$

//      			This is only done when writing to the .properties file in #unwindValue(String)
//      			case '!':
//      			return "\\!";//$NON-NLS-1$
//      			case '#':
//      			return "\\#";//$NON-NLS-1$

	        		default:
	        			if (((c < 0x0020) || (c > 0x007e))){
	        				return new StringBuffer()
							.append('\\')
							.append('u')
							.append(toHex((c >> 12) & 0xF))
							.append(toHex((c >>  8) & 0xF))
							.append(toHex((c >>  4) & 0xF))
							.append(toHex( c        & 0xF)).toString();

	        			} else
	        				return String.valueOf(c);
	        	}
	        }

	private static char toHex(int halfByte) {
		return HEX_DIGITS[(halfByte & 0xF)];
	}

	private static String escapeCommentChars(String string) {
	    StringBuffer sb = new StringBuffer(string.length() + 5);
	    for (int i = 0; i < string.length(); i++) {
	      char c = string.charAt(i);
	      switch (c) {
	      case '!':
	        sb.append("\\!"); //$NON-NLS-1$
	        break;
	      case '#':
	        sb.append("\\#"); //$NON-NLS-1$
	        break;
	      default:
	        sb.append(c);
	      }
	    }
	    return sb.toString();
	}

	private static String escapeLeadingWhiteSpaces(String str) {
		int firstNonWhiteSpace= findFirstNonWhiteSpace(str);
		StringBuffer buf= new StringBuffer(firstNonWhiteSpace);
		for (int i = 0; i < firstNonWhiteSpace; i++) {
			buf.append('\\');
		    buf.append(str.charAt(i));
		}
		buf.append(str.substring(firstNonWhiteSpace));
		return buf.toString();
	}

	/**
	 * @param s the string to inspect
	 * @return the first non whitespace character, the length if only whitespace characters
	 */
	private static int findFirstNonWhiteSpace(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i)))
				return i;
		}
		return s.length();
	}

	private static class KeyValuePairModell extends KeyValuePair {

        int fOffset;
        int fLeadingWhiteSpaces;

        public KeyValuePairModell(String key, String value, int offset, int leadingWhiteSpaces) {
            super(key, value);
            fOffset = offset;
            fLeadingWhiteSpaces = leadingWhiteSpaces;
        }

        public KeyValuePairModell(KeyValuePair keyValuePair) {
            super(keyValuePair.fKey, keyValuePair.fValue);
        }

        private String getKeyValueText() {
			return fKey + '=' + fValue;
        }
    }

    /**
     * anchor element for a list of KeyValuePairs. (it is greater than every
     * other KeyValuePair)
     */
    private static class LastKeyValuePair extends KeyValuePairModell {

    	private boolean fNeedsNewLine;

        public LastKeyValuePair(int offset, boolean needsNewLine) {
            super("zzzzzzz", "key", offset, 0); //$NON-NLS-1$ //$NON-NLS-2$
            fNeedsNewLine= needsNewLine;
        }
        public boolean needsNewLine() {
        	return fNeedsNewLine;
        }
        public void resetNeedsNewLine() {
        	fNeedsNewLine= false;
        }
    }
}
