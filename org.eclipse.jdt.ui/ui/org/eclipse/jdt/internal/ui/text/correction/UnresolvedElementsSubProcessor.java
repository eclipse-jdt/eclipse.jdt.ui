package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnresolvedElementsSubProcessor {
	
	
	public static void getVariableProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		IBuffer buf= cu.getBuffer();
		String variableName= buf.getText(problemPos.getOffset(), problemPos.getLength());
		if (variableName.indexOf('.') != -1) {
			return;
		}

		// corrections
		int kind= SimilarElementsRequestor.VARIABLES;
		SimilarElementsRequestor requestor= new SimilarElementsRequestor(variableName, kind, null, null);
		cu.codeComplete(problemPos.getOffset(), requestor);
		
		HashSet result= requestor.getResults();
		
		Iterator iter= result.iterator();
		while (iter.hasNext()) {
			SimilarElement curr= (SimilarElement) iter.next();
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changevariable.description", curr.getName()); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr.getName(), 3));
		}
		
		// new field
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem instanceof IMember) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.description", variableName); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal((IMember) elem, problemPos, label, NewVariableCompletionProposal.FIELD, variableName, 2));
		}
		if (elem instanceof IMethod) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createlocal.description", variableName); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal((IMember) elem, problemPos, label, NewVariableCompletionProposal.LOCAL, variableName, 1));
		
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createparameter.description", variableName); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal((IMember) elem, problemPos, label, NewVariableCompletionProposal.PARAM, variableName, 1));
		}			
		
		try {
			IScanner scanner= ASTResolving.createScanner(cu, problemPos.getOffset() + problemPos.getLength());
			if (scanner.getNextToken() == ITerminalSymbols.TokenNameDOT) {
				getTypeProposals(problemPos, SimilarElementsRequestor.TYPES, proposals);
			}
		} catch (InvalidInputException e) {
		}
			
	}
	
	public static void getTypeProposals(ProblemPosition problemPos, int kind, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length < 1) {
			return;
		}
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		// corrections
		String typeName= cu.getBuffer().getText(problemPos.getOffset(), problemPos.getLength());
		
		SimilarElementsRequestor requestor= new SimilarElementsRequestor(typeName, kind, null, null);
		cu.codeComplete(problemPos.getOffset() + 1, requestor);
		HashSet result= requestor.getResults();
	
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		
		Iterator iter= result.iterator();
		while (iter.hasNext()) {
			SimilarElement elem= (SimilarElement) iter.next();
			String curr= elem.getName();
			
			ImportEdit importEdit= new ImportEdit(cu, settings);
			importEdit.addImport(curr);
			
			String simpleName= Signature.getSimpleName(curr);
			boolean importOnly= simpleName.equals(typeName);
			
			CUCorrectionProposal proposal= new CUCorrectionProposal("", cu, 0); //$NON-NLS-1$
			proposals.add(proposal);
			
			CompilationUnitChange change= proposal.getCompilationUnitChange();
			
			if (!importEdit.isEmpty()) {
				change.addTextEdit("Add Import", importEdit); //$NON-NLS-1$
			}
			if (!importOnly) {
				change.addTextEdit("Change", SimpleTextEdit.createReplace(problemPos.getOffset(), problemPos.getLength(), simpleName)); //$NON-NLS-1$
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", simpleName)); //$NON-NLS-1$
				proposal.setRelevance(3);
			} else {
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importtype.description", curr)); //$NON-NLS-1$
				proposal.setRelevance(5);
			}
		}
				
	}
	
	public static void getMethodProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length < 3) {
			return;
		}
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		// corrections
		String methodName= args[1];
		String[] arguments= getArguments(args[2]);
		
		SimilarElementsRequestor requestor= new SimilarElementsRequestor(methodName, SimilarElementsRequestor.METHODS, arguments, null);
		cu.codeComplete(problemPos.getOffset(), requestor);
		
		HashSet result= requestor.getResults();
		
		Iterator iter= result.iterator();
		while (iter.hasNext()) {
			SimilarElement elem= (SimilarElement) iter.next();
			String curr= elem.getName();
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changemethod.description", curr); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr, 2));
		}
		
		// new method
		String typeName= args[0];
		IType type= JavaModelUtil.findType(cu.getJavaProject(), typeName);
		if (type != null && type.getCompilationUnit() != null) {
			ICompilationUnit changedCU= type.getCompilationUnit();
			if (!changedCU.isWorkingCopy()) {
				changedCU= EditorUtility.getWorkingCopy(changedCU);
				if (changedCU == null) {
					// not yet supported, waiting for new working copy support
					return;
				}					
				type= (IType) JavaModelUtil.findMemberInCompilationUnit(changedCU, type);
				if (type == null) {
					return; // type does not exist in working copy
				}
			}
			String label;
			if (cu.equals(changedCU)) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.description", methodName); //$NON-NLS-1$
			} else {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", new Object[] { methodName, type.getElementName() } ); //$NON-NLS-1$
			}
			proposals.add(new NewMethodCompletionProposal(type, problemPos, label, methodName, arguments, 1));
		}
	}
	
	private static String[] getArguments(String signature) {
		StringTokenizer tok= new StringTokenizer(signature, ","); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken().trim();
		}
		return res;
	}	


}
