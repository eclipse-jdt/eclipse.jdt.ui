package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.ProblemPosition;

public class JavaCorrectionProcessor implements IContentAssistProcessor {

	private CompilationUnitEditor fCompilationUnitEditor;

	/**
	 * Constructor for JavaCorrectionProcessor.
	 */
	public JavaCorrectionProcessor(CompilationUnitEditor editor) {
		fCompilationUnitEditor= editor;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		List problemPositions= fCompilationUnitEditor.getProblemPositions();
		if (problemPositions == null)
			return null;
		

		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(fCompilationUnitEditor.getEditorInput());

		ArrayList proposals= new ArrayList();
			
		for (Iterator e = problemPositions.iterator(); e.hasNext();) {
			ProblemPosition pp = (ProblemPosition) e.next();
			if (pp.overlapsWith(documentOffset, 1)) {
				collectCorrections(cu, pp, proposals);
			}
		}
		if (proposals.isEmpty()) {
			return null;
		}
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
	
	private void collectCorrections(ICompilationUnit cu, ProblemPosition problemPos, ArrayList proposals) {
		try {
			int id= problemPos.getProblem().getID();
			switch (id) {
				case IProblem.UnterminatedString:
					proposals.add(new InsertCharacterCorrectionProposal(cu, problemPos, "Insert missing quote", "\"", false));
					break;
				case IProblem.UnterminatedComment:
					proposals.add(new InsertCharacterCorrectionProposal(cu, problemPos, "Terminate Comment", "*/", false));
					break;
				case IProblem.UndefinedMethod:
					UnknownMethodEvaluator.getProposals(cu, problemPos, proposals);
					break;
				case IProblem.PublicClassMustMatchFileName:
					ReorgEvaluator.getWrongTypeNameProposals(cu, problemPos, proposals);
					break;
				case IProblem.PackageIsNotExpectedPackage:
					ReorgEvaluator.getWrongPackageDeclNameProposals(cu, problemPos, proposals);
					break;
				case IProblem.UndefinedType:
				case IProblem.FieldTypeNotFound:
				case IProblem.ArgumentTypeNotFound:
				case IProblem.ReturnTypeNotFound:
					UnknownTypeEvaluator.getTypeProposals(cu, problemPos, UnknownTypeEvaluator.TYPE, proposals);
					break;
				case IProblem.SuperclassNotFound:
				case IProblem.ExceptionTypeNotFound:
					UnknownTypeEvaluator.getTypeProposals(cu, problemPos, UnknownTypeEvaluator.CLASS, proposals);
					break;				
				case IProblem.InterfaceNotFound: 
					UnknownTypeEvaluator.getTypeProposals(cu, problemPos, UnknownTypeEvaluator.INTERFACE, proposals);
					break;	
				default:
					//proposals.add(new NoCorrectionProposal(problemPos));
			}
		} catch(CoreException e) {
			JavaPlugin.log(e);
		}
	}

	/*
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

}