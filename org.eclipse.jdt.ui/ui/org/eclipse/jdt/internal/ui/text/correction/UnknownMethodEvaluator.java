/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class UnknownMethodEvaluator {
		
	private static class SimilarMethodsRequestor extends CompletionRequestorAdapter {
		
		private String fMethodName;
		private int fNumberOfArguments;
		private HashSet fResult;
		
		public SimilarMethodsRequestor(String methodName, int numberOfArguments) {
			fMethodName= methodName;
			fNumberOfArguments= numberOfArguments;
			fResult= new HashSet();
		}
		
		/*
		 * @see ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
		 */
		public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int completionStart, int completionEnd) {
			String methodName= new String(selector);
			if (!fResult.contains(methodName)) {
				if (fNumberOfArguments == parameterTypeNames.length 
					&& NameMatcher.isSimilarName(fMethodName, methodName)) {
						fResult.add(methodName);
				}
			}
		}
		/**
		 * Gets the result.
		 * @return Returns a HashSet
		 */
		public HashSet getResult() {
			return fResult;
		}

	}
	
	
	public static void getProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length < 3) {
			return;
		}
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		// corrections
		String methodName= args[1];
		String[] arguments= getArguments(args[2]);
		int nArguments= arguments.length;
		
		
		SimilarMethodsRequestor requestor= new SimilarMethodsRequestor(methodName, nArguments);
		cu.codeComplete(problemPos.getOffset(), requestor);
		
		HashSet result= requestor.getResult();
		
		Iterator iter= result.iterator();
		while (iter.hasNext()) {
			String curr= (String) iter.next();
			String label= "Change to " + curr + "(...)";
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr));
		}
		
		// new method
		String typeName= args[0];
		
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem instanceof IMember) {
			IType parentType= (IType) JavaModelUtil.findElementOfKind(elem, IJavaElement.TYPE);
			if (parentType != null && typeName.equals(JavaModelUtil.getFullyQualifiedName(parentType))) {
				String label= "Create method " + methodName + "(...)";
				proposals.add(new NewMethodCompletionProposal(parentType, problemPos, label, methodName, arguments));
			}
		}
	}
	
	private static String[] getArguments(String signature) {
		StringTokenizer tok= new StringTokenizer(signature, ",");
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}
}