/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedEnvironment;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedUIControl;

import org.eclipse.ui.texteditor.link.EditorHistoryUpdater;

import org.eclipse.jdt.internal.corext.template.java.JavaTemplateMessages;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * An experimental proposal.
 */
public class ExperimentalProposal extends JavaCompletionProposal {

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
	    String displayString, int[] positionOffsets, int[] positionLengths, ITextViewer viewer, int relevance)
	{
		super(replacementString, replacementOffset, replacementLength, image, displayString, relevance);		

		fPositionOffsets= positionOffsets;
		fPositionLengths= positionLengths;
		fViewer= viewer;
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		super.apply(document, trigger, offset);

		int replacementOffset= getReplacementOffset();
		String replacementString= getReplacementString();

		if (fPositionOffsets.length > 0) {
			try {
				LinkedEnvironment env= new LinkedEnvironment();
				for (int i= 0; i != fPositionOffsets.length; i++) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					group.addPosition(new LinkedPosition(document, replacementOffset + fPositionOffsets[i], fPositionLengths[i], LinkedPositionGroup.NO_STOP));
					env.addGroup(group);
				}
				
				env.forceInstall();
				
				LinkedUIControl ui= new LinkedUIControl(env, fViewer);
				ui.setPositionListener(new EditorHistoryUpdater());
				ui.setExitPosition(fViewer, replacementOffset + replacementString.length(), 0, Integer.MAX_VALUE);
				ui.setDoContextInfo(true);
				ui.enter();
	
				fSelectedRegion= ui.getSelectedRegion();
	
			} catch (BadLocationException e) {
				JavaPlugin.log(e);	
				openErrorDialog(e);
			}		
		} else
			fSelectedRegion= new Region(replacementOffset + replacementString.length(), 0);
	}
	
	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return new Point(getReplacementOffset(), 0);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	private void openErrorDialog(BadLocationException e) {
		Shell shell= fViewer.getTextWidget().getShell();
		MessageDialog.openError(shell, JavaTemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
	}	

}
