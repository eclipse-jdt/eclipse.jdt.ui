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
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Uses the {@link org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner} to get the indentation
 * level for a certain position in a document.
 * 
 * <p>An instance holds some internal position in the document and is therefore not threadsafe.</p>
 * 
 * @since 3.0
 */
public class JavaIndenter {

	/** The document being scanned. */
	private IDocument fDocument;
	/** The indentation accumulated by <code>findPreviousIndenationUnit</code>. */
	private int fIndent;
	/** The absolute (character-counted) indentation offset for special cases (method defs, array initializers) */
	private int fAlign;
	/** Whether to add one space to the absolute indentation. */
	private boolean fAlignPlusOne;
	/** The stateful scanpositionf or the indentation methods. */
	private int fPosition;
	/** The previous position. */
	private int fPreviousPos;
	/** The most recent token. */
	private int fToken;
	/** 
	 * The scanner we will use to scan the document. It has to be installed on the same document
	 * as the one we get.
	 */
	private JavaHeuristicScanner fScanner;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param document the document to scan
	 */
	public JavaIndenter(IDocument document, JavaHeuristicScanner scanner) {
		Assert.isNotNull(document);
		Assert.isNotNull(scanner);
		fDocument= document;
		fScanner= scanner;
	}
	
	/**
	 * Computes the indentation at the reference point of <code>position</code>.
	 * 
	 * @param position the position in the document, either at the beginning of a line or in the 
	 * whitespace at the beginning of a line
	 * @return a String which reflects the indentation at the line in which the reference position
	 * to <code>position</code> resides, or <code>null</code> if it cannot be determined.
	 */
	public String getReferenceIndentation(int position) {
		
		try {
			// account for unindenation characters already typed in, but after position
			// also account for a dangling else
			
			boolean danglingElse= false;
			boolean matchBrace= false;
			
			if (position < fDocument.getLength()) {
				IRegion line= fDocument.getLineInformationOfOffset(position);
				int next= fScanner.nextToken(position, line.getOffset() + line.getLength());
				switch (next) {
					case Symbols.TokenEOF:
					case Symbols.TokenELSE:
						danglingElse= true;
						break;
					case Symbols.TokenRBRACE: // closing braces get unindented
						matchBrace= true;
				}
			} else {
				danglingElse= true;
			}
			
			// find the base position
			int unit= findReferencePosition(position, danglingElse, matchBrace);
			
			// if we were unable to find anything, return null
			if (unit == JavaHeuristicScanner.NOT_FOUND)
				return null; //$NON-NLS-1$
			
			// get base indent at the reference location
			IRegion line= fDocument.getLineInformationOfOffset(unit);
			int offset= line.getOffset();
			int nonWS= fScanner.findNonWhitespaceForwardInAnyPartition(offset, offset + line.getLength());
			StringBuffer indent= new StringBuffer(fDocument.get(offset, nonWS - offset));
			
			return indent.toString();
			
		} catch (BadLocationException e) {
		}
		
		return null;
	}
	
	/**
	 * Computes the indentation at <code>position</code>.
	 * 
	 * @param position the position in the document, either at the beginning of a line or in the 
	 * whitespace at the beginning of a line
	 * @return a String which reflects the correct indentation for the line in which position
	 * resides, or <code>null</code> if it cannot be determined..
	 */
	public String computeIndentation(int position) {
		
		try {
			// account for unindenation characters already typed in, but after position
			// also account for a dangling else
			
			boolean danglingElse= false;
			boolean unindent= false;
			boolean matchBrace= false;
			
			if (position < fDocument.getLength()) {
				IRegion line= fDocument.getLineInformationOfOffset(position);
				int next= fScanner.nextToken(position, line.getOffset() + line.getLength());
				int prevPos= Math.max(position - 1, 0);
				switch (next) {
					case Symbols.TokenEOF:
					case Symbols.TokenELSE:
						danglingElse= true;
						break;
					case Symbols.TokenCASE:
					case Symbols.TokenDEFAULT:
						// only if not right after the brace!
						if (prefAlignCaseWithSwitch() || fScanner.previousToken(prevPos, JavaHeuristicScanner.UNBOUND) != Symbols.TokenLBRACE)
							unindent= true;
						break;
					case Symbols.TokenLBRACE: // for opening-brace-on-new-line style
						if (fScanner.isBracelessBlockStart(prevPos, JavaHeuristicScanner.UNBOUND))
							unindent= true;
						break;
					case Symbols.TokenRBRACE: // closing braces get unindented
						matchBrace= true;
						break;
					case Symbols.TokenWHILE:
						break;
				}
			} else {
				danglingElse= true; // assume an else could come - align with 'if' 
			}
			
			// find the base position
			int unit= findReferencePosition(position, danglingElse, matchBrace);
			
			// handle special alignment
			if (fAlign != JavaHeuristicScanner.NOT_FOUND) {
				// a special case has been detected.
				IRegion line= fDocument.getLineInformationOfOffset(fAlign);
				int offset= line.getOffset();
				return createIndent(offset, fAlign);
			}
			
			// if we were unable to find anything, return null
			if (unit == JavaHeuristicScanner.NOT_FOUND)
				return null; //$NON-NLS-1$
			
			// get base indent at the reference location
			IRegion line= fDocument.getLineInformationOfOffset(unit);
			int offset= line.getOffset();
			int nonWS= fScanner.findNonWhitespaceForwardInAnyPartition(offset, offset + line.getLength());
			StringBuffer indent= new StringBuffer(fDocument.get(offset, nonWS - offset));
			
			// add additional indent
			indent.append(createIndent(fIndent));
			if (unindent)
				unindent(indent);
				
			return indent.toString();
			
		} catch (BadLocationException e) {
		}
		
		return null; //$NON-NLS-1$
	}

	/**
	 * Reduces indentation in <code>indent</code> by one.
	 * 
	 * @param indent the indentation to be modified
	 */
	private void unindent(StringBuffer indent) {
		String oneIndent= createIndent(1);
		int i= indent.lastIndexOf(oneIndent); //$NON-NLS-1$
		if (i != -1) {
			indent.deleteCharAt(i);
		}			
	}

	/**
	 * Creates an indentation string of the length indent - start + 1, consisting of the content 
	 * in <code>fDocument</code> in the range [start, indent), with every character replaced by
	 * a space except for tabs, which are kept as such.
	 * 
	 * @return the indentation corresponding to the document content specified by <code>start</code>
	 * and <code>indent</code>
	 */
	private String createIndent(int start, int indent) {
		int tabLen;
		if (JavaPlugin.getDefault() != null)
			tabLen= CodeFormatterUtil.getTabWidth();
		else
			tabLen= 4;
		
		StringBuffer ret= new StringBuffer();
		try {
			int spaces= 0;
			while (start < indent) {
				
				char ch= fDocument.getChar(start);
				if (ch == '\t') {
					ret.append('\t');
					spaces= 0;
				} else {
					spaces++;
					if (spaces == tabLen) {
						ret.append('\t');
						spaces= 0;
					}
				}
				
				start++;
			}
			if (fAlignPlusOne)
				spaces++;
			
			if (spaces == tabLen)
				ret.append('\t');
			else
				while (spaces-- > 0)
					ret.append(' ');
			
		} catch (BadLocationException e) {
		}
		
		return ret.toString();
	}

	/**
	 * Creates a string that represents the given number of indents (can be spaces or tabs..)
	 * @param indent the requested indentation level.
	 * 
	 * @return the indentation specified by <code>indent</code>
	 */
	private String createIndent(int indent) {
		// get a sensible default when running without the infrastructure for testing
		if (JavaPlugin.getDefault() == null) {
			StringBuffer ret= new StringBuffer();
			while (indent-- > 0)
				ret.append('\t');
			
			return ret.toString();
		}
			
		return CodeFormatterUtil.createIndentString(indent);
	}
	
	/**
	 * Returns the reference position regarding to indentation for <code>position</code>, or
	 * <code>NOT_FOUND</code>. <code>fIndent</code> will contain the relative indentation (in 
	 * indentation units, not characters) after the call. If there is a special alignment (e.g. for
	 * a method declaration where parameters should be aligned), <code>fAlign</code> will contain the absolute
	 * position of the alignment reference in <code>fDocument</code>, otherwise <code>fAlign</code>
	 * is set to <code>JavaHeuristicScanner.NOT_FOUND</code>.
	 * 
	 * @param position the position for which the reference is computed
	 * @return the reference statement relative to which <code>position</code> should be indented.
	 */
	public int findReferencePosition(int position) {
		return findReferencePosition(position, false, false);
	}
	
	/**
	 * Returns the reference position regarding to indentation for <code>position</code>, or
	 * <code>NOT_FOUND</code>. <code>fIndent</code> will contain the relative indentation (in 
	 * indentation units, not characters) after the call. If there is a special alignment (e.g. for
	 * a method declaration where parameters should be aligned), <code>fAlign</code> will contain the absolute
	 * position of the alignment reference in <code>fDocument</code>, otherwise <code>fAlign</code>
	 * is set to <code>JavaHeuristicScanner.NOT_FOUND</code>.
	 * 
	 * @param position the position for which the reference is computed
	 * @param danglingElse whether a dangling else should be assumed at <code>position</code>
	 * @param matchBrace whether the position of the matching brace should be returned instead of doing code analysis
	 * @return the reference statement relative to which <code>position</code> should be indented.
	 */
	private int findReferencePosition(int position, boolean danglingElse, boolean matchBrace) {
		fIndent= 0; // the indentation modification
		fAlign= JavaHeuristicScanner.NOT_FOUND;
		fPosition= position;
		
		boolean indentBlockLess= true; // whether to indent after an if / while / for without block (set false by semicolons and braces)
		boolean takeNextExit= true; // whether the next possible exit should be taken (instead of looking for the base; see blockless stuff)
		boolean found= false; // whether we have found anything at all. If we have, we'll trace back to it once we have a hoist point
		boolean hasBrace= false;
		
		int commaPosition= JavaHeuristicScanner.NOT_FOUND;
		
		if (matchBrace) {
			if (!skipScope(Symbols.TokenLBRACE, Symbols.TokenRBRACE))
				fPosition= position;
			else {
				indentBlockLess= false;
				hasBrace= true;
			}
		}
		
		nextToken();
		while (true) {
			
			switch (fToken) {
				// skip all scopes introduced by parenthesis' or brackets:
				case Symbols.TokenRBRACKET:
					skipScope(Symbols.TokenLBRACKET, Symbols.TokenRBRACKET);
					nextToken();
					break;
					
				case Symbols.TokenRPAREN:
					skipScope(Symbols.TokenLPAREN, Symbols.TokenRPAREN);
					
					// handle special indentations: non-block conditionals
					nextToken();
					int pos= fPosition;

					switch (fToken) {
						case Symbols.TokenWHILE:
							if (hasMatchingDo()) {
								nextToken();
								break;
							} else {
								nextToken(pos);
							}
						case Symbols.TokenIF:
							if (danglingElse && fToken == Symbols.TokenIF)
								takeNextExit= true;
						case Symbols.TokenFOR:
							if (indentBlockLess)
								fIndent++;
							if (takeNextExit)
								return pos;
					}

					break;
				
				case Symbols.TokenDO: // do blockless special
					if (indentBlockLess) {
						fIndent++;
						return fPosition;
					} else if (hasBrace) {
						return fPosition;
					} else {
						fIndent= 0; // after a do, there is a mandatory while on the same level
						return fPosition;
					}
					
				case Symbols.TokenELSE: // else blockless special
	
					if (indentBlockLess)
						fIndent++;
					
					if (takeNextExit)
						return fPosition;
					
					// else
					if (!searchIfForElse())
						return JavaHeuristicScanner.NOT_FOUND;
					
					nextToken();
					break;
				
				case Symbols.TokenCOLON: // switch statements and labels
					if (searchCaseGotoDefault()) {
						if (!hasBrace)
							fIndent++;
						return fPosition;
					}
					break;
					
				case Symbols.TokenQUESTIONMARK: // ternary expressions
					if (takeNextExit && prefTenaryDeepAlign())
						fAlign= fPosition;
					nextToken();
					break;
				
				// When we find a semi or lbrace, we have found a hoist point
				// Take the first start to the right from it. If there is only whitespace
				// up to position, search one step more.
				case Symbols.TokenLBRACE:
				
					int searchPos= fPreviousPos;
					
					// special array handling
					nextToken();
					if (fToken == Symbols.TokenEQUAL || skipBrackets()) {
						int first= fScanner.findNonWhitespaceForwardInAnyPartition(searchPos, position);
						// ... with a first element already defined - take its offset
						if (prefArrayDeepIndent() && first != JavaHeuristicScanner.NOT_FOUND) {
							fAlign= first;
						} else
							fIndent += prefArrayIndent();
					}
					
					hasBrace= true;
					
					if (found)
						return fScanner.findNonWhitespaceForward(searchPos, position);
                    
					// search start of code forward or continue
					takeNextExit= true;
					indentBlockLess= false;
					
					// indent when searching over an LBRACE
					fIndent++;
					break;
					
				case Symbols.TokenSEMICOLON:
					// search start of code forward or continue
					if (found)
						return fScanner.findNonWhitespaceForward(fPreviousPos, position);
					
					takeNextExit= false; // search to the bottom of blockless statements
					indentBlockLess= false; // don't indent at the next blockless introducer
					
					nextToken();
					break;
				
				case Symbols.TokenEOF:
					if (found)
						return fScanner.findNonWhitespaceForward(0, position);
					
					return JavaHeuristicScanner.NOT_FOUND;
					
				// RBRACE is either the end of a statement as SEMICOLON, 
				// or - if no statement start can be found - must be skipped as RPAREN and RBRACKET
				case Symbols.TokenRBRACE:
					if (found && fScanner.nextToken(fPreviousPos, position) != Symbols.TokenSEMICOLON) // don't take it for array initializers
						return fScanner.findNonWhitespaceForward(fPreviousPos, position);

					skipScope(Symbols.TokenLBRACE, Symbols.TokenRBRACE);

					takeNextExit= false; // search to the bottom of blockless statements
					indentBlockLess= false; // don't indent at the next blockless introducer

					nextToken();
					break;
				
				// use double indentation inside conditions and calls
				// handle method definitions separately
				case Symbols.TokenLPAREN:
					// TODO differentiate between conditional continuation and calls
					if (!hasBrace)
						fIndent += prefCallContinuationIndent();
					
					searchPos= fPreviousPos;
					
					if (prefMethodDeclDeepIndent() && looksLikeMethodDecl() && found) {
						fAlign= fScanner.findNonWhitespaceForward(searchPos, position);
					}
					
					break;

				// array dimensions
				case Symbols.TokenLBRACKET:
					if (prefArrayDimensionsDeepIndent() && found)
						fAlign= fScanner.findNonWhitespaceForward(fPreviousPos, position);
					
					fIndent+= prefArrayDimensionIndent();
						
					nextToken();
					break;
				
				case Symbols.TokenCOMMA:
					if (found)
						commaPosition= fScanner.findNonWhitespaceForward(fPreviousPos, position);
					nextToken();
					break;

				default:
					nextToken();
			}
			
			found= true;

		}
	}
	
	/**
	 * Searches for a case, goto, or default after a scanned colon.
	 * 
	 * @return <code>true</code> if one of the above keywords can be scanned, possibly separated
	 * by an identifier or constant.
	 */
	private boolean searchCaseGotoDefault() {
		// after a colon
		while (true) {
			nextToken();
			switch (fToken) {
				// number or char literals won't bother us, no scopes allowed
				case Symbols.TokenOTHER:
				case Symbols.TokenIDENT:
					break;
				case Symbols.TokenEOF:
					return false;
				case Symbols.TokenCASE:
				case Symbols.TokenDEFAULT:
				case Symbols.TokenGOTO:
					return true;
				default:
					return false;
			}
			
		}
	}

	/**
	 * while(condition); is ambiguous when parsed backwardly, as it is a valid statement by its own,
	 * so we have to check whether there is a matching do. A <code>do</code> can either be separated
	 * from the while by a block, or by a single statement, which limits our search distance.
	 * 
	 * @return <code>true</code> if the <code>while</code> currently in <code>fToken</code> has a matching <code>do</code>.
	 */
	private boolean hasMatchingDo() {
		Assert.isTrue(fToken == Symbols.TokenWHILE);
		
		return skipStatementOrBlock() && fToken == Symbols.TokenDO;
	}

	/**
	 * Skips a statement or block, the token being the next token after it.
	 * 
	 * @return <code>true</code> if a statement or block could be parsed, <code>false</code> otherwise.
	 */
	private boolean skipStatementOrBlock() {
		nextToken();
		
		switch (fToken) {		
			case Symbols.TokenRBRACE:
				// do { BLOCK } while
				if (skipScope(Symbols.TokenLBRACE, Symbols.TokenRBRACE)) {
					nextToken();
					return true;
				}
				break;
			case Symbols.TokenSEMICOLON:
				// do statement; while
				nextToken();
				while (true) {
					switch (fToken) {
						case Symbols.TokenRBRACE:
							// array definition
							skipScope(Symbols.TokenLBRACE, Symbols.TokenRBRACE);
							nextToken();
									
							if (skipBrackets())
								break;
							else
								return false;
						case Symbols.TokenRBRACKET: // array index
							skipScope(Symbols.TokenLBRACKET, Symbols.TokenRBRACKET);
							break;
						case Symbols.TokenRPAREN: // call, if , for, ..., step over
							skipScope(Symbols.TokenLPAREN, Symbols.TokenRPAREN);
							break;
						case Symbols.TokenSEMICOLON:
							return true;
						case Symbols.TokenLBRACE:
							return true;
						case Symbols.TokenLPAREN:
							return false;
						case Symbols.TokenLBRACKET:
							return false;
						case Symbols.TokenDO:
							return true;
						case Symbols.TokenIF:
							return true;
						case Symbols.TokenFOR:
							return true;
						case Symbols.TokenWHILE:
							return true;
						case Symbols.TokenEOF:
							return false;
					}
					nextToken();
			}
		}  
		
		return false;		
	}
	
	/**
	 * Skips brackets if the current token is a RBRACKET. There can be nothing in between, this is
	 * only to be used for <code>[]</code> elements.
	 * 
	 * @return <code>true</code> if a <code>[]</code> could be scanned, the current token is left at
	 * the LBRACKET.
	 */
	private boolean skipBrackets() {
		if (fToken == Symbols.TokenRBRACKET) {
			nextToken();
			if (fToken == Symbols.TokenLBRACKET) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Searches for the <code>if</code> matching a just scanned else.
	 * 
	 * @return <code>true</code> if the matching if can be found, <code>false</code> otherwise
	 */
	private boolean searchIfForElse() {
		
		int depth= 1;
		while (true) {
			nextToken();
			
			switch (fToken) {
				case Symbols.TokenRBRACE:
					skipScope(Symbols.TokenLBRACE, Symbols.TokenRBRACE);
					break;
				case Symbols.TokenIF:
					depth--;
					if (depth == 0)
						return true;
					break;
				case Symbols.TokenELSE:
					depth++;
					break;
				
				case Symbols.TokenEOF:
					return false;
			} 
		}
			
		
	}

	/**
	 * Reads the next token in backward direction from the heuristic scanner and sets the fields
	 * <code>fToken, fPreviousPosition</code> and <code>fPosition</code> accordingly.
	 */
	private void nextToken() {
		nextToken(fPosition);
	}

	/**
	 * Reads the next token in backward direction of <code>start</code> from the heuristic scanner and sets the fields
	 * <code>fToken, fPreviousPosition</code> and <code>fPosition</code> accordingly.
	 */
	private void nextToken(int start) {
		fToken= fScanner.previousToken(start - 1, JavaHeuristicScanner.UNBOUND);
		fPreviousPos= start;
		fPosition= fScanner.getPosition() + 1;
	}

	/**
	 * Returns <code>true</code> if the current tokens look like a method declaration header 
	 * (i.e. only the return type and method name). 
	 * 
	 * @return <code>true</code> if the current position looks like a method header.
	 */
	private boolean looksLikeMethodDecl() {
		
		nextToken();
		if (fToken == Symbols.TokenIDENT) { // method name
			do nextToken();
			while (skipBrackets()); // optional brackets for array valued return types
			return fToken == Symbols.TokenIDENT; // type name
			
		}
		return false;
	}
	
	/**
	 * Scans tokens for the matching parenthesis.
	 * 
	 * @return <code>true</code> if a matching token was found, <code>false</code> otherwise
	 */
	private boolean skipScope(int openToken, int closeToken) {
		
		int depth= 1;

		while (true) {
			nextToken();
			
			if (fToken == closeToken) {
				depth++;
			} else if (fToken == openToken) {
				depth--;
				if (depth == 0)
					return true;
			} else if (fToken == Symbols.TokenEOF) {
					return false;
			}
		}
	}

	private boolean prefAlignCaseWithSwitch() {
		// TODO preference lookup
		return false;
	}

	private int prefArrayDimensionIndent() {
		// TODO preference lookup
		return 2;
	}

	private boolean prefArrayDimensionsDeepIndent() {
		// TODO preference lookup
		return true;
	}

	private boolean prefMethodDeclDeepIndent() {
		// TODO preference lookup
		return true;
	}

	private int prefCallContinuationIndent() {
		// TODO preference lookup
		return 2;
	}

	private int prefArrayIndent() {
		// TODO preference lookup
		return 2;
	}

	private boolean prefArrayDeepIndent() {
		// TODO preference lookup
		return false;
	}

	private boolean prefTenaryDeepAlign() {
		// TODO preference lookup
		return true;
	}

}
