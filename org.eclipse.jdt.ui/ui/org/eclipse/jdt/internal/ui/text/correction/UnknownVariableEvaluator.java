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
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class UnknownVariableEvaluator {
		
	private static class SimilarVariableRequestor extends CompletionRequestorAdapter {
		
		private String fVariableName;
		private HashSet fResult;
		
		public SimilarVariableRequestor(String variableName) {
			fVariableName= variableName;
			fResult= new HashSet();
		}


		private void addVariable(char[] name) {
			String variableName= new String(name);
			if (!fResult.contains(variableName)) {
				if (NameMatcher.isSimilarName(fVariableName, variableName)) {
					fResult.add(variableName);
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see ICompletionRequestor#acceptField(char[], char[], char[], char[], char[], char[], int, int, int)
		 */
		public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd) {
			addVariable(name);
		}

		/* (non-Javadoc)
		 * @see ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int)
		 */
		public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int completionStart, int completionEnd) {
			addVariable(name);
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
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		String variableName= cu.getBuffer().getText(problemPos.getOffset(), problemPos.getLength());

		if (variableName.indexOf('.') != -1) {
			return;
		}

		// corrections
		SimilarVariableRequestor requestor= new SimilarVariableRequestor(variableName);
		cu.codeComplete(problemPos.getOffset(), requestor);
		
		HashSet result= requestor.getResult();
		
		Iterator iter= result.iterator();
		while (iter.hasNext()) {
			String curr= (String) iter.next();
			String label= "Change to " + curr;
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr));
		}
		
		// new field
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem instanceof IMember) {
			IType parentType= (IType) JavaModelUtil.findElementOfKind(elem, IJavaElement.TYPE);

			if (parentType != null) {
				String label= "Create field " + variableName;
				proposals.add(new NewVariableCompletionProposal(parentType, problemPos, label, variableName));
			}
		}
	}
}