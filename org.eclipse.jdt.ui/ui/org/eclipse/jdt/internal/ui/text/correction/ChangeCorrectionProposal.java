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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;

public abstract class ChangeCorrectionProposal implements ICompletionProposal {

	private ProblemPosition fProblemPosition;
	private String fName;

	public ChangeCorrectionProposal(String name, ProblemPosition problemPos) {
		fProblemPosition= problemPos;
		fName= name;
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
		buf.append("<p>");
		try {
			buf.append(getChange().getName());
		} catch (CoreException e) {
			JavaPlugin.log(e);
			buf.append(getDisplayString());
		}
		buf.append("</p>");
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
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ENV_VAR);
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fProblemPosition.getOffset(), fProblemPosition.getLength());
	}

	/**
	 * Gets the problem position.
	 * @return Returns a problem position
	 */
	public ProblemPosition getProblemPosition() {
		return fProblemPosition;
	}

	/**
	 * Gets the change element.
	 * @return Returns a CompilationUnitChange
	 */
	protected abstract Change getChange() throws CoreException;

	/**
	 * Sets the display name.
	 * @param name The name to set
	 */
	public void setDisplayName(String name) {
		fName= name;
	}

}
