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

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.compare.JavaTokenComparator;


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
		change.setKeepExecutedTextEdits(true);
		fIsInitialized= true;
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}	
	
	private static Change createCompilationUnitChange(String name, ICompilationUnit cu, boolean doSave) throws CoreException {
		CompilationUnitChange change= new CompilationUnitChange(name, cu);
		change.setKeepExecutedTextEdits(true);
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

			TextBuffer previewConent= change.getPreviewTextBuffer();
			String currentConentString= change.getCurrentContent();
			
			ITokenComparator leftSide= new JavaTokenComparator(previewConent.getContent(), true); 
			ITokenComparator rightSide= new JavaTokenComparator(currentConentString, true);
			
			RangeDifference[] differences= RangeDifferencer.findRanges(leftSide, rightSide);
			for (int i= 0; i < differences.length; i++) {
				RangeDifference curr= differences[i];
				int start= leftSide.getTokenStart(curr.leftStart());
				int end= leftSide.getTokenStart(curr.leftEnd());
				if (curr.kind() == RangeDifference.CHANGE && curr.leftLength() > 0) {
					buf.append("<b>"); //$NON-NLS-1$
					appendContent(previewConent, start, end, buf, false);
					buf.append("</b>"); //$NON-NLS-1$
				} else if (curr.kind() == RangeDifference.NOCHANGE) {
					appendContent(previewConent, start, end, buf, true);
				}
			}			
		} catch(CoreException e) {
			JavaPlugin.log(e);
		}
		return buf.toString();
	}
	
	private final int surroundLines= 1;

	private void appendContent(TextBuffer text, int startOffset, int endOffset, StringBuffer buf, boolean surroundLinesOnly) {
		int startLine= text.getLineOfOffset(startOffset);
		int endLine= text.getLineOfOffset(endOffset);
		
		boolean dotsAdded= false;
		if (surroundLinesOnly && startOffset == 0) { // no surround lines for the top no-change range
			startLine= Math.max(endLine - surroundLines, 0);
			buf.append("...<br>"); //$NON-NLS-1$
			dotsAdded= true;
		}
		
		for (int i= startLine; i <= endLine; i++) {
			if (surroundLinesOnly) {
				if ((i - startLine > surroundLines) && (endLine - i > surroundLines)) {
					if (!dotsAdded) {
						buf.append("...<br>"); //$NON-NLS-1$
						dotsAdded= true;
					} else if (endOffset == text.getLength()) {
						return; // no surround lines for the bottom no-change range
					}
					continue;
				}
			}
			
			TextRegion lineInfo= text.getLineInformation(i);
			int start= lineInfo.getOffset();
			int end= start + lineInfo.getLength();

			int from= Math.max(start, startOffset);
			int to= Math.min(end, endOffset);
			String content= text.getContent(from, to - from);
			if (surroundLinesOnly && (from == start) && Strings.containsOnlyWhitespaces(content)) {
				continue; // ignore empty lines exept when range started in the middle of a line
			}
			buf.append(content);
			if (to == end && to != endOffset) { // new line when at the end of the line, and not end of range
				buf.append("<br>"); //$NON-NLS-1$
			}
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
