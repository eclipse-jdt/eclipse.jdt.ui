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

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.link.LinkedEnvironment;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class ChangeCorrectionProposal implements IJavaCompletionProposal {
	
	private static class CorrectionChangeExceptionHandler implements IChangeExceptionHandler {
		public void handle(ChangeContext context, IChange change, Exception e) {
			throw new ChangeAbortException(e);
		}
	}

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
			if (change != null) {
				// close any open linked mode before applying our changes
				LinkedEnvironment.closeEnvironment(document);
				
				ChangeContext context= new ChangeContext(new CorrectionChangeExceptionHandler());
				change.aboutToPerform(context, new NullProgressMonitor());
				change.perform(context, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, CorrectionMessages.getString("ChangeCorrectionProposal.error.title"), CorrectionMessages.getString("ChangeCorrectionProposal.error.message"));  //$NON-NLS-1$//$NON-NLS-2$
		} catch (ChangeAbortException e) {
			Throwable wrapped= e.getThrowable();
			if (wrapped instanceof CoreException) {
				ExceptionHandler.handle((CoreException) wrapped, CorrectionMessages.getString("ChangeCorrectionProposal.error.title"), CorrectionMessages.getString("ChangeCorrectionProposal.error.message"));  //$NON-NLS-1$//$NON-NLS-2$
			} else {
				throw e;
			}
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
	 * @param change the change
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
