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
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.Strings;

public class SelectionAwareSourceRangeComputer extends TargetSourceRangeComputer {

	private ASTNode[] fSelectedNodes;
	private IDocument fDocument;
	private int fSelectionStart;
	private int fSelectionLength;
	
	private Map/*<ASTNode, SourceRange>*/ fRanges;
	
	public SelectionAwareSourceRangeComputer(ASTNode[] selectedNodes, IDocument document, int selectionStart, int selectionLength) throws BadLocationException, CoreException {
		fSelectedNodes= selectedNodes;
		fDocument= document;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
	}
	
	public SourceRange computeSourceRange(ASTNode node) {
		try {
			if (fRanges == null)
				initializeRanges();
			SourceRange result= (SourceRange)fRanges.get(node);
			if (result != null)
				return result;
			return super.computeSourceRange(node);
		} catch (BadLocationException e) {
			// fall back to standard implementation
			fRanges= new HashMap();
		} catch (CoreException e) {
			// fall back to standard implementation
			fRanges= new HashMap();
		}
		return super.computeSourceRange(node);
	}

	private void initializeRanges() throws BadLocationException, CoreException {
		fRanges= new HashMap();
		if (fSelectedNodes.length == 0)
			return;
		
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		String documentPortionToScan= fDocument.get(fSelectionStart, fSelectionLength);
		scanner.setSource(documentPortionToScan.toCharArray());
		TokenScanner tokenizer= new TokenScanner(scanner);
		int pos= tokenizer.getNextStartOffset(0, false);
		ASTNode firstNode= fSelectedNodes[0];
		int newStart= Math.min(fSelectionStart + pos, firstNode.getStartPosition());
		SourceRange range= super.computeSourceRange(firstNode);
		SourceRange firstRange= new SourceRange(newStart, range.getLength() + range.getStartPosition() - newStart);
		fRanges.put(firstNode, firstRange);
		
		ASTNode lastNode= fSelectedNodes[fSelectedNodes.length - 1];
		int scannerStart= lastNode.getStartPosition() + lastNode.getLength() - fSelectionStart;
		tokenizer.setOffset(scannerStart);
		pos= scannerStart;
		int token= -1;
		try {
			while (true) {
				token= tokenizer.readNext(false);
				pos= tokenizer.getCurrentEndOffset();
			}
		} catch (CoreException e) {
		}
		if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
			int index= pos - 1;
			while(index >= 0 && Strings.isLineDelimiterChar(documentPortionToScan.charAt(index))) {
				pos--;
				index--;
			}
		}
		
		int newEnd= Math.max(fSelectionStart + pos, lastNode.getStartPosition() + lastNode.getLength());
		range= firstNode == lastNode ? firstRange : super.computeSourceRange(lastNode);
		SourceRange lastRange= new SourceRange(range.getStartPosition(), newEnd - range.getStartPosition());
		fRanges.put(lastNode, lastRange);
	}
}
