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
			return new NLSLine[0];
		} catch (InvalidInputException x) {
			return new NLSLine[0];
		}		
	}
	
	//modifies its parameter
	private static NLSSubstitution[] processLines(NLSLine[] lines) {
		List result= new ArrayList();
		int keyCounter= countTaggedElements(lines) + 1;
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for(int j= 0; j < elements.length; j++){
				NLSElement element= elements[j];
				if (element.hasTag()) //don't show nls'ed stuff
					continue;
				String val= element.getValue().substring(1, element.getValue().length() - 1);	// strip the ""
				element.setValue(createModifiedValue(val));
				result.add(new NLSSubstitution(createKey(keyCounter++), element, NLSSubstitution.DEFAULT));
			}
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}
	
	//for editing in wizard
	private static String createModifiedValue(String rawValue){
		return unwindEscapeChars(rawValue);
	}
	
	private static String unwindEscapeChars(String s){
		StringBuffer sb= new StringBuffer(s.length());
		int length= s.length();
		for (int i= 0; i < length; i++){
			char c= s.charAt(i);
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
//These can be used unescaped in properties file:
//			case '\"' :
//				return "\\\"";//$NON-NLS-1$
//			case '\'' :
//				return "\\\'";//$NON-NLS-1$
			case '\\' :
				return "\\\\";//$NON-NLS-1$
//This is only done when writing to the .properties file in NLSRefactoring.convertToPropertyValue(.)
//			case '!':
//				return "\\!";//$NON-NLS-1$
//			case '#':
//				return "\\#";//$NON-NLS-1$
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
	
	private static String createKey(int keyCounter){
		return String.valueOf(keyCounter);
	}	

	private static int countTaggedElements(NLSLine[] nlsLines) {
		int count= -1;
		for (int i= 0; i < nlsLines.length; i++) {
			NLSElement[] elements= nlsLines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				if (elements[j].hasTag())
					count++;
			}
		}
		return count;
	}
}
