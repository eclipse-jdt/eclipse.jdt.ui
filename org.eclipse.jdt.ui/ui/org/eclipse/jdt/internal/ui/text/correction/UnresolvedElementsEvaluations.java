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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnresolvedElementsEvaluations {
	
	
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
			String label= CorrectionMessages.getFormattedString("UnknownVariableEvaluator.change.description", curr.getName());
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr.getName(), 3));
		}
		
		// new field
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem instanceof IMember) {
			IType parentType= (IType) JavaModelUtil.findElementOfKind(elem, IJavaElement.TYPE);

			if (parentType != null) {
				String label= CorrectionMessages.getFormattedString("UnknownVariableEvaluator.create.description", variableName); //$NON-NLS-1$
				proposals.add(new NewVariableCompletionProposal(parentType, problemPos, label, variableName, 2));
			}
		}
		
		try {
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(buf.getCharacters());
			scanner.resetTo(problemPos.getOffset() + problemPos.getLength(), buf.getLength());
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
			
			CUCorrectionProposal proposal= new CUCorrectionProposal("", problemPos, 0); //$NON-NLS-1$
			proposals.add(proposal);
			
			CompilationUnitChange change= proposal.getCompilationUnitChange();
			
			if (!importEdit.isEmpty()) {
				change.addTextEdit("Add Import", importEdit); //$NON-NLS-1$
			}
			if (!importOnly) {
				change.addTextEdit("Change", SimpleTextEdit.createReplace(problemPos.getOffset(), problemPos.getLength(), simpleName)); //$NON-NLS-1$
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnknownTypeEvaluator.change.description", simpleName)); //$NON-NLS-1$
				proposal.setRelevance(3);
			} else {
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnknownTypeEvaluator.import.description", curr)); //$NON-NLS-1$
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
			String label= CorrectionMessages.getFormattedString("UnknownMethodEvaluator.change.description", curr); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr, 2));
		}
		
		// new method
		String typeName= args[0];
		
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem instanceof IMember) {
			IType parentType= (IType) JavaModelUtil.findElementOfKind(elem, IJavaElement.TYPE);
			if (parentType != null && typeName.equals(JavaModelUtil.getFullyQualifiedName(parentType))) {
				String label= CorrectionMessages.getFormattedString("UnknownMethodEvaluator.create.description", methodName); //$NON-NLS-1$
				proposals.add(new NewMethodCompletionProposal(parentType, problemPos, label, methodName, arguments, 1));
			}
		}
	}
	
	private static String[] getArguments(String signature) {
		StringTokenizer tok= new StringTokenizer(signature, ","); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}	


}
