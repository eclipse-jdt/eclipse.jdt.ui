package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CleanUpRefactoringWizard;
import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;

/**
 * A correction proposal which uses an @see org.eclipse.jdt.internal.corext.fix.IFix to
 * fix a problem. A fix correction proposal may have an @see org.eclipse.jdt.internal.corext.fix.IMultiFix
 * attachet which can be executed instead of the provided IFix.
 */
public class FixCorrectionProposal extends CUCorrectionProposal implements ICompletionProposalExtension2 {

	private final IFix fFix;
	private final IMultiFix fMultiFix;

	public FixCorrectionProposal(IFix fix, IMultiFix multiFix, int relevance, Image image) {
		super(fix.getDescription(), fix.getCompilationUnit(), relevance, image);
		fFix= fix;
		fMultiFix= multiFix;
	}
	
	public IFix getFix() {
		return fFix;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createTextChange()
	 */
	protected TextChange createTextChange() throws CoreException {
		return getFix().createChange();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		if (stateMask == SWT.CONTROL && fMultiFix != null){
			CleanUpRefactoring refactoring= new CleanUpRefactoring();
			refactoring.addCompilationUnit(getCompilationUnit());
			refactoring.addProblemSolution(fMultiFix);
			
			CleanUpRefactoringWizard refactoringWizard= new CleanUpRefactoringWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE, true, false);
			
			RefactoringStarter starter= new RefactoringStarter();
			try {
				starter.activate(refactoring, refactoringWizard, JavaPlugin.getActiveWorkbenchShell(), "Clean ups", true);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return;
		}
		apply(viewer.getDocument());
	}

	public void selected(ITextViewer viewer, boolean smartToggle) {}

	public void unselected(ITextViewer viewer) {}

	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

}
