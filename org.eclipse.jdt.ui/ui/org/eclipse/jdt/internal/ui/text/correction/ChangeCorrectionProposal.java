/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.text.java.IJavaCompletionProposal;

public class ChangeCorrectionProposal implements IJavaCompletionProposal {

	private Change fChange;
	private String fName;
	private int fRelevance;
	private Image fImage;

	public ChangeCorrectionProposal(String name, Change change, int relevance) {
		this(name, change, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}
	
	public ChangeCorrectionProposal(String name, Change change, int relevance, Image image) {
		fName= name;
		fChange= change;
		fRelevance= relevance;
		fImage= image;
	}
	
	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		Change change= null;
		try {
			change= getChange();
			
			ChangeContext context= new ChangeContext(new AbortChangeExceptionHandler());
			change.aboutToPerform(context, new NullProgressMonitor());
			change.perform(context, new NullProgressMonitor());
		} catch(ChangeAbortException e) {
			JavaPlugin.log(e);
		} catch(CoreException e) {
			JavaPlugin.log(e);
		} finally {
			if (change != null) {
				change.performed();
			}
		}
	}
	
	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		buf.append("<p>"); //$NON-NLS-1$
		try {
			Change change= getChange();
			if (change != null) {
				buf.append(change.getName());
			} else {
				return null;
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			buf.append(getDisplayString());
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
	 * Sets the proposal's image.
	 * 
	 * @param image the desired image. Can be <code>null</code>
	 */
	public void setImage(Image image) {
		fImage= image;
	}

	/**
	 * Gets the change element.
	 * @return Returns a Change
	 */
	protected Change getChange() throws CoreException {
		return fChange;
	}
	
	/**
	 * Sets the change element.
	 * @param 
	 */
	protected void setChange(Change change) throws CoreException {
		fChange= change;
	}	

	/**
	 * Sets the display name.
	 * @param name The name to set
	 */
	public void setDisplayName(String name) {
		fName= name;
	}

	/**
	 * Gets the relevance.
	 * @return Returns an int
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
