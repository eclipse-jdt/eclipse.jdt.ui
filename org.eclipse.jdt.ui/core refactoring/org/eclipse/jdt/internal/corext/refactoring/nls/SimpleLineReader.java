/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

/**
 * Simple LineReader Helper. Returns lines including "line-break" characters.
 */
public class SimpleLineReader {

    private String fInput;
    private int fIndex;

    public SimpleLineReader(String input) {
        this.fInput = input;
    }    
    
    public String readLine() { 
        if (fIndex >= fInput.length()) {
            return null;
        }
        
        int lineBreakIndex = -1;
        int lineBreakIndexLF = fInput.indexOf('\n', fIndex);
        int lineBreakIndexCR = fInput.indexOf('\r', fIndex);       
        
        if ((lineBreakIndexCR) != -1 && (lineBreakIndexLF != -1)) {
            lineBreakIndex = Math.min(lineBreakIndexLF, lineBreakIndexCR);
        } else {
            lineBreakIndex = Math.max(lineBreakIndexCR, lineBreakIndexLF);
        }
        
        if ((lineBreakIndex != -1) &&
                (lineBreakIndex < fInput.length() - 1) && 
                (fInput.charAt(lineBreakIndex) == '\r') && 
                (fInput.charAt(lineBreakIndex+1) == '\n')) {
            lineBreakIndex++;
        }        
        
        if (lineBreakIndex == -1) {
            String res = fInput.substring(fIndex);
            fIndex=fInput.length();
            return res;
        }
        lineBreakIndex++;
        String res = fInput.substring(fIndex, lineBreakIndex);
        fIndex = lineBreakIndex;
        return res;        
    }
}
