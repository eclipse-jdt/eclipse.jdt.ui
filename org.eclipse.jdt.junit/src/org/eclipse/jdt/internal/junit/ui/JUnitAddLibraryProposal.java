/*
 * Created on Jul 16, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.wizards.NewTestCaseCreationWizardPage;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * @author egamma
 */
final class JUnitAddLibraryProposal implements IJavaCompletionProposal {
	private final IInvocationContext fContext;

	public JUnitAddLibraryProposal(IInvocationContext context) {
		fContext= context;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return 0;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {   
		IJavaProject project= fContext.getCompilationUnit().getJavaProject();
		try {
			NewTestCaseCreationWizardPage.addJUnitToBuildPath(JUnitPlugin.getActiveWorkbenchShell(), project);
			// force a reconcile
			int offset= fContext.getSelectionOffset();
			int length= fContext.getSelectionLength();
			String s= document.get(offset, length);
			document.replace(offset, length, s);
		} catch (JavaModelException e) {
			ErrorDialog.openError(JUnitPlugin.getActiveWorkbenchShell(), JUnitMessages.getString("JUnitAddLibraryProposal.title"), JUnitMessages.getString("JUnitAddLibraryProposal.cannotAdd"), e.getStatus());  //$NON-NLS-1$ //$NON-NLS-2$

		} catch (BadLocationException e) {
			//ignore
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fContext.getSelectionOffset(), fContext.getSelectionLength());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return JUnitMessages.getString("JUnitAddLibraryProposal.info"); //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return JUnitMessages.getString("JUnitAddLibraryProposal.label"); //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}
}