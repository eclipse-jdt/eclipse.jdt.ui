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
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

/**
  */
public class ASTRewriteAnalyzer extends ASTVisitor {

	private static final String KEY= "ASTChangeData";
	
	private static final int INSERT= 1;
	private static final int REPLACE= 2;

	public static void markAsInserted(ASTNode node) {
		node.setProperty(KEY, new ASTRewriteInfo(INSERT, null));
	}
	
	public static void markAsReplaced(ASTNode node, ASTNode modifiedNode) {
		node.setProperty(KEY, new ASTRewriteInfo(REPLACE, modifiedNode));
	}
	
	public static boolean isInserted(ASTNode node) {
		ASTRewriteInfo info= (ASTRewriteInfo) node.getProperty(KEY);
		return info != null && info.operation == INSERT;
	}
	
	public static boolean isReplaced(ASTNode node) {
		ASTRewriteInfo info= (ASTRewriteInfo) node.getProperty(KEY);
		return info != null && info.operation == REPLACE;
	}
	
	public static boolean isInsertOrReplace(ASTNode node) {
		ASTRewriteInfo info= (ASTRewriteInfo) node.getProperty(KEY);
		return info != null && (info.operation == REPLACE || info.operation == INSERT);
	}	
		
	public static ASTNode getChangedNode(ASTNode node) {
		ASTRewriteInfo info= (ASTRewriteInfo) node.getProperty(KEY);
		return info != null ? info.modifiedNode : null;
	}
	
		
	private static class ASTRewriteInfo {
		public int operation;
		public ASTNode modifiedNode;
		
		public ASTRewriteInfo(int op, ASTNode modifiedNode) {
			this.operation= op;
			this.modifiedNode= modifiedNode;
		}
	}	

	private CompilationUnitChange fChange;
	private TextBuffer fTextBuffer;
	
	private final int[] MODIFIERS= new int[] { ITerminalSymbols.TokenNamepublic, ITerminalSymbols.TokenNameprotected, ITerminalSymbols.TokenNameprivate,
		ITerminalSymbols.TokenNamestatic, ITerminalSymbols.TokenNamefinal, ITerminalSymbols.TokenNameabstract, ITerminalSymbols.TokenNamenative,
		ITerminalSymbols.TokenNamevolatile, ITerminalSymbols.TokenNamestrictfp, ITerminalSymbols.TokenNametransient, ITerminalSymbols.TokenNamesynchronized };

	/**
	 * Constructor for ASTChangeAnalyzer.
	 */
	public ASTRewriteAnalyzer(TextBuffer textBuffer, CompilationUnitChange change) {
		fTextBuffer= textBuffer;
		fChange= change;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		Expression expression= node.getExpression();
		if (expression != null) {
			if (isReplaced(expression)) {
				replaceNode(expression, (Expression) getChangedNode(expression));
			} else if (isInserted(expression)) {
				insertNode(expression, node.getStartPosition(), new int[] { ITerminalSymbols.TokenNamereturn });
			} else {
				expression.accept(this);
			}
		}
		return false;
	}	
	
	private void insertNode(ASTNode inserted, int offset, int[] prevTokens) {
		try {
			IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), offset);
			ASTResolving.overreadToken(scanner, prevTokens);
		
			int pos= scanner.getCurrentTokenStartPosition();
			String str= generateSource(inserted, 0);
			if (Character.isLetterOrDigit(fTextBuffer.getChar(pos))) {
				str= str + " ";
			}
			if (pos > 0 && Character.isLetterOrDigit(fTextBuffer.getChar(pos - 1))) {
				str= " " + str;
			}			
			
			fChange.addTextEdit("Add Node", SimpleTextEdit.createInsert(pos, str));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		} catch (InvalidInputException e) {
			JavaPlugin.log(e);
		}
	}
				
			
	private void replaceNode(ASTNode old, ASTNode modified) {
		if (modified == null) {
			int startPos= old.getStartPosition();
			int endPos= startPos + old.getLength();
			TextRegion lineStart= fTextBuffer.getLineInformationOfOffset(startPos);
			while (startPos > lineStart.getOffset() && Character.isWhitespace(fTextBuffer.getChar(startPos - 1))) {
				startPos--;
			}
			fChange.addTextEdit("Remove Node", SimpleTextEdit.createReplace(startPos, endPos - startPos, ""));
		} else {
			String str= generateSource(modified, 0);
			fChange.addTextEdit("Replace Node", SimpleTextEdit.createReplace(old.getStartPosition(), old.getLength(), str));
		}
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Block)
	 */
	public boolean visit(Block block) {
		List list= block.statements();

		Statement last= null; 
		for (int i= 0; i < list.size(); i++) {
			Statement elem= (Statement) list.get(i);
			if (isReplaced(elem)) {
				replaceStatement(elem, (Statement) getChangedNode(elem));
				last= elem;
			} else if (isInserted(elem)) {
				insertStatement(elem, last, block.getStartPosition(), false);
			} else {
				elem.accept(this);
				last= elem;
			}
		}		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration methodDecl) {
		Type returnType= methodDecl.getReturnType();
		if (isReplaced(returnType)) {
			replaceNode(returnType, getChangedNode(returnType));
		} else if (isInserted(returnType)) {
			insertNode(returnType, methodDecl.getStartPosition(), MODIFIERS);
		}
		
		SimpleName simpleName= methodDecl.getName();
		if (isReplaced(simpleName)) {
			replaceNode(simpleName, getChangedNode(simpleName));
		}
		
		List parameters= methodDecl.parameters();
		List exceptions= methodDecl.thrownExceptions();
		
		boolean changedParams= hasChanges(parameters);
		boolean changedExc= hasChanges(exceptions);
		
		if (changedParams || changedExc) {
			try {
				int offset= simpleName.getStartPosition() + simpleName.getLength();
				IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), offset);
				
				if (changedParams) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameLPAREN);
					rewriteList(scanner.getCurrentTokenEndPosition() + 1, "", parameters);
				}
				if (changedExc) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameRPAREN);
					rewriteList(scanner.getCurrentTokenEndPosition() + 1, " throws ", exceptions);
				}
				
			} catch (InvalidInputException e) {
				// ignore
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}

		Block body= methodDecl.getBody();
		if (body != null) {
			rewriteMethodBody(methodDecl, body);
		}				
		return false;
	}

	public void rewriteMethodBody(MethodDeclaration methodDecl, Block body) {
		if (isInsertOrReplace(body)) {
			try {
				IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), methodDecl.getStartPosition());
				if (isInserted(body)) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameSEMICOLON);
					int startPos= scanner.getCurrentTokenStartPosition();
					int endPos= methodDecl.getStartPosition() + methodDecl.getLength();					
					String str= " " + Strings.trimLeadingTabsAndSpaces(generateSource(body, getIndent(methodDecl.getStartPosition())));
					fChange.addTextEdit("Insert body", SimpleTextEdit.createReplace(startPos, endPos - startPos, str));
				} else if (isReplaced(body)) {
					ASTNode changed= getChangedNode(body);
					if (changed == null) {
						fChange.addTextEdit("Remove body", SimpleTextEdit.createReplace(body.getStartPosition(), body.getLength(), ";"));
					} else {
						String str= Strings.trimLeadingTabsAndSpaces(generateSource(changed, getIndent(body.getStartPosition())));
						fChange.addTextEdit("Replace body", SimpleTextEdit.createReplace(body.getStartPosition(), body.getLength(), str));
					}
				}
			} catch (InvalidInputException e) {
				// ignore
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}					
		} else {
			body.accept(this);
		}
	}
	
	private int getNextExistingStartPos(List list, int startIndex, int defaultPos) {
		for (int i= startIndex; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (!isInserted(elem)) {
				return elem.getStartPosition();
			}
		}
		return defaultPos;
	}
	
	private boolean hasChanges(List list) {
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isInsertOrReplace(elem)) {
				return true;
			}
		}
		return false;
	}
	
	private void rewriteList(int startPos, String keyword, List list) {
		int currPos= startPos;
		int endPos= startPos;
		
		// count number of nodes before and after the rewrite
		int before= 0;
		int after= 0;
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isInserted(elem)) {
				after++;
			} else {
				before++;
				if (before == 1) { // first entry
					currPos= elem.getStartPosition();
				}
				if (!isReplaced(elem) || getChangedNode(elem) != null) {
					after++;
				}
			}
			endPos= elem.getStartPosition() + elem.getLength();
		}
		
		if (after == 0) {
			if (before != 0) { // deleting the list
				fChange.addTextEdit("Remove all", SimpleTextEdit.createReplace(startPos, endPos - startPos, ""));
			}
			return;
		}
		
		if (before == 0) { // creating a new list -> insert keyword first (e.g. " throws ")
			fChange.addTextEdit("keyword", SimpleTextEdit.createInsert(startPos, keyword));
		}
		
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isInserted(elem)) {
				after--;
				String str= generateSource(elem, 0);
				if (after != 0) { // not the last that will be entered
					str= str + ", ";
				}
				fChange.addTextEdit("Insert", SimpleTextEdit.createInsert(currPos, str));
			} else {
				before--;
				int currEnd= elem.getStartPosition() + elem.getLength();
				int nextStart= getNextExistingStartPos(list, i + 1, currEnd); // start of next 
				
				if (isReplaced(elem)) {
					ASTNode changed= getChangedNode(elem);
					if (changed == null) {
						fChange.addTextEdit("Remove", SimpleTextEdit.createReplace(currPos, nextStart - currPos, ""));
					} else {
						after--;
						
						String str= generateSource(changed, 0);
						if (after == 0) { // will be last node -> remove comma
							fChange.addTextEdit("Replace", SimpleTextEdit.createReplace(currPos, nextStart - currPos, str));
						} else if (before == 0) { // was last, but not anymore -> add comma
							fChange.addTextEdit("Replace", SimpleTextEdit.createReplace(currPos, currEnd - currPos, str + ", "));
						} else {
							fChange.addTextEdit("Replace", SimpleTextEdit.createReplace(currPos, currEnd - currPos, str));
						}
					}
				} else { // no change
					after--;
										
					if (after == 0 && before != 0) { // will be last node -> remove comma
						fChange.addTextEdit("Remove comma", SimpleTextEdit.createReplace(currEnd, nextStart - currEnd, ""));
					} else if (after != 0 && before == 0) { // was last, but not anymore -> add comma
						fChange.addTextEdit("Add comma", SimpleTextEdit.createInsert(currEnd, ", "));
					}					
				}
				currPos= nextStart;
			}
		}
	}
	
	private void replaceStatement(ASTNode elem, ASTNode changed) {
		if (changed == null) {
			int start= elem.getStartPosition();
			int end= start + elem.getLength();
			
			TextRegion endRegion= fTextBuffer.getLineInformationOfOffset(end);
			int lineEnd= endRegion.getOffset() + endRegion.getLength();
			// move end to include all spaces and tabs
			while (end < lineEnd && Character.isWhitespace(fTextBuffer.getChar(end))) {
				end++;
			}
			if (lineEnd == end) { // if there is no comment / other statement remove the line (indent + new line)
				int startLine= fTextBuffer.getLineOfOffset(start);
				if (startLine > 0) {
					TextRegion prevRegion= fTextBuffer.getLineInformation(startLine - 1);
					int cutPos= prevRegion.getOffset() + prevRegion.getLength();
					String str= fTextBuffer.getContent(cutPos, start - cutPos);
					if (Strings.containsOnlyWhitespaces(str)) {
						start= cutPos;
					}
				}
			}
			
			fChange.addTextEdit("Remove Statement", SimpleTextEdit.createReplace(start, end - start, ""));
		} else {
			int startLine= fTextBuffer.getLineOfOffset(elem.getStartPosition());
			int indent= fTextBuffer.getLineIndent(startLine, CodeFormatterUtil.getTabWidth());
			String str= Strings.trimLeadingTabsAndSpaces(generateSource(changed, indent));
			fChange.addTextEdit("Replace Statement", SimpleTextEdit.createReplace(elem.getStartPosition(), elem.getLength(), str));
		}
	}

	private void insertStatement(ASTNode elem, ASTNode sibling, int parentBracketPos, boolean additionalNewLine) {
		int insertPos;
		int indent;
		if (sibling == null) {
			indent= getIndent(parentBracketPos) + 1;
			insertPos= parentBracketPos + 1;
		} else {
			indent= getIndent(sibling.getStartPosition());
			insertPos= sibling.getStartPosition() + sibling.getLength();
		}

		StringBuffer buf= new StringBuffer();
		buf.append(fTextBuffer.getLineDelimiter());
		buf.append(generateSource(elem, indent));
		if (additionalNewLine) {
			buf.append(fTextBuffer.getLineDelimiter());
		}
		
		fChange.addTextEdit("Add Statement", SimpleTextEdit.createInsert(insertPos, buf.toString()));
	}

	private int getIndent(int pos) {
		int line= fTextBuffer.getLineOfOffset(pos);
		return fTextBuffer.getLineIndent(line, CodeFormatterUtil.getTabWidth());
	}

	
	private String generateSource(ASTNode node, int indent) {
		String str= ASTNodes.asString(node);
		return StubUtility.codeFormat(str, indent, fTextBuffer.getLineDelimiter());
	}
	

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration typeDecl) {
		SimpleName simpleName= typeDecl.getName();
		if (isReplaced(simpleName)) {
			replaceNode(simpleName, getChangedNode(simpleName));
		}
	
		Name superClass= typeDecl.getSuperclass();
		if (!typeDecl.isInterface() && superClass != null) {
			if (isInserted(superClass)) {
				String str= " extends " + ASTNodes.asString(superClass);
				int pos= simpleName.getStartPosition() + simpleName.getLength();
				fChange.addTextEdit("Insert Supertype", SimpleTextEdit.createInsert(pos, str));
			} else if (isReplaced(superClass)) {
				ASTNode changed= getChangedNode(superClass);
				if (changed == null) {
					int startPos= simpleName.getStartPosition() + simpleName.getLength();
					int endPos= superClass.getStartPosition() + superClass.getLength();
					fChange.addTextEdit("Remove Supertype", SimpleTextEdit.createReplace(startPos, endPos - startPos, ""));
				} else {
					String str= ASTNodes.asString(changed);
					fChange.addTextEdit("Replace Supertype", SimpleTextEdit.createReplace(superClass.getStartPosition(), superClass.getLength(), str));
				}
			}
		}
		List interfaces= typeDecl.superInterfaces();
		if (hasChanges(interfaces)) {
			int startPos;
			if (typeDecl.isInterface() || superClass == null) {
				startPos= simpleName.getStartPosition() + simpleName.getLength();
			} else {
				startPos= superClass.getStartPosition() + superClass.getLength();
			}
			rewriteList(startPos, " implements ", interfaces);
		}
		
		List members= typeDecl.bodyDeclarations();
		ASTNode last= null;
		for (int i= 0; i < members.size(); i++) {
			ASTNode elem= (ASTNode) members.get(i);
			if (isInserted(elem)) {
				int offset= typeDecl.getStartPosition() + typeDecl.getLength() - 1;
				if (last == null) {
					try {
						int pos= simpleName.getStartPosition() + simpleName.getLength();
						IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), pos);
						ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameLBRACE);		
						
						offset= scanner.getCurrentTokenStartPosition();
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					} catch (InvalidInputException e) {
						// ignore
					}
				}
				insertStatement(elem, last, offset, true);
			} else {
				last= elem;
				if (isReplaced(elem)) {
					replaceStatement(elem, getChangedNode(elem));
				} else {
					elem.accept(this);
				}
			}
		}	
		return false;
	}

}
