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
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(buffer.getContent().toCharArray());
		
		int token= getSurroundingComment(scanner);
		if (token == ITerminalSymbols.TokenNameEOF) {
			return null;
		}
		int commentStart= scanner.getCurrentTokenStartPosition();
		int commentEnd= scanner.getCurrentTokenEndPosition() + 1;
		
		int contentStart= commentStart + 2;
		int contentEnd= commentEnd;
		if (token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
			contentStart= commentStart + 3;
			contentEnd= commentEnd - 2;
		} else if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
			contentEnd= commentEnd - 2;
		}
		if (hasContent(buffer, contentStart, fLocation.getOffset()) || hasContent(buffer, contentEnd, fLocation.getOffset() + fLocation.getLength())) {
			return new Position(fLocation.getOffset(), fLocation.getLength());
		}
		
		TextRegion startRegion= buffer.getLineInformationOfOffset(commentStart);
		int start= startRegion.getOffset();
		boolean contentAtBegining= hasContent(buffer, start, commentStart);
		
		if (contentAtBegining) {
			start= commentStart;
		}
		
		int end;
		if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
			if (contentAtBegining) {
				end= startRegion.getOffset() + startRegion.getLength(); // only to the end of the line
			} else {
				end= commentEnd; // includes new line
			}
		} else {
			int endLine= buffer.getLineOfOffset(commentEnd - 1);
			if (endLine + 1 == buffer.getNumberOfLines() || contentAtBegining) {
				TextRegion endRegion= buffer.getLineInformation(endLine);
				end= endRegion.getOffset() + endRegion.getLength();
			} else {
				TextRegion endRegion= buffer.getLineInformation(endLine + 1);
				end= endRegion.getOffset();
			}					
		}
		if (hasContent(buffer, commentEnd, end)) {
			end= commentEnd;
			start= commentStart; // only remove comment
		}
		return new Position(start, end - start);
	}
	
	private int getSurroundingComment(IScanner scanner) {
		try {
			int start= fLocation.getOffset();
			int end= start + fLocation.getLength();
				
			int token= scanner.getNextToken();
			while (token != ITerminalSymbols.TokenNameEOF) {
				if (TokenScanner.isComment(token)) {
					int currStart= scanner.getCurrentTokenStartPosition();
					int currEnd= scanner.getCurrentTokenEndPosition() + 1;
					if (currStart <= start && end <= currEnd) {
						return token;
					}
				}
				token= scanner.getNextToken();
			}
				
		} catch (InvalidInputException e) {
			// ignore
		}
		return ITerminalSymbols.TokenNameEOF;
	}	
	
	private boolean hasContent(TextBuffer buf, int start, int end) {
		for (int i= start; i < end; i++) {
			char ch= buf.getChar(i);
			if (!Character.isWhitespace(ch)) {
				return true;
			}
		}
		return false;
	}	
	
}
