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

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;

public class ChangeCorrectionProposal implements ICompletionProposal {

	private Change fChange;
	private String fName;
	private int fRelevance;
	
	private Object fElementToOpen;

	public ChangeCorrectionProposal(String name, Change change, int relevance) {
		fName= name;
		fChange= change;
		fRelevance= relevance;
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
		
		try {
			if (fElementToOpen != null) {
				IEditorPart part= EditorUtility.openInEditor(fElementToOpen, true);
				TextRange range= getRangeToReveal();
				if (part instanceof ITextEditor && range != null) {
					((ITextEditor) part).selectAndReveal(range.getOffset(), range.getLength());
				}
			}
		} catch (PartInitException e) {
			JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);			
		}
	}
	

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		buf.append("<p>"); //$NON-NLS-1$
		try {
			buf.append(getChange().getName());
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
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ENV_VAR);
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return null;
	}

	/**
	 * Gets the change element.
	 * @return Returns a Change
	 */
	protected Change getChange() throws CoreException {
		return fChange;
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

	/**
	 * Sets the revealRange.
	 * @param revealRange The revealRange to set
	 */
	public void setElementToOpen(Object elementToOpen) {
		fElementToOpen= elementToOpen;
	}
	
	protected TextRange getRangeToReveal() throws CoreException {
		return null;
	}

}
