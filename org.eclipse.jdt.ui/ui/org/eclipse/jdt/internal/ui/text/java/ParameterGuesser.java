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
package org.eclipse.jdt.internal.ui.text.java;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
 
/**
 * This class triggers a code-completion that will track all local ane member variables for later
 * use as a parameter guessing proposal.
 * 
 * @author Andrew McCullough
 */
public class ParameterGuesser {

	private static final class Variable {

		/**
		 * Variable type. Used to choose the best guess based on scope (Local beats instance beats inherited)
		 */
		public static final int LOCAL= 0;	
		public static final int FIELD= 1;
		public static final int INHERITED_FIELD= 2;

		public final String typePackage;
		public final String typeName;
		public final String name;
		public final int variableType;
		public final int positionScore;
		public boolean alreadyMatched;
		
		/**
		 * Creates a variable.
		 */
		public Variable(String typePackage, String typeName, String name, int variableType, int positionScore) {
			this.typePackage= typePackage;
			this.typeName= typeName;
			this.name= name;
			this.variableType= variableType;
			this.positionScore= positionScore;
		}

		/*
		 * @see Object#toString()
		 */
		public String toString() {

			StringBuffer buffer= new StringBuffer();

			if (typePackage.length() != 0) {
				buffer.append(typePackage);
				buffer.append('.');
			}

			buffer.append(typeName);
			buffer.append(' ');
			buffer.append(name);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(variableType);
			buffer.append(')');

			return buffer.toString();
		}
	}

	private static final class VariableCollector extends CompletionRequestorAdapter {

		/** The enclosing type name */
		private String fEnclosingTypeName;
		/** The local and member variables */
		private List fVariables;

		public List collect(int codeAssistOffset, ICompilationUnit compilationUnit) throws JavaModelException {	
			Assert.isTrue(codeAssistOffset >= 0);
			Assert.isNotNull(compilationUnit);
	
			fVariables= new ArrayList();			
					
			String source= compilationUnit.getSource();
			if (source == null)
				return fVariables;
					
			fEnclosingTypeName= getEnclosingTypeName(codeAssistOffset, compilationUnit);
	
			// find some whitepace to start our variable-finding code complete from.
			// this allows the VariableTracker to find all available variables (no prefix to match for the code completion)				
			int completionOffset= getCompletionOffset(source, codeAssistOffset);
			
			compilationUnit.codeComplete(completionOffset, this);
			
			return fVariables;
		}

		private static String getEnclosingTypeName(int codeAssistOffset, ICompilationUnit compilationUnit) throws JavaModelException {

			IJavaElement element= compilationUnit.getElementAt(codeAssistOffset);
			if (element == null)
				return null;
				
			element= element.getAncestor(IJavaElement.TYPE);
			if (element == null)
				return null;
				
			return element.getElementName();		
		}

		private static int getCompletionOffset(String source, int start) {
			int index= start;
			while (index > 0 && !Character.isWhitespace(source.charAt(index - 1)))
				index--;
			return index;
		}

		/**
		 * Determine if the declaring type matches the type of the code completion invokation
		 */
		private final boolean isInherited(String declaringTypeName) {
			return !declaringTypeName.equals(fEnclosingTypeName);
		}

		private void addVariable(int varType, char[] typePackageName, char[] typeName, char[] name) {
			fVariables.add(new Variable(new String(typePackageName), new String(typeName), new String(name), varType, fVariables.size()));
		}

		/*
		 * @see ICompletionRequestor#acceptField(char[], char[], char[], char[], char[], char[], int, int, int, int)
		 */
		public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
			char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart,
			int completionEnd, int relevance)
		{
			if (!isInherited(new String(declaringTypeName)))
				addVariable(Variable.FIELD, typePackageName, typeName, name);
			else
				addVariable(Variable.INHERITED_FIELD, typePackageName, typeName, name);
		}
	
		/*
		 * @see ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int, int)
		 */
		public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers,
			int completionStart, int completionEnd, int relevance)
		{
			addVariable(Variable.LOCAL, typePackageName, typeName, name);
		}
	}
	
	/** The compilation unit we are computing the completion for */
	private final ICompilationUnit fCompilationUnit;
	/** The code assist offset. */
	private final int fCodeAssistOffset;
	/** Local and member variables of the compilation unit */
	private List fVariables;

	/**
	 * Creates a parameter guesser for compilation unit and offset.
	 * 
	 * @param codeAssistOffset the offset at which to perform code assist
	 * @param compilationUnit the compilation unit in which code assist is performed
	 */
	public ParameterGuesser(int codeAssistOffset, ICompilationUnit compilationUnit) {
		Assert.isTrue(codeAssistOffset >= 0);
		Assert.isNotNull(compilationUnit);
		
		fCodeAssistOffset= codeAssistOffset;
		fCompilationUnit= compilationUnit;
	}
	
	/**
	 * Returns the offset at which code assist is performed.
	 */
	public int getCodeAssistOffset() {
		return fCodeAssistOffset;	
	}
	
	/**
	 * Returns the compilation unit in which code assist is performed.
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;	
	}
	
	/**
	 * Returns the name of the variable/field that best matches the type and name of the argument.
	 * .
	 * @param paramPackage - the package of the parameter we are trying to match
	 * @param paramType - the qualified name of the parameter we are trying to match
	 * @param paramName - the name of the paramater (used to find similarly named matches)
	 * @return returns the name of the best match, or <code>null</code> if no match found
	 */
	public String guessParameterName(String paramPackage, String paramType, String paramName) throws JavaModelException {
		
		if (fVariables == null) {
			VariableCollector variableCollector= new VariableCollector();
			fVariables= variableCollector.collect(fCodeAssistOffset, fCompilationUnit);
		}
		
		List typeMatches= findFieldsMatchingType(fVariables, paramPackage, paramType);
		return chooseBestMatch(typeMatches, paramName);
	}
	
	/**
	 * Determine the best match of all possible type matches.  The input into this method is all 
	 * possible completions that match the type of the argument. The purpose of this method is to
	 * choose among them based on the following simple rules:
	 * 
	 * 	1) Local Variables > Instance/Class Variables > Inherited Instance/Class Variables
	 * 
	 * 	2) A longer case insensitive substring match will prevail
	 * 
	 * 	3) A better source position score will prevail (the declaration point of the variable, or
	 * 		"how close to the point of completion?"
	 * 
	 *  4) Variables that have not been used already during this completion will prevail over 
	 * 		those that have already been used (this avoids the same String/int/char from being passed
	 * 		in for multiple arguments)
	 * 
	 * @return returns <code>null</code> if no match is found
	 */
	private static String chooseBestMatch(List typeMatches, String paramName) {
		
		if (typeMatches == null)
			return null;
		
		Variable bestMatch= null;
		int bestSubstringScore= 0;
		
		for (Iterator i= typeMatches.iterator(); i.hasNext(); ) {
			
			Variable variable= (Variable) i.next();			
			if (variable.alreadyMatched)
				continue;
			
			int subStringScore= getLongestCommonSubstring(variable.name, paramName).length();
	
			if (bestMatch == null) {
				bestMatch= variable;
				bestSubstringScore= subStringScore;
	
			} else if (variable.variableType < bestMatch.variableType) {
				bestMatch= variable;
			
			} else if (subStringScore > bestSubstringScore) {
				bestMatch= variable;
				bestSubstringScore= subStringScore;
				
			} else if (variable.positionScore > bestMatch.positionScore) {
				bestMatch= variable;
				
			} else if (bestMatch.alreadyMatched && !variable.alreadyMatched) {
				bestMatch= variable;
			}
		}		 

		if (bestMatch == null)
			return null;
		
		bestMatch.alreadyMatched= true;
		return bestMatch.name;
	}

	/**
	 * Finds a local or member variable that matched the type of the parameter
	 */
	private List findFieldsMatchingType(List variables, String typePackage, String typeName) throws JavaModelException {
		
		if (typeName == null || typeName.length() == 0)
			return null;
		
		// traverse the lists in reverse order, since it is empirically true that the code
		// completion engine returns variables in the order they are found -- and we want to find
		// matches closest to the codecompletion point.. No idea if this behavior is guaranteed.
		
		List matches= new ArrayList();
		
		for (ListIterator iterator= variables.listIterator(variables.size()); iterator.hasPrevious(); ) {
			Variable variable= (Variable) iterator.previous();
			if (isTypeMatch(variable, typePackage, typeName))
				matches.add(variable);
		}
				
		return matches.isEmpty() ? null : matches;
	}

	/**
	 * Return true if variable is a match for the given type.  This method will search the SuperTypeHierarchy
	 * of the vairable's type and see if the argument's type is included. This method is approximately 
	 * the same as:  if (argumentVar.getClass().isAssignableFrom(field.getClass()))
	 */
	private boolean isTypeMatch(Variable variable, String typePackage, String typeName) throws JavaModelException {
		
		// this should look at fully qualified name, but currently the ComepletionEngine is not
		// sending the packag names...
		
		// if there is no package specified, do the check on type name only.  This will work for primitives
		// and for local variables that cannot be resolved.
		
		if (typePackage == null || variable.typePackage == null || 
			typePackage.length() == 0 || variable.typePackage.length() == 0) {
			
			if (variable.typeName.equals(typeName))
				return true;
		} 
		
		// if we get to here, we're doing a "fully qualified match" -- meaning including packages, no primitives
		// and no unresolved variables.  
			
		// if there is an exact textual match, there is no need to search type hierarchy.. this is
		// a quick way to pick up an exact match.
		if (variable.typeName.equals(typeName) && variable.typePackage.equals(typePackage))
			return true;
		
		// otherwise, we get a match/nomatch by searching the type hierarchy	
		return isAssignable(variable, typePackage, typeName);
	}

	/**
	 * Returns true if variable is assignable to a type, false otherwise.
	 */
	private boolean isAssignable(Variable variable, String typePackage, String typeName) throws JavaModelException {
		
		// check for an exact match (fast)
		StringBuffer paramTypeName= new StringBuffer();
		if (typePackage.length() != 0) {
			paramTypeName.append(typePackage);
			paramTypeName.append('.');
		}
		paramTypeName.append(typeName);

		StringBuffer varTypeName= new StringBuffer();
		if (variable.typePackage.length() != 0) {
			varTypeName.append(variable.typePackage);
			varTypeName.append('.');
		}
		varTypeName.append(variable.typeName);
		
		IJavaProject project= fCompilationUnit.getJavaProject();
		IType paramType= project.findType(paramTypeName.toString());
		IType varType= project.findType(varTypeName.toString());
		if (varType == null || paramType == null)
			return false;

		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(varType);
		return hierarchy.contains(paramType);
	}

	/**
	 * Returns the longest common substring of two strings.
	 */
	private static String getLongestCommonSubstring(String first, String second) {
		
		String shorter= (first.length() <= second.length()) ? first : second;
		String longer= shorter == first ? second : first;

		int minLength= shorter.length();

		StringBuffer pattern= new StringBuffer(shorter.length() + 2);	
		String longestCommonSubstring= ""; //$NON-NLS-1$
		
		for (int i= 0; i < minLength; i++) {
			for (int j= i + 1; j <= minLength; j++) {				
				if (j - i < longestCommonSubstring.length())
					continue;
				
				String substring= shorter.substring(i, j);
				pattern.setLength(0);
				pattern.append('*');
				pattern.append(substring);
				pattern.append('*');
				
				StringMatcher matcher= new StringMatcher(pattern.toString(), true, false);
				if (matcher.match(longer))
					longestCommonSubstring= substring;
			}
		}
	
		return longestCommonSubstring;
	}

}
