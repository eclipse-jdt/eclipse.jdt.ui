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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.SourceRange;

/**
 * Utility class used to get better source ranges for <code>ISourceReference</code>
 * and temp declarations.
 */
public class SourceRangeComputer {
	
	private final ISourceRange fSourceRange;
	private final String fCuSource;
	
	private SourceRangeComputer(ISourceRange sourceRange, String cuSource){
		fSourceRange= sourceRange;
		fCuSource= cuSource;
	}
	
	/**
	 * Returns the computed source of the elements.
	 * @see SourceRangeComputer#computeSourceRange(ISourceReference, String)
	 */
	public static String computeSource(ISourceReference elem) throws JavaModelException{
		String cuSource= getCuSource(elem);
		ISourceRange range= SourceRangeComputer.computeSourceRange(elem, cuSource);
		return cuSource.substring(range.getOffset(), range.getOffset() + range.getLength());
	}

    private static String getCuSource(ISourceReference elem) throws JavaModelException {
		ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(elem);
		if (cu != null && cu.exists()){
			return cu.getSource();
    	} else if (elem instanceof IMember){
			IMember member= (IMember)elem;
			if (! member.isBinary())
				return null;
			IClassFile classFile= (IClassFile)member.getAncestor(IJavaElement.CLASS_FILE);
			if (classFile == null)
				return null;
			return classFile.getSource();
		}
        return null;
    }
	
	public static ISourceRange computeSourceRange(ISourceReference element, String cuSource) throws JavaModelException{
			if (! element.exists())
				return element.getSourceRange();
			if (cuSource == null)
				return element.getSourceRange();
			return computeSourceRange(element.getSourceRange(), cuSource);				
	}
	
	public static ISourceRange computeSourceRange(ISourceRange sourceRange, String cuSource) {
		SourceRangeComputer inst= new SourceRangeComputer(sourceRange, cuSource);
		try {
			int offset= inst.computeOffset();
			int end= inst.computeEnd();
			return new SourceRange(offset, (end - offset));
		} catch (BadLocationException exception) {
			// Should never happen
			JavaPlugin.log(exception);
		}
		return null;
	}

	private int computeEnd() throws BadLocationException {
		int end= fSourceRange.getOffset() + fSourceRange.getLength();
		try{	
			IScanner scanner= ToolFactory.createScanner(true, true, false, true);
			scanner.setSource(fCuSource.toCharArray());
			scanner.resetTo(end, fCuSource.length() - 1);
			IDocument document= new Document(fCuSource);
			int startLine= document.getLineOfOffset(scanner.getCurrentTokenEndPosition() + 1);
			
			int token= scanner.getNextToken();
			while (token != ITerminalSymbols.TokenNameEOF) {
				switch (token) {
					case ITerminalSymbols.TokenNameWHITESPACE:
						break;
					case ITerminalSymbols.TokenNameSEMICOLON:
						break;	
					case ITerminalSymbols.TokenNameCOMMENT_LINE :
						if (startLine == document.getLineOfOffset(scanner.getCurrentTokenStartPosition() + 1))
							break;
						else
							return stopProcessing(end, scanner, document, startLine);
					case ITerminalSymbols.TokenNameCOMMENT_BLOCK :
						if (startLine == document.getLineOfOffset(scanner.getCurrentTokenStartPosition() + 1))
							break;
						else	
							return stopProcessing(end, scanner, document, startLine);
					default:
						return stopProcessing(end, scanner, document, startLine);
				}
				token= scanner.getNextToken();
			}
			return end;//fallback
		} catch (InvalidInputException e){
			return end;//fallback
		}
	}

	private int stopProcessing(int end, IScanner scanner, IDocument buff, int startLine) throws BadLocationException {
		int currentTokenStartLine= buff.getLineOfOffset(scanner.getCurrentTokenStartPosition() + 1);
		int currentTokenEndLine= buff.getLineOfOffset(scanner.getCurrentTokenEndPosition() + 1);
		if (endOnCurrentTokenStart(startLine, currentTokenStartLine, currentTokenEndLine))
			return scanner.getCurrentTokenEndPosition() - scanner.getCurrentTokenSource().length + 1;
		IRegion tokenStartLine= buff.getLineInformation(currentTokenStartLine);
		if (tokenStartLine != null)
			return tokenStartLine.getOffset();
		else
			return end; //fallback	
	}

	private boolean endOnCurrentTokenStart(int startLine, int currentTokenStartLine,int currentTokenEndLine) {
		if (startLine == currentTokenEndLine)
			return true;
		return (startLine == currentTokenStartLine && currentTokenStartLine != currentTokenEndLine);
	}
	
	private int computeOffset() throws BadLocationException {
		int offset= fSourceRange.getOffset();
		try{
			IDocument document= new Document(fCuSource);
			int lineOffset= document.getLineInformationOfOffset(offset).getOffset();
			IScanner scanner= ToolFactory.createScanner(true, true, false, true);
			scanner.setSource(document.get().toCharArray());
			scanner.resetTo(lineOffset, document.getLength() - 1);
			
			int token= scanner.getNextToken();
			while (token != ITerminalSymbols.TokenNameEOF) {
				switch (token) {
					case ITerminalSymbols.TokenNameWHITESPACE:
						break;
					case ITerminalSymbols.TokenNameSEMICOLON:
						break;	
					default:
						if (scanner.getCurrentTokenStartPosition() == offset)
							return lineOffset;
						else
							return offset;	
				}
				token= scanner.getNextToken();
			}
			return offset;	//should never get here really
		} catch (InvalidInputException e){
			return offset;//fallback
		}
	}
}

