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


import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
  */
public class TaskMarkerProposal extends CUCorrectionProposal {

	private IProblemLocation fLocation;

	public TaskMarkerProposal(ICompilationUnit cu, IProblemLocation location, int relevance) {
		super("", cu, relevance, null); //$NON-NLS-1$
		fLocation= location;
		
		setDisplayName(CorrectionMessages.getString("TaskMarkerProposal.description")); //$NON-NLS-1$
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createCompilationUnitChange(java.lang.String, org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.internal.corext.textmanipulation.TextEdit)
	 */
	protected CompilationUnitChange createCompilationUnitChange(String name, ICompilationUnit cu, TextEdit rootEdit) throws CoreException {
		CompilationUnitChange change= super.createCompilationUnitChange(name, cu, rootEdit);

		Position pos= null;
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(change.getFile());
			pos= getUpdatedPosition(buffer);
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}
		if (pos != null) {
			rootEdit.addChild(new ReplaceEdit(pos.getOffset(), pos.getLength(), "")); //$NON-NLS-1$
		} else {
			rootEdit.addChild(new ReplaceEdit(fLocation.getOffset(), fLocation.getLength(), "")); //$NON-NLS-1$
		}
		return change;
	}
	
		
		
	private Position getUpdatedPosition(TextBuffer buffer) {
		IScanner scanner= getSurroundingComment(buffer);
		if (scanner == null) {
			return null;
		}
		int commentStart= scanner.getCurrentTokenStartPosition();
		int commentEnd= scanner.getCurrentTokenEndPosition() + 1;
		TextRegion startRegion= buffer.getLineInformationOfOffset(commentStart);
		int start= startRegion.getOffset();
		if (hasContent(buffer.getContent(start, fLocation.getOffset() - start))) {
			return null;
		}
		int end;
		if (buffer.getChar(commentStart) == '/' && buffer.getChar(commentStart + 1) == '/') {
			end= commentEnd;
		} else {
			int endLine= buffer.getLineOfOffset(commentEnd - 1);
			if (endLine + 1 == buffer.getNumberOfLines()) {
				TextRegion endRegion= buffer.getLineInformation(endLine);
				end= endRegion.getOffset() + endRegion.getLength();
			} else {
				TextRegion endRegion= buffer.getLineInformation(endLine + 1);
				end= endRegion.getOffset();
			}					
		}
		int posEnd= fLocation.getOffset() + fLocation.getLength();
		if (hasContent(buffer.getContent(posEnd, end - posEnd))) {
			return null;
		}
		return new Position(start, end - start);
	}
	
	private IScanner getSurroundingComment(TextBuffer buffer) {
		try {
			IScanner scanner= ToolFactory.createScanner(true, false, false, false);
			scanner.setSource(buffer.getContent().toCharArray());
			scanner.resetTo(0, buffer.getLength() - 1);
				
			int start= fLocation.getOffset();
			int end= start + fLocation.getLength();
				
			int token= scanner.getNextToken();
			while (token != ITerminalSymbols.TokenNameEOF) {
				if (TokenScanner.isComment(token)) {
					int currStart= scanner.getCurrentTokenStartPosition();
					int currEnd= scanner.getCurrentTokenEndPosition() + 1;
					if (currStart <= start && end <= currEnd) {
						return scanner;
					}
				}
				token= scanner.getNextToken();
			}
				
		} catch (InvalidInputException e) {
			// ignore
		}
		return null;
	}	
	
	private boolean hasContent(String string) {
		for (int i= 0; i < string.length(); i++) {
			char ch= string.charAt(i);
			if (Character.isLetter(ch)) {
				return true;
			}
		}
		return false;
	}	
	
}
