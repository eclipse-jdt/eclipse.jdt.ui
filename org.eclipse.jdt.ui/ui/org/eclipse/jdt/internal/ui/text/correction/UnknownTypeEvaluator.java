
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


public class UnknownTypeEvaluator {

	public static final int CLASS= 1;
	public static final int INTERFACE= 2;
	public static final int IMPORTED_ONLY= 4;
	
	public static final int TYPE= CLASS | INTERFACE;
	public static final int TYPE_IMP= CLASS | INTERFACE | IMPORTED_ONLY;
		
	private static class SimilarTypesRequestor extends CompletionRequestorAdapter {
		
		private String fTypeName;
		private int fKind;
		private HashSet fResult;
		private HashSet fOthers;
		
		public SimilarTypesRequestor(String typeName, int kind) {
			fTypeName= typeName;
			fResult= new HashSet();
			fOthers= new HashSet();
			fKind= kind;
		}
		
		public HashSet getResults() {
			if (fResult.size() == 0) {
				if (fOthers.size() < 6) {
					return fOthers;
				}
			}
			return fResult;
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
			if ("java.util.List".equals(qualifiedName)) {
				buf.append("a");
			}
			
			if (NameMatcher.isSimilarName(fTypeName, new String(typeName))) {
				if ((fKind & IMPORTED_ONLY) == 0 || qualifiedName.equals(new String(completionName))) {
					fResult.add(qualifiedName);
					return;
				}
			}
			fOthers.add(qualifiedName);
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
	
	
	public static void getTypeProposals(ICompilationUnit cu, ProblemPosition problemPos, int kind, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length < 1) {
			return;
		}
		
		// corrections
		String typeName= cu.getBuffer().getText(problemPos.getOffset(), problemPos.getLength());
		
		
		SimilarTypesRequestor requestor= new SimilarTypesRequestor(typeName, kind);
		cu.codeComplete(problemPos.getOffset() + 1, requestor);
		HashSet result= requestor.getResults();
	
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		
		Iterator iter= result.iterator();
		while (iter.hasNext()) {
			String curr= (String) iter.next();
			
			ImportEdit importEdit= new ImportEdit(cu, settings);
			importEdit.addImport(curr);
			
			String simpleName= Signature.getSimpleName(curr);
			boolean importOnly= simpleName.equals(typeName);
			
			CUCorrectionProposal proposal= new CUCorrectionProposal("", cu, problemPos);
			proposals.add(proposal);
			
			CompilationUnitChange change= proposal.getCompilationUnitChange();
			
			if (!importEdit.isEmpty()) {
				change.addTextEdit("Add Import", importEdit);
			}
			if (!importOnly) {
				change.addTextEdit("Change", SimpleTextEdit.createReplace(problemPos.getOffset(), problemPos.getLength(), simpleName));
				proposal.setDisplayName("Change to '" + simpleName + "'");
			} else {
				proposal.setDisplayName("Import '" + curr + "'");
			}
		}
				
	}
}