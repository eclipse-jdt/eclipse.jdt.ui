/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.nls.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;

/**
 * This class is responsible for creating and storing <code>NLSSubstitution</code> and 
 * <code>NLSLine</code> elements for a given <code>ICompilationUnit</code>.
 */
public class NLSHolder {

	private static final char SUBSTITUTE_CHAR= '_';	
	private static final char[] UNWANTED_CHARS= new char[]{' ', ':', '"', '\\', '\'', '?'};
	public static final String[] UNWANTED_STRINGS= {" ", ":", "\"", "\\", "'", "?"}; //$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

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
		List lines= createRawLines(cu);
		NLSSubstitution[] subs= processLines(lines);
		NLSLine[] nlsLines = (NLSLine[]) lines.toArray(new NLSLine[lines.size()]);
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
	
	private static List createRawLines(ICompilationUnit cu){
		try {
			return NLSScanner.scan(cu);
		} catch (JavaModelException x) {
			return null;
		} catch (InvalidInputException x) {
			return null;
		}		
	}
	
	//modifies its parameter
	private static NLSSubstitution[] processLines(List lines) {
		if (lines == null) 
			return new NLSSubstitution[0];
		List result= new ArrayList();
		int counter= 1;
		for (Iterator e= lines.iterator(); e.hasNext(); ) {
			NLSElement[] elements= ((NLSLine) e.next()).getElements();
			for(int i= 0; i < elements.length; i++){
				NLSElement element= elements[i];
				if (element.hasTag()) //don't show nls'ed stuff
					continue;
				element.setValue(unwindEscapeChars(element.getValue()));
				result.add(new NLSSubstitution(createKey(element, counter++), element, NLSSubstitution.DEFAULT));
			}
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}
	
	private static String unwindEscapeChars(String s){
		StringBuffer sb= new StringBuffer(s.length());
		int last= s.length() - 1;
		for (int i= 0; i < s.length(); i++){
			char c= s.charAt(i);
			if (i == 0 || i == last) //the first and last " should not be converted to \"
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
			default: 
				return String.valueOf(c);
		}		
	}
	
	private static String createKey(NLSElement element, int counter){
		String result= NLSRefactoring.removeQuotes(element.getValue());
		for (int i= 0; i < UNWANTED_CHARS.length; i++)
			result= result.replace(UNWANTED_CHARS[i], SUBSTITUTE_CHAR);
		return result + '_' + counter;
	}	
}