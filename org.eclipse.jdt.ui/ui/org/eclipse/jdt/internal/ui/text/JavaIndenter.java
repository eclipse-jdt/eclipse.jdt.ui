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

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Uses the {@link org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner}to
 * get the indentation level for a certain position in a document.
 * 
 * <p>
 * An instance holds some internal position in the document and is therefore
 * not threadsafe.
 * </p>
 * 
 * @since 3.0
 */
public class JavaIndenter {

	/** The document being scanned. */
	private IDocument fDocument;
	/** The indentation accumulated by <code>findPreviousIndenationUnit</code>. */
	private int fIndent;
	/**
	 * The absolute (character-counted) indentation offset for special cases
	 * (method defs, array initializers)
	 */
	private int fAlign;
	/** The stateful scanposition for the indentation methods. */
	private int fPosition;
	/** The previous position. */
	private int fPreviousPos;
	/** The most recent token. */
	private int fToken;
	/** The line of <code>fPosition</code>. */
	private int fLine;
	/**
	 * The scanner we will use to scan the document. It has to be installed
	 * on the same document as the one we get.
	 */
	private JavaHeuristicScanner fScanner;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param document the document to scan
	 * @param scanner the {@link JavaHeuristicScanner} to be used for scanning
	 * the document. It must be installed on the same <code>IDocument</code>.
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
	 * @param offset the offset in the document
	 * @return a String which reflects the indentation at the line in which the
	 *         reference position to <code>offset</code> resides, or <code>null</code>
	 *         if it cannot be determined
	 */
	public StringBuffer getReferenceIndentation(int offset) {
		
		int unit= findReferencePosition(offset, true);
		
		// if we were unable to find anything, return null
		if (unit == JavaHeuristicScanner.NOT_FOUND)
			return null;
		
		return getLeadingWhitespace(unit);
	}
	
	/**
	 * Computes the indentation at <code>offset</code>.
	 * 
	 * @param offset the offset in the document
	 * @return a String which reflects the correct indentation for the line in
	 *         which offset resides, or <code>null</code> if it cannot be
	 *         determined
	 */
	public StringBuffer computeIndentation(int offset) {
		
		StringBuffer indent= getReferenceIndentation(offset);
		
		// handle special alignment
		if (fAlign != JavaHeuristicScanner.NOT_FOUND) {
			try {
				// a special case has been detected.
				IRegion line= fDocument.getLineInformationOfOffset(fAlign);
				int lineOffset= line.getOffset();
				return createIndent(lineOffset, fAlign);
			} catch (BadLocationException e) {
				return null;
			}
		}
		
		if (indent == null)
			return null;
		
		// add additional indent
		indent.append(createIndent(fIndent));
		if (fIndent < 0)
			unindent(indent);
		
		return indent;
	}

	/**
	 * Returns the indentation of the line at <code>offset</code> as a
	 * <code>StringBuffer</code>. If the offset is not valid, the empty string
	 * is returned.
	 * 
	 * @param offset the offset in the document
	 * @return the indentation (leading whitespace) of the line in which
	 * 		   <code>offset</code> is located
	 */
	private StringBuffer getLeadingWhitespace(int offset) {
		StringBuffer indent= new StringBuffer();
		try {
			IRegion line= fDocument.getLineInformationOfOffset(offset);
			int lineOffset= line.getOffset();
			int nonWS= fScanner.findNonWhitespaceForwardInAnyPartition(lineOffset, lineOffset + line.getLength());
			indent.append(fDocument.get(lineOffset, nonWS - lineOffset));
			return indent;
		} catch (BadLocationException e) {
			return indent;
		}
	}

	/**
	 * Reduces indentation in <code>indent</code> by one indentation unit.
	 * 
	 * @param indent the indentation to be modified
	 */
	private void unindent(StringBuffer indent) {
		CharSequence oneIndent= createIndent(1);
		int i= indent.lastIndexOf(oneIndent.toString()); //$NON-NLS-1$
		if (i != -1) {
			indent.delete(i, i + oneIndent.length());
		}			
	}

	/**
	 * Creates an indentation string of the length indent - start + 1,
	 * consisting of the content in <code>fDocument</code> in the range
	 * [start, indent), with every character replaced by a space except for
	 * tabs, which are kept as such.
	 * 
	 * <p>Every run of the number of spaces that make up a tab are replaced
	 * by a tab character.</p>
	 * 
	 * @return the indentation corresponding to the document content specified
	 *         by <code>start</code> and <code>indent</code>
	 */
	private StringBuffer createIndent(int start, int indent) {
		int tabLen= prefTabLength();		
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
			// remainder
			if (spaces == tabLen)
				ret.append('\t');
			else
				while (spaces-- > 0)
					ret.append(' ');
			
		} catch (BadLocationException e) {
		}
		
		return ret;
	}

	/**
	 * Creates a string that represents the given number of indents (can be
	 * spaces or tabs..)
	 * 
	 * @param indent the requested indentation level.
	 * 
	 * @return the indentation specified by <code>indent</code>
	 */
	private CharSequence createIndent(int indent) {
		// get a sensible default when running without the infrastructure for testing
		if (JavaPlugin.getDefault() == null) {
			StringBuffer ret= new StringBuffer();
			while (indent-- > 0)
				ret.append('\t');
			
			return ret;
		}
			
		return CodeFormatterUtil.createIndentString(indent);
	}
	
	/**
	 * Returns the reference position regarding to indentation for <code>offset</code>,
	 * or <code>NOT_FOUND</code>. This method calls
	 * {@link #findReferencePosition(int, boolean) findReferencePosition(offset, false)}
	 * 
	 * @param offset the offset for which the reference is computed
	 * @return the reference statement relative to which <code>offset</code>
	 *         should be indented, or {@link JavaHeuristicScanner#NOT_FOUND}
	 */
	public int findReferencePosition(int offset) {
		return findReferencePosition(offset, false);
	}
	
	/**
	 * Returns the reference position regarding to indentation for <code>position</code>,
	 * or <code>NOT_FOUND</code>.
	 * 
	 * <p>If <code>peekNextChar</code> is <code>true</code>, the next token after
	 * <code>offset</code> is read and taken into account when computing the
	 * indentation. Currently, if the next token is the first token on the line
	 * (i.e. only preceded by whitespace), the following tokens are specially
	 * handled:
	 * <ul>
	 * 	<li><code>switch</code> labels are indented relative to the switch block</li>
	 * 	<li>opening curly braces are aligned correctly with the introducing code</li>
	 * 	<li>closing curly braces are aligned properly with the introducing code of
	 * 		the matching opening brace</li>
	 * 	<li>closing parenthesis' are aligned with their opening peer</li>
	 * 	<li>the <code>else</code> keyword is aligned with its <code>if</code>, anything
	 * 		else is aligned normally (i.e. with the base of any introducing statements).</li>
	 *  <li>if there is no token on the same line after <code>offset</code>, the indentation
	 * 		is the same as for an <code>else</code> keyword</li>
	 * </ul>
	 * 
	 * @param offset the offset for which the reference is computed
	 * @param peekNextChar whether to take the next character (closing
	 *        brace etc.) on the same line as <code>offset</code> into account
	 * 		  and handle indentation accordingly
	 * @return the reference statement relative to which <code>offset</code>
	 *         should be indented, or {@link JavaHeuristicScanner#NOT_FOUND}
	 */
	public int findReferencePosition(int offset, boolean peekNextChar) {
		boolean danglingElse= false;
		boolean unindent= false;
		boolean matchBrace= false;
		boolean matchParen= false;
		boolean matchCase= false;
		
			// account for unindenation characters already typed in, but after position
			// if they are on a line by themselves, the indentation gets adjusted
			// accordingly
			//
			// also account for a dangling else 
		if (peekNextChar) {
			if (offset < fDocument.getLength()) {
				try {
					IRegion line= fDocument.getLineInformationOfOffset(offset);
					int lineOffset= line.getOffset();
					int next= fScanner.nextToken(offset, lineOffset + line.getLength());
					int prevPos= Math.max(offset - 1, 0);
					boolean isFirstTokenOnLine= fDocument.get(lineOffset, prevPos + 1 - lineOffset).trim().length() == 0;
					switch (next) {
						case Symbols.TokenEOF:
						case Symbols.TokenELSE:
							danglingElse= true;
							break;
						case Symbols.TokenCASE:
						case Symbols.TokenDEFAULT:
							if (isFirstTokenOnLine)
								matchCase= true;
							break;
						case Symbols.TokenLBRACE: // for opening-brace-on-new-line style
							if (fScanner.isBracelessBlockStart(prevPos, JavaHeuristicScanner.UNBOUND))
								unindent= true;
							if (fScanner.previousToken(prevPos, JavaHeuristicScanner.UNBOUND) == Symbols.TokenCOLON)
								unindent= true;
							break;
						case Symbols.TokenRBRACE: // closing braces get unindented
							if (isFirstTokenOnLine)
								matchBrace= true;
							break;
						case Symbols.TokenRPAREN:
							if (isFirstTokenOnLine)
								matchParen= true;
							break;
					}
				} catch (BadLocationException e) {
				}
			} else {
				// assume an else could come if we are at the end of file
				danglingElse= true; 
			}
		}
		
		int ref= findReferencePosition(offset, danglingElse, matchBrace, matchParen, matchCase);
		if (unindent)
			fIndent--;
		return ref;
	}
	
	/**
	 * Returns the reference position regarding to indentation for <code>position</code>,
	 * or <code>NOT_FOUND</code>.<code>fIndent</code> will contain the
	 * relative indentation (in indentation units, not characters) after the
	 * call. If there is a special alignment (e.g. for a method declaration
	 * where parameters should be aligned), <code>fAlign</code> will contain
	 * the absolute position of the alignment reference in <code>fDocument</code>,
	 * otherwise <code>fAlign</code> is set to <code>JavaHeuristicScanner.NOT_FOUND</code>.
	 * 
	 * @param position the position for which the reference is computed
	 * @param danglingElse whether a dangling else should be assumed at <code>position</code>
	 * @param matchBrace whether the position of the matching brace should be
	 *            returned instead of doing code analysis
	 * @param matchBrace whether the position of the matching parenthesis
	 *            should be returned instead of doing code analysis
	 * @param matchCase whether the position of a switch statement reference
	 *            should be returned (either an earlier case statement or the
	 *            switch block brace)
	 * @return the reference statement relative to which <code>position</code>
	 *         should be indented, or {@link JavaHeuristicScanner#NOT_FOUND}
	 */
	private int findReferencePosition(int offset, boolean danglingElse, boolean matchBrace, boolean matchParen, boolean matchCase) {
		fIndent= 0; // the indentation modification
		fAlign= JavaHeuristicScanner.NOT_FOUND;
		fPosition= offset;
		
		// forward cases
		// an unindentation happens sometimes if the next token is special, namely on braces, parens and case labels
		// align braces
		if (matchBrace) {
			skipScope(Symbols.TokenLBRACE, Symbols.TokenRBRACE);
			return skipToStatementStart(true, true);
		}
		
		// align parenthesis'
		if (matchParen) {
			skipScope(Symbols.TokenLPAREN, Symbols.TokenRPAREN);
			return fPosition;
		}
		
		// the only reliable way to get case labels aligned (due to many different styles of using braces in a block)
		// is to go for another case statement, or the scope opening brace
		if (matchCase) {
			return matchCaseAlignment();
		}
		
		nextToken();
		switch (fToken) {
			case Symbols.TokenRBRACE:
				// skip the block and fall through
				skipScope();
			case Symbols.TokenSEMICOLON:
				// this is the 90% case: after a statement block 
				// the end of the previous statement / block previous.end
				// search to the end of the statement / block before the previous; the token just after that is previous.start
				return skipToStatementStart(danglingElse, false);
			
			// scope introduction: special treat who special is
			case Symbols.TokenLPAREN:
			case Symbols.TokenLBRACE:
			case Symbols.TokenLBRACKET:
				return handleScopeIntroduction(offset + 1);
				
			case Symbols.TokenEOF:
				// trap when hitting start of document
				return 0;
			
			case Symbols.TokenEQUAL:
				// indent assignments
				fIndent= prefAssignmentIndent();
				return fPosition;
				
			case Symbols.TokenCOLON:
				// TODO handle ternary deep indentation
				fIndent= prefCaseBlockIndent();
				return fPosition;
			
			case Symbols.TokenQUESTIONMARK:
				if (prefTernaryDeepAlign()) {
					setFirstElementAlignment(fPosition, offset + 1);
					return fPosition;
				} else {
					fIndent= prefTernaryIndent();
					return fPosition;
				}
				
			// indentation for blockless introducers:
			case Symbols.TokenDO:
			case Symbols.TokenWHILE:
			case Symbols.TokenELSE:
				fIndent= prefSimpleIndent();
				return fPosition;
			case Symbols.TokenRPAREN:
				if (skipScope(Symbols.TokenLPAREN, Symbols.TokenRPAREN)) {
					nextToken();
					if (fToken == Symbols.TokenIF || fToken == Symbols.TokenWHILE || fToken == Symbols.TokenFOR) {
						fIndent= prefSimpleIndent();
						return fPosition;
					}
				}
				// else: fall through to default
				
			case Symbols.TokenCOMMA:
				// inside a list of some type
				// easy if there is already a list item before with its own indentation - we just align
				// if not: take the start of the list ( LPAREN, LBRACE, LBRACKET ) and either align or
				// indent by list-indent
			default:
				// inside whatever we don't know about: similar to the list case:
				// if we are inside a continued expression, then either align with a previous line that has indentation
				// or indent from the expression start line (either a scope introducer or the start of the expr).
				return skipToPreviousListItemOrListStart();
				
		}
	}

	/**
	 * Skips to the start of a statement that ends at the current position.
	 * 
	 * @param danglingElse whether to indent aligned with the last <code>if</code>
	 * @return the reference offset of the start of the statement 
	 */
	private int skipToStatementStart(boolean danglingElse, boolean isInBlock) {
		while (true) {
			nextToken();
			
			if (isInBlock) {
				switch (fToken) {
					// exit on all block introducers
					case Symbols.TokenIF:
					case Symbols.TokenELSE:
					case Symbols.TokenSYNCHRONIZED:
					case Symbols.TokenCOLON:
					case Symbols.TokenSTATIC:
					case Symbols.TokenCATCH:
					case Symbols.TokenDO:
					case Symbols.TokenWHILE:
					case Symbols.TokenFINALLY:
					case Symbols.TokenFOR:
					case Symbols.TokenTRY:
						return fPosition;
						
					case Symbols.TokenSWITCH:
						fIndent= prefCaseIndent();
						return fPosition;
				}
			}
			
			switch (fToken) {
				// scope introduction through: LPAREN, LBRACE, LBRACKET
				// search stop on SEMICOLON, RBRACE, COLON, EOF
				// -> the next token is the start of the statement (i.e. previousPos when backward scanning)
				case Symbols.TokenLPAREN:
				case Symbols.TokenLBRACE:
				case Symbols.TokenLBRACKET:
				case Symbols.TokenSEMICOLON:
				case Symbols.TokenEOF:
				case Symbols.TokenCOLON:
					return fPreviousPos;
				
				case Symbols.TokenRBRACE:
					// RBRACE is a little tricky: it can be the end of an array definition, but
					// usually it is the end of a previous block
					int pos= fPreviousPos; // store state
					if (skipScope() && looksLikeArrayInitializerIntro()) {
						continue; // its an array
					} else
						return pos; // its not - do as with all the above
					
				// scopes: skip them
				case Symbols.TokenRPAREN:
				case Symbols.TokenRBRACKET:
					skipScope();
					break;
					
				// IF / ELSE: align the position after the conditional block with the if
				// so we are ready for an else, except if danglingElse is false
				// in order for this to work, we must skip an else to its if
				case Symbols.TokenIF:
					if (danglingElse)
						return fPosition;
					else
						break;
				case Symbols.TokenELSE:
					// skip behind the next if, as we have that one covered
					skipNextIF();
					break;
				
				case Symbols.TokenDO:
					// align the WHILE position with its do
					return fPosition;
					
				case Symbols.TokenWHILE:
					// this one is tricky: while can be the start of a while loop
					// or the end of a do - while 
					pos= fPosition;
					if (hasMatchingDo()) {
						// continue searching from the DO on 
						break;
					} else {
						// continue searching from the WHILE on
						fPosition= pos;
						break;
					}
				default:
					// keep searching
					
			}

		}
	}
	
	/**
	 * Returns as a reference any previous <code>switch</code> labels (<code>case</code>
	 * or <code>default</code>) or the offset of the brace that scopes the switch 
	 * statement. Sets <code>fIndent</code> to <code>prefCaseIndent</code> upon
	 * a match.
	 *  
	 * @return the reference offset for a <code>switch</code> label
	 */
	private int matchCaseAlignment() {
		while (true) {
			nextToken();
			switch (fToken) {
				// invalid cases: another case label or an LBRACE must come before a case
				// -> bail out with the current position
				case Symbols.TokenLPAREN:
				case Symbols.TokenLBRACKET:
				case Symbols.TokenEOF:
					return fPosition;
				case Symbols.TokenLBRACE:
					// opening brace of switch statement
					fIndent= prefCaseIndent();
					return fPosition;
				case Symbols.TokenCASE:
				case Symbols.TokenDEFAULT:
					// align with previous label
					fIndent= 0;
					return fPosition;
				
				// scopes: skip them
				case Symbols.TokenRPAREN:
				case Symbols.TokenRBRACKET:
				case Symbols.TokenRBRACE:
					skipScope();
					break;
					
				default:
					// keep searching
					continue;
					
			}
		}
	}

	/**
	 * Returns the reference position for a list element. The algorithm
	 * tries to match any previous indentation on the same list. If there is none,
	 * the reference position returned is determined depending on the type of list:
	 * The indentation will either match the list scope introducer (e.g. for
	 * method declarations), so called deep indents, or simply increase the
	 * indentation by a number of standard indents. See also {@link #handleScopeIntroduction(int)}.
	 * 
	 * @return the reference position for a list item: either a previous list item
	 * that has its own indentation, or the list introduction start.
	 */
	private int skipToPreviousListItemOrListStart() {
		int startLine= fLine;
		int startPosition= fPosition;
		while (true) {
			nextToken();
			
			// if any line item comes with its own indentation, adapt to it
			if (fLine < startLine) {
				try {
					int lineOffset= fDocument.getLineOffset(startLine);
					fAlign= fScanner.findNonWhitespaceForwardInAnyPartition(lineOffset, startPosition + 1);
				} catch (BadLocationException e) {
					// ignore and return just the position
				}
				return startPosition;
			}
			
			switch (fToken) {
				// scopes: skip them
				case Symbols.TokenRPAREN:
				case Symbols.TokenRBRACKET:
				case Symbols.TokenRBRACE:
					skipScope();
					break;
				
				// scope introduction: special treat who special is
				case Symbols.TokenLPAREN:
				case Symbols.TokenLBRACE:
				case Symbols.TokenLBRACKET:
					return handleScopeIntroduction(startPosition + 1);
					
				case Symbols.TokenSEMICOLON:
					return fPosition;
				case Symbols.TokenEOF:
					return 0;
					
			}
		}
	}
	
	/**
	 * Skips a scope and positions the cursor (<code>fPosition</code>) on the 
	 * token that opens the scope. Returns <code>true</code> if a matching peer
	 * could be found, <code>false</code> otherwise. The current token when calling
	 * must be one out of <code>Symbols.TokenRPAREN</code>, <code>Symbols.TokenRBRACE</code>,
	 * and <code>Symbols.TokenRBRACKET</code>.  
	 * 
	 * @return <code>true</code> if a matching peer was found, <code>false</code> otherwise
	 */
	private boolean skipScope() {
		switch (fToken) {
			case Symbols.TokenRPAREN:
				return skipScope(Symbols.TokenLPAREN, Symbols.TokenRPAREN);
			case Symbols.TokenRBRACKET:
				return skipScope(Symbols.TokenLBRACKET, Symbols.TokenRBRACKET);
			case Symbols.TokenRBRACE:
				return skipScope(Symbols.TokenLBRACE, Symbols.TokenRBRACE);
			default:
				Assert.isTrue(false);
				return false;
		}
	}

	/**
	 * Handles the introduction of a new scope. The current token must be one out
	 * of <code>Symbols.TokenLPAREN</code>, <code>Symbols.TokenLBRACE</code>,
	 * and <code>Symbols.TokenLBRACKET</code>. Returns as the reference position
	 * either the token introducing the scope or - if available - the first
	 * java token after that.
	 * 
	 * <p>Depending on the type of scope introduction, the indentation will align
	 * (deep indenting) with the reference position (<code>fAlign</code> will be 
	 * set to the reference position) or <code>fIndent</code> will be set to 
	 * the number of indentation units.
	 * </p>
	 * 
	 * @param bound the bound for the search for the first token after the scope 
	 * introduction.
	 * @return
	 */
	private int handleScopeIntroduction(int bound) {
		switch (fToken) {
			// scope introduction: special treat who special is
			case Symbols.TokenLPAREN:
				int pos= fPosition; // store
				
				// special: method declaration deep indentation
				if (looksLikeMethodDecl()) {
					if (prefMethodDeclDeepIndent())
						return setFirstElementAlignment(pos, bound);
				} else {
					fPosition= pos;
					if (looksLikeMethodCall()) {
						if (prefMethodCallDeepIndent())
							return setFirstElementAlignment(pos, bound);
					} else if (prefParenthesisDeepIndent())
						return setFirstElementAlignment(pos, bound);
				}
				
				// normal: return the parenthesis as reference
				fIndent= prefParenthesisIndent();
				return pos;
				
			case Symbols.TokenLBRACE:
				pos= fPosition; // store
				
				// special: array initializer
				if (looksLikeArrayInitializerIntro())
					if (prefArrayDeepIndent())
						return setFirstElementAlignment(pos, bound);
					else
						fIndent= prefArrayIndent();
				else
					fIndent= prefBlockIndent();
				
				// normal: skip to the statement start before the scope introducer
				// opening braces are often on differently ending indents than e.g. a method definition
				fPosition= pos; // restore
				return skipToStatementStart(true, true); // set to true to match the first if
				
			case Symbols.TokenLBRACKET:
				pos= fPosition; // store
				
				// special: method declaration deep indentation
				if (prefArrayDimensionsDeepIndent()) {
					return setFirstElementAlignment(pos, bound);
				}
				
				// normal: return the bracket as reference
				fIndent= prefBracketIndent();
				return pos; // restore
				
			default:
				Assert.isTrue(false);
				return -1; // dummy
		}
	}

	/**
	 * Sets the deep indent offset (<code>fAlign</code>) to either the offset
	 * right after <code>scopeIntroducerOffset</code> or - if available - the
	 * first Java token after <code>scopeIntroducerOffset</code>, but before
	 * <code>bound</code>.
	 * 
	 * @param scopeIntroducerOffset the offset of the scope introducer
	 * @param bound the bound for the search for another element
	 * @return the reference position
	 */
	private int setFirstElementAlignment(int scopeIntroducerOffset, int bound) {
		int firstPossible= scopeIntroducerOffset + 1; // align with the first position after the scope intro
		fAlign= fScanner.findNonWhitespaceForwardInAnyPartition(firstPossible, bound);
		if (fAlign == JavaHeuristicScanner.NOT_FOUND)
			fAlign= firstPossible;
		return fAlign;
	}


	/**
	 * Returns <code>true</code> if the next token received after calling
	 * <code>nextToken</code> is either an equal sign or an array designator ('[]').
	 * 
	 * @return <code>true</code> if the next elements look like the start of an array definition
	 */
	private boolean looksLikeArrayInitializerIntro() {
		nextToken();
		if (fToken == Symbols.TokenEQUAL || skipBrackets()) {
			return true;
		}
		return false;
	}

	/**
	 * Skips over the next <code>if</code> keyword. The current token when calling
	 * this method must be an <code>else</code> keyword. Returns <code>true</code>
	 * if a matching <code>if</code> could be found, <code>false</code> otherwise.
	 * The cursor (<code>fPosition</code>) is set to the offset of the <code>if</code>
	 * token.
	 * 
	 * @return <code>true</code> if a matching <code>if</code> token was found, <code>false</code> otherwise
	 */
	private boolean skipNextIF() {
		Assert.isTrue(fToken == Symbols.TokenELSE);
		
		while (true) {
			nextToken();
			switch (fToken) {
				// scopes: skip them
				case Symbols.TokenRPAREN:
				case Symbols.TokenRBRACKET:
				case Symbols.TokenRBRACE:
					skipScope();
					break;
					
				case Symbols.TokenIF:
					// found it, return
					return true;
				case Symbols.TokenELSE:
					// recursively skip else-if blocks
					skipNextIF();
					break;
				
				// shortcut scope starts
				case Symbols.TokenLPAREN:
				case Symbols.TokenLBRACE:
				case Symbols.TokenLBRACKET:
				case Symbols.TokenEOF:
					return false;
				}
		}
	}


	/**
	 * while(condition); is ambiguous when parsed backwardly, as it is a valid
	 * statement by its own, so we have to check whether there is a matching
	 * do. A <code>do</code> can either be separated from the while by a
	 * block, or by a single statement, which limits our search distance.
	 * 
	 * @return <code>true</code> if the <code>while</code> currently in
	 *         <code>fToken</code> has a matching <code>do</code>.
	 */
	private boolean hasMatchingDo() {
		Assert.isTrue(fToken == Symbols.TokenWHILE);
		nextToken();
		switch (fToken) {
			case Symbols.TokenRBRACE:
				skipScope(); // and fall thru
			case Symbols.TokenSEMICOLON:
				skipToStatementStart(false, false);
				return fToken == Symbols.TokenDO;
		}
		return false;
	}
	
	/**
	 * Skips brackets if the current token is a RBRACKET. There can be nothing
	 * but whitespace in between, this is only to be used for <code>[]</code> elements.
	 * 
	 * @return <code>true</code> if a <code>[]</code> could be scanned, the
	 *         current token is left at the LBRACKET.
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
	 * Reads the next token in backward direction from the heuristic scanner
	 * and sets the fields <code>fToken, fPreviousPosition</code> and <code>fPosition</code>
	 * accordingly.
	 */
	private void nextToken() {
		nextToken(fPosition);
	}
	
	/**
	 * Reads the next token in backward direction of <code>start</code> from
	 * the heuristic scanner and sets the fields <code>fToken, fPreviousPosition</code>
	 * and <code>fPosition</code> accordingly.
	 */
	private void nextToken(int start) {
		fToken= fScanner.previousToken(start - 1, JavaHeuristicScanner.UNBOUND);
		fPreviousPos= start;
		fPosition= fScanner.getPosition() + 1;
		try {
			fLine= fDocument.getLineOfOffset(fPosition);
		} catch (BadLocationException e) {
			fLine= -1;
		}
	}
	
	/**
	 * Returns <code>true</code> if the current tokens look like a method
	 * declaration header (i.e. only the return type and method name). The
	 * heuristic calls <code>nextToken</code> and expects an identifier
	 * (method name) and a type declaration (an identifier with optional 
	 * brackets) which also covers the visibility modifier of constructors; it
	 * does not recognize package visible constructors. 
	 * 
	 * @return <code>true</code> if the current position looks like a method
	 *         declaration header.
	 */
	private boolean looksLikeMethodDecl() {
		/*
		 * TODO This heuristic does not recognize package private constructors
		 * since those do have neither type nor visibility keywords.
		 * One option would be to go over the parameter list, but that might
		 * be empty as well - hard to do without an AST...
		 */
		
		nextToken();
		if (fToken == Symbols.TokenIDENT) { // method name
			do nextToken();
			while (skipBrackets()); // optional brackets for array valued return types
			return fToken == Symbols.TokenIDENT; // type name
			
		}
		return false;
	}
	
	/**
	 * Returns <code>true</code> if the current tokens look like a method
	 * call header (i.e. an identifier as opposed to a keyword taking parenthesized
	 * parameters such as <code>if</code>).
	 * <p>The heuristic calls <code>nextToken</code> and expects an identifier
	 * (method name).
	 * 
	 * @return <code>true</code> if the current position looks like a method call
	 *         header.
	 */
	private boolean looksLikeMethodCall() {
		nextToken();
		return fToken == Symbols.TokenIDENT; // method name
	}
	
	/**
	 * Scans tokens for the matching opening peer. The internal cursor
	 * (<code>fPosition</code>) is set to the offset of the opening peer if found.
	 * 
	 * @return <code>true</code> if a matching token was found, <code>false</code>
	 *         otherwise
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

	private int prefTabLength() {
		int tabLen;
		// TODO adjust once this is a per-project setting
		JavaPlugin plugin= JavaPlugin.getDefault();
		if (plugin != null)
			if (plugin.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SPACES_FOR_TABS))
				tabLen= -1; // results in no tabs being substituted for space runs
			else
				tabLen= plugin.getPreferenceStore().getInt(PreferenceConstants.EDITOR_TAB_WIDTH);
		else
			tabLen= 4; // sensible default for testing

		return tabLen;
	}
	
	// TODO add preference lookup or probing

	private boolean prefArrayDimensionsDeepIndent() {
		return true; // no real pref - real option at all?
	}

	private int prefArrayIndent() {
		return 2; // default: double indent arrays
	}

	private boolean prefArrayDeepIndent() {
		return true; // sensible default
	}

	private boolean prefTernaryDeepAlign() {
		return true; // sensible default
	}

	private int prefTernaryIndent() {
		return 2; // default: double indent conditional expressions
	}

	private int prefCaseIndent() {
		return 0; // default: sun default is not to indent cases
	}

	private int prefAssignmentIndent() {
		return prefBlockIndent();
	}

	private int prefCaseBlockIndent() {
		return prefBlockIndent();
	}

	private int prefSimpleIndent() {
		return prefBlockIndent();
	}

	private int prefBracketIndent() {
		return prefBlockIndent();
	}

	private boolean prefMethodDeclDeepIndent() {
		return true; // sensible default
	}

	private boolean prefMethodCallDeepIndent() {
		return false; // sensible default
	}

	private boolean prefParenthesisDeepIndent() {
		return false; // sensible default
	}

	private int prefParenthesisIndent() {
		return prefContinuationIndent();
	}

	private int prefBlockIndent() {
		return 1; // sensible default
	}
	
	private int prefContinuationIndent() {
		return 1; // sensible default
	}

}
