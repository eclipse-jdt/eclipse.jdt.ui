
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.javaeditor.ProblemPosition;

public class UnknownTypeEvaluator {

	private static final int CLASS= 1;
	private static final int INTERFACE= 2;
	private static final int IMPORTED_ONLY= 4;

		
	private static class SimilarTypesRequestor extends CompletionRequestorAdapter {
		
		private String fTypeName;
		private int fKind;
		private HashSet fResult;
		
		public SimilarTypesRequestor(String typeName, int kind, HashSet result) {
			fTypeName= typeName;
			fResult= result;
			fKind= kind;
		}
		
		/*
		 * @see ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
		 */
		private void addType(char[] packageName, char[] typeName, char[] completionName) {
			StringBuffer buf= new StringBuffer();
			if (packageName.length > 0) {
				buf.append(packageName);
				buf.append('.');
			}
			buf.append(typeName);
			String qualifiedName= buf.toString();
			if (fResult.contains(qualifiedName)) {
				return;
			}
			if (NameMatcher.isSimilarName(new String(typeName), fTypeName)) {
				if ((fKind & IMPORTED_ONLY) == 0 || qualifiedName.equals(new String(completionName))) {
					fResult.add(qualifiedName);
				}
			}
		}
		
		/*
		 * @see ICompletionRequestor#acceptClass(char[], char[], char[], int, int, int)
		 */
		public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd) {
			if ((fKind & CLASS) != 0) {
				addType(packageName, className, completionName);
			}
		}

		/*
		 * @see ICompletionRequestor#acceptInterface(char[], char[], char[], int, int, int)
		 */
		public void acceptInterface(char[] packageName, char[] interfaceName, char[] completionName, int modifiers, int completionStart, int completionEnd) {
			if ((fKind & INTERFACE) != 0) {
				addType(packageName, interfaceName, completionName);
			}
		}

	}
	
	
	public static void getTypeProposals(ICompilationUnit cu, ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		IProblem problem= problemPos.getProblem();
		String[] args= problem.getArguments();
		if (args.length < 1) {
			return;
		}
		
		// corrections
		String typeName= args[0];
		
		HashSet result= new HashSet();
		SimilarTypesRequestor requestor= new SimilarTypesRequestor(typeName, CLASS | INTERFACE | IMPORTED_ONLY, result);
		cu.codeComplete(problemPos.getOffset() + 1, requestor);
		
		Iterator iter= result.iterator();
		while (iter.hasNext()) {
			String curr= (String) iter.next();
			String label= "Change to " + curr + "(...)";
			proposals.add(new ReplaceCorrectionProposal(cu, problemPos, label, curr));
		}
		
		// add import
		
				
	}
}