/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedEnvironment;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Implementation of a Java completion proposal to be used for quick fix and quick assist
 * proposals that invoke a {@link Change}. The proposal offers a proposal information but no context
 * information.
 * @since 3.0
 */
public class ChangeCorrectionProposal implements IJavaCompletionProposal {
	
	private Change fChange;
	private String fName;
	private int fRelevance;
	private Image fImage;
	
	/**
	 * Constructs a change correction proposal.
	 * @param name The name that is displayed in the proposal selection dialog.
	 * @param change The change that is executed when the proposal is applied.  
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public ChangeCorrectionProposal(String name, Change change, int relevance, Image image) {
		Assert.isNotNull(name);
		fName= name;
		fChange= change;
		fRelevance= relevance;
		fImage= image;
	}
	
	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		try {
			performChange(JavaPlugin.getActivePage().getActiveEditor(), document);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, CorrectionMessages.getString("ChangeCorrectionProposal.error.title"), CorrectionMessages.getString("ChangeCorrectionProposal.error.message"));  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	/**
	 * Performs the change associated with this proposal.
	 * @param activeEditor The editor currently active or <code>null</code> if no
	 * editor is active.
	 * @param document The document of the editor currently active or <code>null</code> if
	 * no editor is visible.
	 * @throws CoreException Thrown when the invocation of the change failed.
	 */
	protected void performChange(IEditorPart activeEditor, IDocument document) throws CoreException {
		Change change= null;
		try {
			change= getChange();
			if (change != null) {
				if (document != null) {
					LinkedEnvironment.closeEnvironment(document);
				}
				
				change.initializeValidationData(new NullProgressMonitor());
				RefactoringStatus valid= change.isValid(new NullProgressMonitor());
				if (valid.hasFatalError()) {
					IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
						valid.getMessageMatchingSeverity(RefactoringStatus.FATAL), null);
					throw new CoreException(status);
				} else {
					change.perform(new NullProgressMonitor());
				}
			}
		} finally {
			if (change != null) {
				change.dispose();
			}
		}
	}
	
	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		buf.append("<p>"); //$NON-NLS-1$
		Change change= getChange();
		if (change != null) {
			buf.append(change.getName());
		} else {
			return null;
		}
		buf.append("</p>"); //$NON-NLS-1$
		return buf.toString();
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fName;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return null;
	}

	/**
	 * Sets the proposal's image or <code>null</code> if no image is desired.
	 * @param image the desired image. 
	 */
	public void setImage(Image image) {
		fImage= image;
	}

	/**
	 * Returns the change that will be executed when the proposal is applied.
	 * @return Returns a Change
	 */
	public Change getChange() {
		return fChange;
	}
	
	/**
	 * Sets the display name.
	 * @param name The name to set
	 */
	public void setDisplayName(String name) {
		Assert.isNotNull(name);
		fName= name;
	}

	/*
	 *(non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return fRelevance;
	}

	/**
	 * Sets the relevance.
	 * @param relevance The relevance to set
	 */
	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}

}
