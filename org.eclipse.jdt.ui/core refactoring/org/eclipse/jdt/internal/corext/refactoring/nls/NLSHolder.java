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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;


/**
 * This class is responsible for creating and storing <code>NLSSubstitution</code> and 
 * <code>NLSLine</code> elements for a given <code>ICompilationUnit</code>.
 */
public class NLSHolder {

    private static final char[] HEX_DIGITS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	private static final char SUBSTITUTE_CHAR= '_';	
	private static final char[] UNWANTED_CHARS= new char[]{' ', ':', '"', '\\', '\'', '?', '='};
	public static final String[] UNWANTED_STRINGS= {" ", ":", "\"", "\\", "'", "?", "="}; //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

	private NLSSubstitution[] fSubstitutions;
	private NLSLine[] fLines;
	private ICompilationUnit fCu;
	
	//clients create instances by using the factory method
	private NLSHolder(NLSSubstitution[] substitutions, NLSLine[] lines, ICompilationUnit cu) {
		fSubstitutions= substitutions;
		fLines= lines;
		fCu= cu;
	}

	public static NLSHolder create(ICompilationUnit cu){
		NLSLine[] nlsLines= createRawLines(cu);
		NLSSubstitution[] subs= processLines(nlsLines);
		return new NLSHolder(subs, nlsLines, cu);
	}

	public NLSSubstitution[] getSubstitutions(){
		return fSubstitutions;
	}
	
	public NLSLine[] getLines(){
		return fLines;
	}
	
	public ICompilationUnit getCu(){
		return fCu;
	}
	
	private static NLSLine[] createRawLines(ICompilationUnit cu){
		try {
			return NLSScanner.scan(cu);
		} catch (JavaModelException x) {
			return null;
		} catch (InvalidInputException x) {
			return null;
		}		
	}
	
	//modifies its parameter
	private static NLSSubstitution[] processLines(NLSLine[] lines) {
		if (lines == null) 
			return new NLSSubstitution[0];
		List result= new ArrayList();
		int counter= 1;
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for(int j= 0; j < elements.length; j++){
				NLSElement element= elements[j];
				if (element.hasTag()) //don't show nls'ed stuff
					continue;
				element.setValue(createModifiedValue(element.getValue()));
				result.add(new NLSSubstitution(createKey(element, counter++), element, NLSSubstitution.DEFAULT));
			}
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}
	
	private static String createModifiedValue(String rawValue){
		String modifiedValue= NLSRefactoring.removeQuotes(rawValue);
		
		modifiedValue= unwindEscapeChars(modifiedValue);
		
		return "\"" + modifiedValue + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private static String unwindEscapeChars(String s){
		StringBuffer sb= new StringBuffer(s.length());
		int last= s.length() - 1;
		int length= s.length();
		for (int i= 0; i < length; i++){
			char c= s.charAt(i);
			if ((c == '\"') && (i == 0 || i == last)) //the first and last " should not be converted to \"
				sb.append(c);
			else	
				sb.append(getUnwoundString(c));
		}
		return sb.toString();
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
			case '\"' :
				return "\\\"";//$NON-NLS-1$
			case '\'' :
				return "\\\'";//$NON-NLS-1$
			case '\\' :
				return "\\\\";//$NON-NLS-1$
			case '!':
				return "\\!";//$NON-NLS-1$
			case '#':
				return "\\#";//$NON-NLS-1$
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
	
	private static String createKey(NLSElement element, int counter){
		String result= NLSRefactoring.removeQuotes(element.getValue());
		for (int i= 0; i < UNWANTED_CHARS.length; i++)
			result= result.replace(UNWANTED_CHARS[i], SUBSTITUTE_CHAR);
		return result + '_' + counter;
	}	
}
