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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.EditChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class CUCorrectionProposal extends ChangeCorrectionProposal {

	private boolean fIsInitialized;

	public CUCorrectionProposal(String name, ICompilationUnit cu, int relevance) throws CoreException {
		this(name, cu, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}
	
	public CUCorrectionProposal(String name, ICompilationUnit cu, int relevance, Image image) throws CoreException {
		super(name, createCompilationUnitChange(name, cu, false), relevance, image);
		fIsInitialized= false;
	}
	
	public CUCorrectionProposal(String name, CompilationUnitChange change, int relevance) throws CoreException {
		super(name, change, relevance);
		change.setTrackPositionChanges(true);
		fIsInitialized= true;
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}	
	
	private static Change createCompilationUnitChange(String name, ICompilationUnit cu, boolean doSave) throws CoreException {
		CompilationUnitChange change= new CompilationUnitChange(name, cu);
		change.setTrackPositionChanges(true);
		change.setSave(doSave);
		return change;
	}

	/*
	 * @see ChangeCorrectionProposal#getChange()
	 */
	protected Change getChange() throws CoreException {
		Change change= super.getChange();
		if (!fIsInitialized) {
			fIsInitialized= true;
			addEdits((CompilationUnitChange) change);
		}
		return change;
	}
	
	protected void addEdits(CompilationUnitChange change) throws CoreException {
	}
	
	
	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		
		try {
			CompilationUnitChange change= getCompilationUnitChange();

			TextBuffer textBuffer= change.getPreviewTextBuffer();

			EditChange[] changes= change.getTextEditChanges();		
			for (int i= 0; i < changes.length; i++) {
				TextRange range= change.getNewTextRange(changes[i]);
				buf.append("<p>...</p>"); //$NON-NLS-1$
				appendContent(textBuffer, range, 1, buf);
			}
			if (changes.length > 0) {
				buf.append("<p>...</p>"); //$NON-NLS-1$
			}
			
		} catch(CoreException e) {
			JavaPlugin.log(e);
		}
		return buf.toString();
	}

	private void appendContent(TextBuffer buffer, TextRange range, int surroundingLines, StringBuffer buf) {
		int startLine= Math.max(buffer.getLineOfOffset(range.getOffset()) - surroundingLines, 0);
		int endLine= Math.min(buffer.getLineOfOffset(range.getInclusiveEnd()) + surroundingLines, buffer.getNumberOfLines() - 1);
		
		int rangeStart= range.getOffset();
		int rangeEnd= rangeStart + range.getLength();
		for (int i= startLine; i <= endLine; i++) {
			buf.append("<p>"); //$NON-NLS-1$
			TextRegion lineInfo= buffer.getLineInformation(i);
			int start= lineInfo.getOffset();
			int end= lineInfo.getOffset() + lineInfo.getLength();
			if (rangeStart >= start && rangeStart <= end) {
				buf.append(buffer.getContent(start, rangeStart - start));
				buf.append("<b>"); //$NON-NLS-1$
				start= rangeStart;
			}
			if (rangeEnd >= start && rangeEnd <= end) {
				buf.append(buffer.getContent(start, rangeEnd - start));
				buf.append("</b>"); //$NON-NLS-1$
				start= rangeEnd;
			}
			buf.append(buffer.getContent(start, end - start));
			buf.append("</p>"); //$NON-NLS-1$
		}
	}		
		
	/**
	 * Gets the compilationUnitChange.
	 * @return Returns a CompilationUnitChange
	 */
	public CompilationUnitChange getCompilationUnitChange() throws CoreException {
		return (CompilationUnitChange) getChange();
	}


}
