/*****************************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;

public class PropertyFileDocumentModell {

    private List fKeyValuePairs;

    public PropertyFileDocumentModell(IDocument document) {
        parsePropertyDocument(document);
    }

    public InsertEdit insert(String key, String value) {
        return insert(new KeyValuePair(key, value));
    }

    public InsertEdit insert(KeyValuePair keyValuePair) {
        return getAdjustetInsertEdit(findInsertPosition(keyValuePair), 
                new KeyValuePairModell(keyValuePair));
    }

    public InsertEdit[] insert(KeyValuePair[] keyValuePairs) {
        return insertionSort(keyValuePairs);        
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
    
    private int findInsertPosition(KeyValuePair keyValuePair) {
        int insertIndex = 0;
        while (((KeyValuePairModell) fKeyValuePairs.get(insertIndex)).compareTo(keyValuePair) < 0) {
            insertIndex++;
        }
        return insertIndex;
    }

    private InsertEdit[] insertionSort(KeyValuePair[] newKeyValuePairs) {
        KeyValuePairModell[] newKeyValuePairModell = new KeyValuePairModell[newKeyValuePairs.length];
        for (int i = 0; i < newKeyValuePairs.length; i++) {
            newKeyValuePairModell[i] = new KeyValuePairModell(newKeyValuePairs[i]);
        }
        
        Arrays.sort(newKeyValuePairModell);
        InsertEdit[] inserts = new InsertEdit[newKeyValuePairs.length];
        int insertIndex = 0;
        for (int i = 0; i < newKeyValuePairs.length; i++) {
            KeyValuePairModell subsKeyValuePair = newKeyValuePairModell[i];
            while (((KeyValuePairModell) fKeyValuePairs.get(insertIndex)).compareTo(subsKeyValuePair) < 0) {
                insertIndex++;
            }
            inserts[i] = getAdjustetInsertEdit(insertIndex, subsKeyValuePair);
        }        
        return inserts;
    }

    private void parsePropertyDocument(IDocument document) {
        fKeyValuePairs = new ArrayList();
        SimpleLineReader reader = new SimpleLineReader(document.get());        
        int offset = 0;
        String line = reader.readLine();
        int leadingWhiteSpaces = 0;
        while (line != null) {
            if (isCommentOrWhiteSpace(line) == false) {
                int idx = getIndexOfSeparationCharacter(line);
                fKeyValuePairs.add(new KeyValuePairModell(line.substring(0, idx), line.substring(idx + 1), offset, leadingWhiteSpaces));
                leadingWhiteSpaces = 0;
            } else {
                leadingWhiteSpaces += line.length();
            }
            offset += line.length();
            line = reader.readLine();
        }
        fKeyValuePairs.add(new LastKeyValuePair(offset));
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

    private boolean isCommentOrWhiteSpace(String line) {
        line = line.trim();
        return (line.length() == 0) || line.startsWith("!") || line.startsWith("#"); //$NON-NLS-1$
    }

    private InsertEdit getAdjustetInsertEdit(int insertIndex, KeyValuePairModell subsKeyValuePair) {
        int offset;
        KeyValuePairModell insertHere = (KeyValuePairModell) fKeyValuePairs.get(insertIndex);
        if (insertIndex == 0) {
            offset = insertHere.fOffset;
        } else {
            KeyValuePairModell insertAfter = (KeyValuePairModell) fKeyValuePairs.get(insertIndex - 1);

            if (subsKeyValuePair.getBetterMatch(insertAfter, insertHere)) {
                offset = insertHere.fOffset - insertHere.fLeadingWhiteSpaces;
            } else {
                offset = insertHere.fOffset;
            }
        }
        return new InsertEdit(offset, subsKeyValuePair.getEncodedText());
    }
    
    private class KeyValuePairModell extends KeyValuePair implements Comparable {        

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

        // TODO encode leading whitespaces !!!
        public String getEncodedText() {
            return unwindEscapeChars(fKey) + '=' + escapeCommentChars(unwindEscapeChars(fValue)) + '\n';
        }
        
        public int compareTo(Object o) {
            KeyValuePair keyValuePair = (KeyValuePair) o;
            return fKey.compareTo(keyValuePair.fKey);
        }

        // TODO bollock match on "this"
        public boolean getBetterMatch(KeyValuePairModell first, KeyValuePairModell second) {
            int points = countPoints(fKey);
            int pointsFirst = countPoints(first.fKey);
            int pointsSecond = countPoints(second.fKey);

            int dif1 = Math.abs(points - pointsFirst);
            int dif2 = Math.abs(points - pointsSecond);

            return dif1 <= dif2;
        }

        private int countPoints(String s) {
            int res = 0;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '.') {
                    res++;
                }
            }
            return res;
        }
        
        private String escapeCommentChars(String string) {
            StringBuffer sb = new StringBuffer(string.length() + 5);
            for (int i = 0; i < string.length(); i++) {
              char c = string.charAt(i);
              switch (c) {
              case '!':
                sb.append("\\!");
                break;
              case '#':
                sb.append("\\#");
                break;
              default:
                sb.append(c);
              }
            }
            return sb.toString();
        }       
        
        // use this to escape "leading" whitespaces
//      private static String escapeEachChar(String s, char escapeChar) {
//      char[] chars = new char[s.length() * 2];
  //
//      for (int i = 0; i < s.length(); i++) {
//        chars[2 * i] = escapeChar;
//        chars[2 * i + 1] = s.charAt(i);
//      }
//      return new String(chars);
//    }

//    /**
//     * returns the length if only whitespaces
//     */
//    private static int findFirstNonWhiteSpace(String s) {
//      for (int i = 0; i < s.length(); i++) {
//        if (!Character.isWhitespace(s.charAt(i)))
//          return i;
//      }
//      return s.length();
//    }
        private String unwindEscapeChars(String s){
        	StringBuffer sb= new StringBuffer(s.length());
        	int length= s.length();
        	for (int i= 0; i < length; i++){
        		char c= s.charAt(i);
        		sb.append(getUnwoundString(c));
        	}
        	return sb.toString();
        }
        
        private String getUnwoundString(char c){
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
//      			This is only done when writing to the .properties file in NLSRefactoring.convertToPropertyValue(.)
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
        
        private char toHex(int halfByte) {
        	return HEX_DIGITS[(halfByte & 0xF)];
        }
        
        private final char[] HEX_DIGITS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    }

    /**
     * anchor element for a list of KeyValuePairs. (it is greater than every
     * other KeyValuePair)
     */
    private class LastKeyValuePair extends KeyValuePairModell {

        public LastKeyValuePair(int offset) {
            super("last", "key", offset, 0); //$NON-NLS-1$ //$NON-NLS-2$
        }

        public int compareTo(Object o) {
            return 1;
        }

        public boolean isBetter(Object o) {
            return true;
        }
    }
}