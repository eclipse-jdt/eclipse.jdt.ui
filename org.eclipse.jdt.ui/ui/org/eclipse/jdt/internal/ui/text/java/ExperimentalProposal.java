/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;
import org.eclipse.jdt.internal.ui.text.template.*;

/**
 * An experimental proposal.
 */
public class ExperimentalProposal implements ICompletionProposal {

	private String fReplacementString;
	private int fReplacementOffset;
	private int fReplacementLength;
	private Image fImage;
	private String fDisplayString;
	private int[] fPositionOffsets;
	private int[] fPositionLengths;
	private ITextViewer fViewer;

	private IRegion fSelectedRegion; // initialized by apply()
		
	/**
	 * Creates a template proposal with a template and its context.
	 * @param template  the template
	 * @param context   the context in which the template was requested.
	 * @param image     the icon of the proposal.
	 */		
	public ExperimentalProposal(String replacementString, int replacementOffset, int replacementLength, Image image,
	    String displayString, int[] positionOffsets, int[] positionLengths, ITextViewer viewer)
	{
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementOffset >= 0);
		Assert.isTrue(replacementLength >= 0);
		
		fReplacementString= replacementString;
		fReplacementOffset= replacementOffset;
		fReplacementLength= replacementLength;
		fImage= image;
		fDisplayString= displayString != null ? displayString : replacementString;
		fPositionOffsets= positionOffsets;
		fPositionLengths= positionLengths;
		fViewer= viewer;
	}

	/**
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		try {
			document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
	
			LinkedPositionManager manager= new LinkedPositionManager(document);
			for (int i= 0; i != fPositionOffsets.length; i++)
				manager.addPosition(fReplacementOffset + fPositionOffsets[i], fPositionLengths[i]);
			
			LinkedPositionUI editor= new LinkedPositionUI(fViewer, manager);
			editor.setFinalCaretOffset(fReplacementOffset + fReplacementString.length());
			editor.enter();

			fSelectedRegion= editor.getSelectedRegion();

		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);
		}
	}
	
	/**
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return new Point(fReplacementOffset, 0);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	/**
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return null;		
	}

	/**
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fDisplayString;
	}

	/**
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/**
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	private void openErrorDialog(BadLocationException e) {
		Shell shell= fViewer.getTextWidget().getShell();
		MessageDialog.openError(shell, TemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
	}	

}