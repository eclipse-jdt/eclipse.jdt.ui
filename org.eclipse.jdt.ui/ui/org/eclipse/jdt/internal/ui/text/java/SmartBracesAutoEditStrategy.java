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
package org.eclipse.jdt.internal.ui.text.java;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaCodeReader;

/**
 * An auto edit strategy which inserts the closing brace automatically if possible.
 * @deprecated
 */
public final class SmartBracesAutoEditStrategy implements IAutoEditStrategy {
	
	private static class CompilationUnitInfo {
		
		public char[] buffer;
		public int delta;
		
		public CompilationUnitInfo(char[] buffer, int delta) {
			this.buffer= buffer;
			this.delta= delta;
		}
	};

	/** The text viewer. */
	private final ITextViewer fTextViewer;
	/** The text input listener. */
	private ITextInputListener fTextInputListener;
	/** The document listener. */
	private final IDocumentListener fDocumentListener= new IDocumentListener() {
		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			fSourceRegion= null;
			fUndoEvent= null;
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
		}
	};
	/** The last changed caused by user. */
	private IRegion fSourceRegion;
	/** The undo command to undo the last change caused by auto edit strategy. */
	private DocumentEvent fUndoEvent;


	/**
	 * Creates a <code>SmartBracesAutoEditStrategy</code>
	 */
	public SmartBracesAutoEditStrategy(ITextViewer textViewer) {
		if (textViewer == null)
			throw new IllegalArgumentException();
		fTextViewer= textViewer;
	}
	
	private void install(IRegion userRegion, DocumentEvent undoEvent) {

		if (userRegion == null || undoEvent == null)
			throw new IllegalArgumentException();
		
		fSourceRegion= userRegion;
		fUndoEvent= undoEvent;

		if (fTextInputListener != null)
			return;

		IDocument document= fTextViewer.getDocument();
		document.addDocumentListener(fDocumentListener);

		fTextInputListener= new ITextInputListener() {
			/*
			 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
			 */
			public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
				if (oldInput != null)	
					oldInput.removeDocumentListener(fDocumentListener);
			}

			/*
			 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
			 */
			public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
				if (newInput != null)	
					newInput.addDocumentListener(fDocumentListener);
			}
		};
		fTextViewer.addTextInputListener(fTextInputListener);
	}

	private boolean isBackspace(DocumentCommand command) {
		return
			(command.text == null || command.text.length() == 0) &&
			fSourceRegion.getOffset() == command.offset && fSourceRegion.getLength() == command.length;
	}
	
	private boolean isDelete(DocumentCommand command) {
		return 
			(command.text == null || command.text.length() == 0) &&
			fSourceRegion.getOffset() + fSourceRegion.getLength() == command.offset &&
			command.length > 0;
	}

	private boolean isClosingBracket(DocumentCommand command) {
		return
			command.offset == fSourceRegion.getOffset() + fSourceRegion.getLength() &&
			command.length <= 1 && "}".equals(command.text); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		try {
			if (!command.doit)
				return;
			if (fSourceRegion != null) {
				
				if (isBackspace(command) || isClosingBracket(command)) {
					// restore
					command.addCommand(fUndoEvent.getOffset(), fUndoEvent.getLength(), fUndoEvent.getText(), true, fDocumentListener);
					command.owner= fDocumentListener;
					command.doit= false;
					fSourceRegion= null;
					fUndoEvent= null;
				} else if (isDelete(command)) {
					// delete magically inserted text
					command.caretOffset= command.offset;
					command.offset= fUndoEvent.getOffset();
					command.length= fUndoEvent.getLength();
					command.text= fUndoEvent.getText();
					command.owner= fDocumentListener;
					command.doit= false;
					fSourceRegion= null;
					fUndoEvent= null;
				}

			} else if (command.text != null && command.text.equals("{")) //$NON-NLS-1$
				smartBraces(document, command);

		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
		}
	}

	private static String getLineDelimiter(IDocument document) {
		try {
			if (document.getNumberOfLines() > 1)
				return document.getLineDelimiter(0);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}

		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	private static String getLineIndentation(IDocument document, int line, int maxOffset) throws BadLocationException {
		int lineOffset= document.getLineOffset(line);
		String string= document.get(lineOffset, maxOffset - lineOffset);
		final int length= string.length();
		int i= 0;
		while (i != length && Character.isWhitespace(string.charAt(i)))
			++i;
		return string.substring(0, i);
	}

	private static IRegion getToken(IDocument document, IRegion scanRegion, int tokenId) throws BadLocationException {

		final String source= document.get(scanRegion.getOffset(), scanRegion.getLength());

		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(source.toCharArray());

		try {
			int id= scanner.getNextToken();
			while (id != ITerminalSymbols.TokenNameEOF && id != tokenId)
				id= scanner.getNextToken();

			if (id == ITerminalSymbols.TokenNameEOF)
				return null;

			int tokenOffset= scanner.getCurrentTokenStartPosition();
			int tokenLength= scanner.getCurrentTokenEndPosition() + 1 - tokenOffset; // inclusive end
			return new Region(tokenOffset + scanRegion.getOffset(), tokenLength);

		} catch (InvalidInputException e) {
			return null;
		}
	}

	private void addReplace(IDocument document, DocumentCommand command, int offset, int length, String text) throws BadLocationException {
		command.addCommand(offset, length, text, true, fDocumentListener); //$NON-NLS-1$
		command.owner= fDocumentListener;
		command.doit= false;
		String oldText= document.get(offset, length);

		int delta= offset <= command.offset ? 0 : (command.text == null ? 0 : command.text.length()) - command.length; 
		IRegion replacedRegion= new Region(command.offset, command.text == null ? 0 : command.text.length());
		DocumentEvent undoEvent= new DocumentEvent(document, offset + delta, text == null ? 0 : text.length(), oldText);
		install(replacedRegion, undoEvent);
	}

	/**
	 * Closes the block immediately on the next line.
	 */
	private void makeBlock(IDocument document, DocumentCommand command) throws BadLocationException {
		int offset= command.offset;
		
		int insertionLine= document.getLineOfOffset(offset);
	
		final String lineDelimiter= getLineDelimiter(document);
		final String lineIndentation= getLineIndentation(document, insertionLine, offset);
	
		final StringBuffer buffer= new StringBuffer();
		buffer.append(lineIndentation);
		buffer.append("}"); //$NON-NLS-1$
		buffer.append(lineDelimiter);
	
		// skip line delimiter, otherwise it may clash with the insertion of '{'
		int replaceOffset= document.getLineOffset(insertionLine) + document.getLineLength(insertionLine);
		addReplace(document, command, replaceOffset, 0, buffer.toString());
	}

	/**
	 * Surrounds a statement with a block.
	 */
	private void makeBlock(IDocument document, DocumentCommand command, IRegion replace, IRegion statement, IRegion nextStatement, IRegion prevStatement, boolean followingControl) throws BadLocationException {

		final int insertionLine= document.getLineOfOffset(replace.getOffset());
		final int statementLine= document.getLineOfOffset(statement.getOffset() + statement.getLength());

		final String outerSpace= (prevStatement.getOffset() + prevStatement.getLength() == replace.getOffset()) ? "" : " "; //$NON-NLS-1$ //$NON-NLS-2$
		final String innerSpace= (replace.getOffset() + replace.getLength() == statement.getOffset()) ? "" : " "; //$NON-NLS-1$ //$NON-NLS-2$

		switch (statementLine - insertionLine) {
			// statement on same line
			case 0:
				{
					int replaceOffset= statement.getOffset() + statement.getLength();
					int replaceLength= document.getChar(replaceOffset) == ' ' ? 1 : 0; // eat existing space					
					addReplace(document, command, replaceOffset, replaceLength, innerSpace + "}" + outerSpace); //$NON-NLS-1$
				}
				break;

			// more than one line distance between block begin and next statement:
			default:
			// statement on next line
			case 1:
				// statement is too far away, assume normal typing; add closing braces before statement
				
				
				/* Changed due to http://dev.eclipse.org/bugs/show_bug.cgi?id=32082 */
				
//				if (statementLineBegin - insertionLine >= 2) {
				if (true) {
					makeBlock(document, command);
					break;					
				}			
			
				final String lineDelimiter= getLineDelimiter(document);
				final String lineIndentation= getLineIndentation(document, insertionLine, replace.getOffset());

				final StringBuffer buffer= new StringBuffer();

				if (nextStatement == null) {
					buffer.append(lineDelimiter);
					buffer.append(lineIndentation);
					buffer.append("}"); //$NON-NLS-1$

					// at end of line to skip a possible comment
					IRegion region= document.getLineInformation(statementLine);
					addReplace(document, command, region.getOffset() + region.getLength(), 0, buffer.toString());

				} else {

					final int nextStatementLine=document.getLineOfOffset(nextStatement.getOffset());

					if (statementLine == nextStatementLine) {
						int replaceOffset= statement.getOffset() + statement.getLength();
						int replaceLength= document.getChar(replaceOffset) == ' ' ? 1 : 0; // eat existing space
						addReplace(document, command, replaceOffset, replaceLength, innerSpace + "}" + outerSpace); //$NON-NLS-1$
						
					} else {
						if (followingControl && JavaCore.DO_NOT_INSERT.equals(JavaCore.getOption(JavaCore.FORMATTER_NEWLINE_CONTROL))) {
							addReplace(document, command, nextStatement.getOffset(), 0, "}" + outerSpace); //$NON-NLS-1$

						} else {
							buffer.append(lineDelimiter);
							buffer.append(lineIndentation);
							buffer.append("}"); //$NON-NLS-1$

							// at end of line to skip a possible comment
							IRegion region= document.getLineInformation(statementLine);
							addReplace(document, command, region.getOffset() + region.getLength(), 0, buffer.toString());
						}
					}
				}
				break;
		}
	}

	private static Statement getNextStatement(Statement statement) {

		ASTNode node= statement.getParent();
		while (node != null && node.getNodeType() != ASTNode.BLOCK)
			node= node.getParent();

		if (node == null)
			return null;

		Block block= (Block) node;
		List statements= block.statements();
		for (final Iterator iterator= statements.iterator(); iterator.hasNext(); ) {
			final Statement nextStatement= (Statement) iterator.next();
			if (nextStatement.getStartPosition() >= statement.getStartPosition() + statement.getLength())
				return nextStatement;
		}
		return null;
	}

	private static boolean areBlocksConsistent(IDocument document, int offset) {
		JavaCodeReader reader= new JavaCodeReader();
		try { 
			int begin= offset;
			int end= offset;
			
			while (true) {
				begin= searchForOpeningPeer(reader, begin, '{', '}', document);
				end= searchForClosingPeer(reader, end, '{', '}', document);
				if (begin == -1 && end == -1)
					return true;
				if (begin == -1 || end == -1)
					return false;
			}

		} catch (IOException e) {
			return false;
		}		
	}

	private static IRegion getSurroundingBlock(IDocument document, int offset) {
		JavaCodeReader reader= new JavaCodeReader();
		try {
			int begin= searchForOpeningPeer(reader, offset, '{', '}', document);
			int end= searchForClosingPeer(reader, offset, '{', '}', document);
			if (begin == -1 || end == -1)
				return null;
			return new Region(begin, end + 1 - begin);
			
		} catch (IOException e) {
			return null;	
		}
	}

	private static CompilationUnitInfo getCompilationUnitForMethod(IDocument document, int offset) {

		try {	
			IRegion sourceRange= getSurroundingBlock(document, offset);
			if (sourceRange == null)
				return null;
			String source= document.get(sourceRange.getOffset(), sourceRange.getLength());

			StringBuffer contents= new StringBuffer();
			contents.append("class C{void m()"); //$NON-NLS-1$
			final int methodOffset= contents.length();
			contents.append(source);
			contents.append('}');
			
			char[] buffer= contents.toString().toCharArray();

			return new CompilationUnitInfo(buffer, sourceRange.getOffset() - methodOffset);

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}

		return null;
	}

	private static IRegion createRegion(ASTNode node, int delta) {
		if (node == null)
			return null;
		return new Region(node.getStartPosition() + delta, node.getLength());
	}

	private void smartBraces(IDocument document, DocumentCommand command) throws BadLocationException {

		if (! JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CLOSE_BRACES))
			return;

		final int offset= command.offset;
		final int length= command.length;
		final IRegion replace= new Region(offset, length);

		CompilationUnitInfo info= getCompilationUnitForMethod(document, offset);
		if (info == null)
			return;
			
		char[] buffer= info.buffer;
		int delta= info.delta; // offset of buffer inside document

		final CompilationUnit compilationUnit= AST.parseCompilationUnit(buffer);

		// continue only if no problems exist in this method
		IProblem[] problems= compilationUnit.getProblems();
		for (int i= 0; i != problems.length; ++i)
			if (problems[i].getID() == IProblem.UnmatchedBracket)
				return;

		ASTNode node= NodeFinder.perform(compilationUnit, offset - delta, length);
		if (node == null)
			return;

		// get truly enclosing node
		while (node != null && length == 0 && (offset - delta == node.getStartPosition() || offset - delta == node.getStartPosition() + node.getLength()))
			node= node.getParent();

		switch (node.getNodeType()) {
		case ASTNode.BLOCK:
			// normal typing: no useful AST node available
			if (areBlocksConsistent(document, offset))
				makeBlock(document, command);
			break;

		case ASTNode.IF_STATEMENT:
			{
				IfStatement ifStatement= (IfStatement) node;
				Expression expression= ifStatement.getExpression();
				Statement thenStatement= ifStatement.getThenStatement();
				Statement elseStatement= ifStatement.getElseStatement();
				Statement nextStatement= getNextStatement(ifStatement);

				IRegion expressionRegion= createRegion(expression, delta);
				IRegion thenRegion= createRegion(thenStatement, delta);
				IRegion elseRegion= createRegion(elseStatement, delta);
				IRegion nextRegion= createRegion(nextStatement, delta);

				IRegion elseToken= null;
				if (elseStatement != null) {
					int sourceOffset= thenRegion.getOffset() + thenRegion.getLength();
					int sourceLength= elseRegion.getOffset() - sourceOffset;

					elseToken= getToken(document, new Region(sourceOffset, sourceLength), ITerminalSymbols.TokenNameelse);
				}

				// between expression and then statement
				if (offset >= expressionRegion.getOffset() + expressionRegion.getLength() &&
					offset + length <= thenRegion.getOffset())
				{
					if (thenStatement == null || thenStatement.getNodeType() == ASTNode.BLOCK)
						return;

					if (elseToken != null)
						nextRegion= elseToken;

					// search for )
					int sourceOffset= expressionRegion.getOffset() + expressionRegion.getLength();
					int sourceLength= thenRegion.getOffset() - sourceOffset;
					IRegion rightParenthesisToken= getToken(document, new Region(sourceOffset, sourceLength), ITerminalSymbols.TokenNameRPAREN);

					makeBlock(document, command, replace, thenRegion, nextRegion, rightParenthesisToken, elseStatement != null);
					break;
				}

				if (elseStatement == null || elseStatement.getNodeType() == ASTNode.BLOCK)
					return;

				// between then and else and
				if (offset >= elseToken.getOffset() + elseToken.getLength() && offset + length <= elseRegion.getOffset())
					makeBlock(document, command, replace, createRegion(elseStatement, delta), nextRegion, elseToken, false);
			}
			break;

		case ASTNode.WHILE_STATEMENT:
		case ASTNode.FOR_STATEMENT:
			{
				Expression expression= node.getNodeType() == ASTNode.WHILE_STATEMENT
					? ((WhileStatement) node).getExpression()
					: ((ForStatement) node).getExpression();
				Statement body= node.getNodeType() == ASTNode.WHILE_STATEMENT
					? ((WhileStatement) node).getBody()
					: ((ForStatement) node).getBody();
				Statement nextStatement= getNextStatement((Statement) node);

				if (expression == null || body == null || body.getNodeType() == ASTNode.BLOCK)
					return;

				IRegion expressionRegion= createRegion(expression, delta);
				IRegion bodyRegion= createRegion(body, delta);
				IRegion nextRegion= createRegion(nextStatement, delta);

				// search for )
				int sourceOffset= expressionRegion.getOffset() + expressionRegion.getLength();
				int sourceLength= bodyRegion.getOffset() - sourceOffset;
				IRegion rightParenthesisToken= getToken(document, new Region(sourceOffset, sourceLength), ITerminalSymbols.TokenNameRPAREN);

				if (offset >= expressionRegion.getOffset() + expressionRegion.getLength() && offset + length <= bodyRegion.getOffset())
					makeBlock(document, command, replace, bodyRegion, nextRegion, rightParenthesisToken, false);
			}
			break;

		case ASTNode.DO_STATEMENT:
			{
				DoStatement doStatement= (DoStatement) node;
				Statement body= doStatement.getBody();
				Expression expression= doStatement.getExpression();
				//Statement nextStatement= getNextStatement((Statement) node);

				if (expression == null || body == null || body.getNodeType() == ASTNode.BLOCK)
					return;

				IRegion doRegion= createRegion(doStatement, delta);
				IRegion bodyRegion= createRegion(body, delta);
				IRegion expressionRegion= createRegion(expression, delta);
				//IRegion nextRegion= createRegion(nextStatement, delta);

				int sourceOffset= bodyRegion.getOffset() + bodyRegion.getLength();
				int sourceLength= expressionRegion.getOffset() - sourceOffset;
				IRegion whileToken= getToken(document, new Region(sourceOffset, sourceLength), ITerminalSymbols.TokenNamewhile);
				IRegion prevRegion= getToken(document, doRegion, ITerminalSymbols.TokenNamedo);

				if (offset >= doRegion.getOffset() && offset + length <= bodyRegion.getOffset())
					makeBlock(document, command, replace, bodyRegion, whileToken, prevRegion, true);
			}
			break;

		default:
			break;
		}
	}

	private static int searchForClosingPeer(JavaCodeReader reader, int offset, int openingPeer, int closingPeer, IDocument document) throws IOException {

		reader.configureForwardReader(document, offset + 1, document.getLength(), true, true);

		int stack= 1;
		int c= reader.read();
		while (c != JavaCodeReader.EOF) {
			if (c == openingPeer && c != closingPeer)
				stack++;
			else if (c == closingPeer)
				stack--;

			if (stack == 0)
				return reader.getOffset();

			c= reader.read();
		}

		return  -1;
	}

	private static int searchForOpeningPeer(JavaCodeReader reader, int offset, int openingPeer, int closingPeer, IDocument document) throws IOException {

		reader.configureBackwardReader(document, offset, true, true);

		int stack= 1;
		int c= reader.read();
		while (c != JavaCodeReader.EOF) {
			if (c == closingPeer && c != openingPeer)
				stack++;
			else if (c == openingPeer)
				stack--;

			if (stack == 0)
				return reader.getOffset();

			c= reader.read();
		}

		return -1;
	}
	
}
