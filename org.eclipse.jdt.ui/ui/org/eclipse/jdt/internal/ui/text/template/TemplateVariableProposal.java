package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TemplateVariableProposal implements ICompletionProposal {

	private String fName;
	private String fDescription;
	private int fOffset;
	private int fLength;	
	private ITextViewer fViewer;
	
	private Point fSelection;

	public TemplateVariableProposal(String name, String description, int offset, int length, ITextViewer viewer) {
		fName= name;
		fDescription= description;
		fOffset= offset;
		fLength= length;
		fViewer= viewer;
	}
	
	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {

		try {
			String variable= "${" + fName + '}'; //$NON-NLS-1$
			document.replace(fOffset, fLength, variable);
			fSelection= new Point(fOffset + variable.length(), 0);

		} catch (BadLocationException e) {
			JavaPlugin.log(e);

			Shell shell= fViewer.getTextWidget().getShell();
			MessageDialog.openError(shell, TemplateMessages.getString("TemplateVariableProposal.error.title"), e.getMessage()); //$NON-NLS-1$
		}
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return fSelection;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return null;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fName + " - " + fDescription; //$NON-NLS-1$
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

}

