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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTWithExistingFlattener.ExistingNodeMarker;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

/**
 * Infrastructure to support code modifications. Existing code must stay untouched, new code
 * added with correct formatting, moved code left with the users formattings / comments.
 * Idea:
 * - Get the AST for existing code 
 * - Inserting new nodes or mark existing nodes as replaced, removed, copied or moved.
 * - This visitor analyses the changes or annottaions and generates text edits (text manipulation
 *    API) that describe the required code changes.
 * See test cases in org.eclipse.jdt.ui.tests / package org.eclipse.jdt.ui.tests.astrewrite for
 * examples
 */
public class ASTRewriteAnalyzer extends ASTVisitor {

	private static final String KEY= "ASTChangeData";

	public static ASTNode createCopyTarget(ASTNode node) {
		return ASTWithExistingFlattener.getPlaceholder(node);
	}

	public static void markAsInserted(ASTNode node) {
		node.setSourceRange(-1, 0);
	}
	
	public static void markAsRemoved(ASTNode node) {
		markAsReplaced(node, null);
	}
	
	public static void markAsReplaced(ASTNode node, ASTNode replacingNode) {
		ASTReplace replace= new ASTReplace();
		replace.replacingNode= replacingNode;
		node.setProperty(KEY, replace);
	}
	
	public static void markAsModified(ASTNode node, ASTNode modifiedNode) {
		ASTModify modify= new ASTModify();
		modify.modifiedNode= modifiedNode;
		node.setProperty(KEY, modify);
	}	
	
	/* package */ static boolean isInsertOrReplace(ASTNode node) {
		return isInserted(node) || isReplaced(node);
	}
	
	/* package */ static boolean isInserted(ASTNode node) {
		return node.getStartPosition() == -1;
	}
	
	/* package */ static boolean isReplaced(ASTNode node) {
		return node.getProperty(KEY) instanceof ASTReplace;
	}
	
	/* package */ static boolean isRemoved(ASTNode node) {
		Object info= node.getProperty(KEY);
		return info instanceof ASTReplace && (((ASTReplace) info).replacingNode == null);
	}	
	
	/* package */ static boolean isModified(ASTNode node) {
		return node.getProperty(KEY) instanceof ASTModify;
	}	
	
	/* package */ static ASTNode getModifiedNode(ASTNode node) {
		Object info= node.getProperty(KEY);
		if (info instanceof ASTModify) {
			return ((ASTModify) info).modifiedNode;
		}
		return null;
	}

	/* package */ static ASTNode getReplacingNode(ASTNode node) {
		Object info= node.getProperty(KEY);
		if (info instanceof ASTReplace) {
			return ((ASTReplace) info).replacingNode;
		}
		return null;
	}
		
	private static final class ASTReplace {
		public ASTNode replacingNode;
	}
	
	private static final class ASTModify {
		public ASTNode modifiedNode;
	}
	
	private CompilationUnitChange fChange;
	private TextBuffer fTextBuffer;
	
	private IScanner fScanner; // shared scanner
	
	private final int[] MODIFIERS= new int[] { ITerminalSymbols.TokenNamepublic, ITerminalSymbols.TokenNameprotected, ITerminalSymbols.TokenNameprivate,
		ITerminalSymbols.TokenNamestatic, ITerminalSymbols.TokenNamefinal, ITerminalSymbols.TokenNameabstract, ITerminalSymbols.TokenNamenative,
		ITerminalSymbols.TokenNamevolatile, ITerminalSymbols.TokenNamestrictfp, ITerminalSymbols.TokenNametransient, ITerminalSymbols.TokenNamesynchronized };

	/**
	 * Constructor for ASTChangeAnalyzer.
	 */
	public ASTRewriteAnalyzer(TextBuffer textBuffer, CompilationUnitChange change) {
		fTextBuffer= textBuffer;
		fChange= change;
		fScanner= null;
	}
	
	private void addInsert(int offset, String insertString, String description) {
		fChange.addTextEdit(description, SimpleTextEdit.createInsert(offset, insertString));
	}
	
	private void addDelete(int offset, int len, String description) {
		fChange.addTextEdit(description, SimpleTextEdit.createDelete(offset, len));
	}
	
	private void addReplace(int offset, int len, String insertString, String description) {
		fChange.addTextEdit(description, SimpleTextEdit.createReplace(offset, len, insertString));
	}
	
	private void addMove(int srcOffset, int srcLen, int destOffset, String indentString, int tabWidth, String description) {
		MoveIndentedTextEdit edit= new MoveIndentedTextEdit(srcOffset, srcLen, destOffset, indentString, tabWidth);
		
		//fChange.addTextEdit(description, edit);
		// workaround: direct evaluation
		addInsert(destOffset, edit.getContent(fTextBuffer), description);
	}		

	private IScanner getScanner(int pos) {
		if (fScanner == null) {
			fScanner= ToolFactory.createScanner(false, false, false, false);
			String content= fTextBuffer.getContent();
			char[] chars= new char[fTextBuffer.getLength()];
			fTextBuffer.getContent().getChars(0, chars.length, chars, 0);
			fScanner.setSource(chars);
		}
		fScanner.resetTo(pos, fTextBuffer.getLength());
		return fScanner;
	}
	
	private void checkNoModification(ASTNode node) {
		Assert.isTrue(!isModified(node), "Can not modify " + node.getClass().getName());
	}
	
	private void checkNoInsertOrReplace(ASTNode node) {
		Assert.isTrue(!isInsertOrReplace(node), "Can not insert node in " + node.getParent().getClass().getName());
	}	
	
	
	private int rewriteNode(ASTNode node, int offset) {
		if (isInserted(node)) {
			insertNode(node, offset);
			return offset;
		}
		if (isReplaced(node)) {
			replaceNode(node, getReplacingNode(node));
		} else {
			node.accept(this);
		}
		return node.getStartPosition() + node.getLength();
	}
	
	private void insertNode(ASTNode inserted, int offset) {
		if (offset > 0 && Character.isLetterOrDigit(fTextBuffer.getChar(offset - 1))) {
			addInsert(offset, " ", "Add Space before");
		}
		addGenerated(offset, 0, inserted, getIndent(offset), true);
		if (Character.isLetterOrDigit(fTextBuffer.getChar(offset))) {
			addInsert(offset, " ", "Add Space after");
		}
	}
			
	private void replaceNode(ASTNode old, ASTNode modified) {
		int offset= old.getStartPosition();
		int len= old.getLength();
		if (modified == null) {
			// remove spaces before node if is not the ident.
			int pos= offset;
			while (pos > 0 && Strings.isIndentChar(fTextBuffer.getChar(pos - 1))) {
				pos--;
			}
			if (pos == 0 || !Character.isWhitespace(fTextBuffer.getChar(pos - 1))) {
				// is not line delim -> not the indent
				len= len + offset - pos;
				offset= pos;
			}
			addDelete(offset, len, "Remove Node");
		} else {
			addGenerated(offset, len, modified, getIndent(offset), true);
		}
	}
	
	private int rewriteOptionalQualifier(ASTNode node, int startPos) {
		if (isInserted(node)) {
			addGenerated(startPos, 0, node, getIndent(startPos), true);
			addInsert(startPos, ".", "Dot");
			return startPos;
		} else {
			if (isReplaced(node)) {
				ASTNode replacingNode= getReplacingNode(node);
				if (replacingNode == null) {
					try {
						int dotStart= node.getStartPosition() + node.getLength();
						int dotEnd= ASTResolving.getPositionAfter(getScanner(dotStart), ITerminalSymbols.TokenNameDOT);
						addDelete(startPos, dotEnd - startPos, "remove node and dot");
					} catch (InvalidInputException e) {
						JavaPlugin.log(e);
					}
				} else {
					addGenerated(startPos, node.getLength(), node, getIndent(startPos), true);
				}
			} else {
				node.accept(this);
			}
			return node.getStartPosition() + node.getLength();
		}
	}
	
	private int rewriteParagraphList(List list, int insertPos, int insertIndent) {
		ASTNode last= null; 
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem = (ASTNode) list.get(i);
			last= rewriteParagraph(elem, last, insertPos, insertIndent, false);
		}
		if (last != null) {
			return last.getStartPosition() + last.getLength();
		}
		return insertPos;
	}

	/**
	 * Rewrite a paragraph (node that is on a new line and has same indent as previous
	 */
	private ASTNode rewriteParagraph(ASTNode elem, ASTNode last, int insertPos, int insertIndent, boolean additionalNewLine) {
		if (elem == null) {
			return last;
		} else if (isInserted(elem)) {
			insertParagraph(elem, last, insertPos, insertIndent, additionalNewLine);
			return last;
		} else {
			if (isReplaced(elem)) {
				replaceParagraph(elem, getReplacingNode(elem));
			} else {
				elem.accept(this);
			}
			return elem;
		}
	}
	

	private void rewriteMethodBody(MethodDeclaration methodDecl, Block body) {
		if (isInsertOrReplace(body)) {
			try {
				IScanner scanner= getScanner(methodDecl.getStartPosition());
				if (isInserted(body)) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameSEMICOLON);
					int startPos= scanner.getCurrentTokenStartPosition();
					int endPos= methodDecl.getStartPosition() + methodDecl.getLength();					
					addDelete(startPos, endPos - startPos, "Remove semicolon");
					addInsert(startPos, " ", "Add space");
					addGenerated(startPos, 0, body, getIndent(methodDecl.getStartPosition()), true);
				} else if (isReplaced(body)) {
					ASTNode changed= getReplacingNode(body);
					if (changed == null) {
						addReplace(body.getStartPosition(), body.getLength(), ";", "Insert semicolon");
					} else {
						addGenerated(body.getStartPosition(), body.getLength(), changed, getIndent(body.getStartPosition()), true);
					}
				}
			} catch (InvalidInputException e) {
				// ignore
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
	
	private boolean isInsertFirst(List list) {
		return !list.isEmpty() && isInserted((ASTNode) list.get(0));
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
	
	private void visitList(List list) {
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			((ASTNode) iter.next()).accept(this);
		}
	}
	
	private int rewriteList(int startPos, String keyword, List list, boolean updateKeyword) {

		int currPos= startPos;
			
		// count number of nodes before and after the rewrite
		int before= 0;
		int after= 0;
		ASTNode lastExisting= null;
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isInserted(elem)) {
				after++;
			} else {
				before++;
				if (before == 1) { // first entry
					currPos= elem.getStartPosition();
				}
				if (!isRemoved(elem)) {
					after++;
				}
				lastExisting= elem;
			}
		}
		
		if (after == 0) {
			if (before != 0) { // deleting the list
				int endPos= lastExisting.getStartPosition() + lastExisting.getLength();
				addDelete(startPos, endPos - startPos, "Remove all");
				return endPos;
			}
			return startPos;
		}
		
		if (before == 0) { // creating a new list -> insert keyword first (e.g. " throws ")
			addInsert(startPos, keyword, "keyword");
		} else if (updateKeyword) {
			int firstStart= getNextExistingStartPos(list, 0, startPos);
			addReplace(startPos, firstStart - startPos, keyword, "Update keyword");
		}
		
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isInserted(elem)) {
				after--;
				addGenerated(currPos, 0, elem, 0, false);
				if (after != 0) { // not the last that will be entered
					addInsert(currPos, ", ", "Add comma");
				}
			} else {
				before--;
				int currEnd= elem.getStartPosition() + elem.getLength();
				int nextStart= getNextExistingStartPos(list, i + 1, currEnd); // start of next 
				
				if (isReplaced(elem)) {
					ASTNode changed= getReplacingNode(elem);
					if (changed == null) {
						addDelete(currPos, nextStart - currPos, "Remove");
					} else {
						after--;
						if (after == 0) { // will be last node -> remove comma
							addGenerated(currPos, nextStart - currPos, changed, 0, false);
						} else if (before == 0) { // was last, but not anymore -> add comma
							addGenerated(currPos, currEnd - currPos, changed, 0, false);
							addInsert(currPos, ", ", "Add comma");
						} else {
							addGenerated(currPos, currEnd - currPos, changed, 0, false);
						}
					}
				} else { // no change
					elem.accept(this);

					after--;
					if (after == 0 && before != 0) { // will be last node -> remove comma
						addDelete(currEnd, nextStart - currEnd, "Remove comma");
					} else if (after != 0 && before == 0) { // was last, but not anymore -> add comma
						addInsert(currEnd, ", ", "Add comma");
					}					
				}
				currPos= nextStart;
			}
		}
		return currPos;
	}
	
	private void replaceParagraph(ASTNode elem, ASTNode changed) {
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
			
			addDelete(start, end - start, "Remove");
		} else {
			int startLine= fTextBuffer.getLineOfOffset(elem.getStartPosition());
			int indent= fTextBuffer.getLineIndent(startLine, CodeFormatterUtil.getTabWidth());
			
			addGenerated(elem.getStartPosition(), elem.getLength(), changed, indent, true);
		}
	}

	private void insertParagraph(ASTNode elem, ASTNode sibling, int insertPos, int indent, boolean additionalNewLine) {
		if (sibling != null) {
			indent= getIndent(sibling.getStartPosition());
			insertPos= sibling.getStartPosition() + sibling.getLength();
		}
		addInsert(insertPos, fTextBuffer.getLineDelimiter(), "Add new line");
		addGenerated(insertPos, 0, elem, indent, false);
		if (additionalNewLine) {
			addInsert(insertPos, fTextBuffer.getLineDelimiter(), "Additional new line");
		}
	}

	private int getIndent(int pos) {
		int line= fTextBuffer.getLineOfOffset(pos);
		return fTextBuffer.getLineIndent(line, CodeFormatterUtil.getTabWidth());
	}

	private void addGenerated(int insertOffset, int insertLen, ASTNode node, int initialIndentLevel, boolean removeLeadingIndent) {		
		if (insertLen > 0) {
			addDelete(insertOffset, insertLen, "Delete existing");
		}
		
		ASTWithExistingFlattener flattener= new ASTWithExistingFlattener();
		node.accept(flattener);
		String formatted= flattener.getFormattedResult(initialIndentLevel, fTextBuffer.getLineDelimiter());
		
		ExistingNodeMarker[] markers= flattener.getExistingNodeMarkers();
		
		int tabWidth= CodeFormatterUtil.getTabWidth();
		int currPos= 0;
		if (removeLeadingIndent) {
			while (currPos < formatted.length() && Strings.isIndentChar(formatted.charAt(currPos))) {
				currPos++;
			}
		}
		for (int i= 0; i < markers.length; i++) {
			ASTNode movedNode= markers[i].existingNode;
			int offset= markers[i].offset;
			
			String insertStr= formatted.substring(currPos, offset);
			addInsert(insertOffset, insertStr, "Insert");
			String indentString=  Strings.getIndentString(getCurrentLine(formatted, offset), tabWidth);
			addMove(movedNode.getStartPosition(), movedNode.getLength(), insertOffset, indentString, tabWidth, "Move");
		
			currPos= offset + markers[i].length;
		}
		String insertStr= formatted.substring(currPos);
		addInsert(insertOffset, insertStr, "Insert");
	}
	
	private String getCurrentLine(String str, int pos) {
		for (int i= pos - 1; i>= 0; i--) {
			char ch= str.charAt(i);
			if (ch == '\n' || ch == '\r') {
				return str.substring(i + 1, pos);
			}
		}
		return str.substring(0, pos);
	}	
	

	private void rewriteModifiers(int startPos, int oldModifiers, int newModifiers) {
		if (oldModifiers == newModifiers) {
			return;
		}
		
		try {
			IScanner scanner= getScanner(startPos);
			int tok= scanner.getNextToken();
			int endPos= startPos;
			loop: while (true) {
				boolean keep= true;
				switch (tok) {
					case ITerminalSymbols.TokenNamepublic: keep= Modifier.isPublic(newModifiers); break;
					case ITerminalSymbols.TokenNameprotected: keep= Modifier.isProtected(newModifiers); break;
					case ITerminalSymbols.TokenNameprivate: keep= Modifier.isPrivate(newModifiers); break;
					case ITerminalSymbols.TokenNamestatic: keep= Modifier.isStatic(newModifiers); break;
					case ITerminalSymbols.TokenNamefinal: keep= Modifier.isFinal(newModifiers); break;
					case ITerminalSymbols.TokenNameabstract: keep= Modifier.isAbstract(newModifiers); break;
					case ITerminalSymbols.TokenNamenative: keep= Modifier.isNative(newModifiers); break;
					case ITerminalSymbols.TokenNamevolatile: keep= Modifier.isVolatile(newModifiers); break;
					case ITerminalSymbols.TokenNamestrictfp: keep= Modifier.isStrictfp(newModifiers); break;
					case ITerminalSymbols.TokenNametransient: keep= Modifier.isTransient(newModifiers); break;
					case ITerminalSymbols.TokenNamesynchronized: keep= Modifier.isSynchronized(newModifiers); break;
					default:
						break loop;
				}
				tok= scanner.getNextToken();
				int currPos= endPos;
				endPos= scanner.getCurrentTokenStartPosition();
				if (!keep) {
					addDelete(currPos, endPos - currPos, "Remove Modifier");
				}
			} 
			int addedModifiers= (newModifiers ^ oldModifiers) & newModifiers;
			if (addedModifiers != 0) {
				StringBuffer buf= new StringBuffer();
				ASTFlattener.printModifiers(addedModifiers, buf);
				addInsert(endPos, buf.toString(), "Add Modifier");
			}
		} catch (InvalidInputException e) {
			// ignore
		}		
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(CompilationUnit)
	 */ 
	public boolean visit(CompilationUnit node) {
		PackageDeclaration packageDeclaration= node.getPackage();
		ASTNode last= rewriteParagraph(packageDeclaration, null, 0, 0, true);
				
		List imports= node.imports();
		int startPos= last != null ? last.getStartPosition() + last.getLength() : 0;
		startPos= rewriteParagraphList(imports, startPos, 0);

		List types= node.types();
		rewriteParagraphList(types, startPos, 0);
		return false;
	}


	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration typeDecl) {
		
		// modifiers & class/interface
		boolean invertType= false;
		if (isModified(typeDecl)) {
			TypeDeclaration modifiedNode= (TypeDeclaration) getModifiedNode(typeDecl);
			int modfiedModifiers= modifiedNode.getModifiers();
			
			rewriteModifiers(typeDecl.getStartPosition(), typeDecl.getModifiers(), modfiedModifiers);
			if (modifiedNode.isInterface() != typeDecl.isInterface()) { // change from class to interface or reverse
				invertType= true;
				try {
					IScanner scanner= getScanner(typeDecl.getStartPosition());
					int typeToken= typeDecl.isInterface() ? ITerminalSymbols.TokenNameinterface : ITerminalSymbols.TokenNameclass;
					ASTResolving.readToToken(scanner, typeToken);
					
					String str= typeDecl.isInterface() ? "class" : "interface";
					int start= scanner.getCurrentTokenStartPosition();
					int end= scanner.getCurrentTokenEndPosition() + 1;
					
					addReplace(start, end - start, str, "Invert Type");
				} catch (InvalidInputException e) {
					// ignore
				}
			}
		}
		
		// name
		SimpleName simpleName= typeDecl.getName();
		if (isReplaced(simpleName)) {
			replaceNode(simpleName, getReplacingNode(simpleName));
		}
		
		// superclass
		Name superClass= typeDecl.getSuperclass();
		if ((!typeDecl.isInterface() || invertType) && superClass != null) {
			if (isInserted(superClass)) {
				String str= " extends " + ASTNodes.asString(superClass);
				int pos= simpleName.getStartPosition() + simpleName.getLength();
				addInsert(pos, str, "Insert Supertype");
			} else if (isReplaced(superClass)) {
				ASTNode changed= getReplacingNode(superClass);
				if (changed == null) {
					int startPos= simpleName.getStartPosition() + simpleName.getLength();
					int endPos= superClass.getStartPosition() + superClass.getLength();
					addDelete(startPos, endPos - startPos, "Remove Supertype");
				} else {
					String str= ASTNodes.asString(changed);
					addReplace(superClass.getStartPosition(), superClass.getLength(), str, "Replace Supertype");
				}
			}
		}
		// extended interfaces
		List interfaces= typeDecl.superInterfaces();
		if (hasChanges(interfaces) || invertType) {
			int startPos;
			if (typeDecl.isInterface() || superClass == null || isInserted(superClass)) {
				startPos= simpleName.getStartPosition() + simpleName.getLength();
			} else {
				startPos= superClass.getStartPosition() + superClass.getLength();
			}
			String keyword= (typeDecl.isInterface() != invertType) ? " extends " : " implements ";
			rewriteList(startPos, keyword, interfaces, invertType);
		}
		
		// type members
		List members= typeDecl.bodyDeclarations();
		
		ASTNode last= null;
		// startPos required if first member is an insert: find position after left brace of type
		int startPos= 0;
		int startIndent= 0;
		if (isInsertFirst(members)) { // calculate only if needed
			startIndent= getIndent(typeDecl.getStartPosition()) + 1;
			try {
				int pos= typeDecl.getStartPosition(); //simpleName.getStartPosition() + simpleName.getLength();
				IScanner scanner= getScanner(pos);
				ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameLBRACE);		
				
				startPos= scanner.getCurrentTokenEndPosition() + 1;
			} catch (InvalidInputException e) {
				// ignore
			}
		}
		rewriteParagraphList(members, startPos, startIndent);
		return false;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration methodDecl) {
		boolean willBeConstructor= methodDecl.isConstructor();
		if (isModified(methodDecl)) {
			MethodDeclaration modifiedNode= (MethodDeclaration) getModifiedNode(methodDecl);
			rewriteModifiers(methodDecl.getStartPosition(), methodDecl.getModifiers(), modifiedNode.getModifiers());
			willBeConstructor= modifiedNode.isConstructor();
		}
		
		Type returnType= methodDecl.getReturnType();
		if (isReplaced(returnType)) {
			replaceNode(returnType, getReplacingNode(returnType));
		} else if (isInserted(returnType)) {
			if (!willBeConstructor) {
				try {
					int startPos= methodDecl.getStartPosition();
					int offset= ASTResolving.getPositionAfter(getScanner(startPos), startPos, MODIFIERS);
					insertNode(returnType, offset);
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			}
		}
		
		SimpleName simpleName= methodDecl.getName();
		if (isReplaced(simpleName)) {
			replaceNode(simpleName, getReplacingNode(simpleName));
		}
		
		List parameters= methodDecl.parameters();
		List exceptions= methodDecl.thrownExceptions();
		
		boolean changedParams= hasChanges(parameters);
		boolean changedExc= hasChanges(exceptions);
		
		if (changedParams || changedExc) {
			try {
				int offset= methodDecl.getStartPosition(); // simpleName.getStartPosition() + simpleName.getLength();
				IScanner scanner= getScanner(offset);
				
				if (changedParams) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameLPAREN);
					rewriteList(scanner.getCurrentTokenEndPosition() + 1, "", parameters, false);
				}
				if (changedExc) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameRPAREN);
					rewriteList(scanner.getCurrentTokenEndPosition() + 1, " throws ", exceptions, false);
				}
				
			} catch (InvalidInputException e) {
				// ignore
			}
		}

		Block body= methodDecl.getBody();
		if (body != null) {
			rewriteMethodBody(methodDecl, body);
		}				
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Block)
	 */
	public boolean visit(Block block) {
		List list= block.statements();
		int startPos= block.getStartPosition() + 1; // insert after left brace
		int startIndent= 0;
		if (!list.isEmpty() && isInserted((ASTNode) list.get(0))) { // calculate only when needed
			startIndent= getIndent(block.getStartPosition()) + 1;
		}
		rewriteParagraphList(list, startPos, startIndent);
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		Expression expression= node.getExpression();
		if (expression != null) {
			int offset= node.getStartPosition();
			if (isInserted(expression)) {
				try {
					offset= ASTResolving.getPositionAfter(getScanner(offset), ITerminalSymbols.TokenNamereturn);
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			}
			rewriteNode(expression, offset);
		}
		return false;
	}		
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		List declarations= node.bodyDeclarations();
		int startPos= node.getStartPosition() + 1;
		int startIndent= isInsertFirst(declarations) ? (getIndent(node.getStartPosition()) + 1) : 0;
		rewriteParagraphList(declarations, startPos, startIndent);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		Expression arrayExpr= node.getArray();
		Assert.isTrue(!isInserted(node), "Array expression in ArrayAccess can only be replaced");
		rewriteNode(arrayExpr, 0);
		
		Expression indexExpr= node.getIndex();
		Assert.isTrue(!isInserted(node), "Index expression in ArrayAccess can only be replaced");
		rewriteNode(indexExpr, 0);		
		return false;
	}

	/** (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		ArrayType arrayType= node.getType();
		int nOldBrackets= arrayType.getDimensions(); // number of total brackets
		int nNewBrackets= nOldBrackets;
		if (isReplaced(arrayType)) { // changed arraytype can have different dimension or type name
			ArrayType replacingType= (ArrayType) getReplacingNode(arrayType);
			Type newElementType= replacingType.getElementType();
			Type oldElementType= arrayType.getElementType();
			if (!newElementType.equals(oldElementType)) {
				replaceNode(oldElementType, newElementType);
			}
			nNewBrackets= replacingType.getDimensions();
		}
		List dimExpressions= node.dimensions(); // dimension node with expressions
		try {
			// offset on first opening brace
			int offset= ASTResolving.getPositionBefore(getScanner(arrayType.getStartPosition()), ITerminalSymbols.TokenNameLBRACKET);
			for (int i= 0; i < dimExpressions.size(); i++) {
				ASTNode elem = (ASTNode) dimExpressions.get(i);
				if (isInserted(elem)) { // insert new dimension
					addInsert(offset, "[", "Left bracket");
					addGenerated(offset, 0, elem, 0, false);
					addInsert(offset, "]", "Right bracket");
					nNewBrackets--;
				} else {
					int elemEnd= elem.getStartPosition() + elem.getLength();
					int endPos= ASTResolving.getPositionAfter(getScanner(elemEnd), ITerminalSymbols.TokenNameRBRACKET);
					if (isReplaced(elem)) {
						ASTNode replacing= getReplacingNode(elem);
						if (replacing == null) { // remove includes open and closing brace
							addDelete(offset, endPos - offset, "Remove dimension");
						} else {
							replaceNode(elem, replacing);
							nNewBrackets--;
						}
					} else {
						elem.accept(this);
						nNewBrackets--;
					}
					offset= endPos;
					nOldBrackets--;
				}
			}
			if (nOldBrackets < nNewBrackets) {
				for (int i= nOldBrackets; i < nNewBrackets; i++) {
					addInsert(offset, "[]", "Empty bracket");
				}
			} else if (nOldBrackets > nNewBrackets) {
				IScanner scanner= getScanner(offset);
				for (int i= nNewBrackets; i < nOldBrackets; i++) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameRBRACKET);
				}
				addDelete(offset, scanner.getCurrentTokenEndPosition() + 1 - offset, "Remove brackets");
			}
			
			ArrayInitializer initializer= node.getInitializer();
			if (initializer != null) {
				rewriteNode(initializer, node.getStartPosition() + node.getLength());
			}
		} catch (InvalidInputException e) {
			JavaPlugin.log(e);
		}		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		List expressions= node.expressions();
		rewriteList(node.getStartPosition() + 1, "", expressions, false);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		checkNoModification(node);
		checkNoInsertOrReplace(node.getComponentType());
		// no changes possible // need to check
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		checkNoInsertOrReplace(node.getExpression());
		checkNoInsertOrReplace(node.getMessage());
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		Expression leftHand= node.getLeftHandSide();
		if (isReplaced(leftHand)) {
			replaceNode(leftHand, getReplacingNode(leftHand));
		} else {
			leftHand.accept(this);
		}
		
		if (isModified(node)) {
			try {
				IScanner scanner= getScanner(leftHand.getStartPosition() + leftHand.getLength());
				scanner.getNextToken(); // op
				Assignment.Operator modifiedOp= ((Assignment) getModifiedNode(node)).getOperator();
				addReplace(scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenSource().length, modifiedOp.toString(), "Replace operator");
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		}
			
		Expression rightHand= node.getRightHandSide();
		if (isReplaced(rightHand)) {
			replaceNode(rightHand, getReplacingNode(rightHand));
		} else {
			rightHand.accept(this);
		}				
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		checkNoModification(node); // no modification possible
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		rewriteNode(node.getLabel(), node.getStartPosition() + node.getLength());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		Type type= node.getType();
		if (isReplaced(type)) {
			replaceNode(type, getReplacingNode(type));
		} else {
			type.accept(this);
		}		
		Expression expression= node.getExpression();
		if (isReplaced(expression)) {
			replaceNode(expression, getReplacingNode(expression));
		} else {
			expression.accept(this);
		}				
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		ASTNode exception= node.getException();
		if (isReplaced(exception)) {
			replaceNode(exception, getReplacingNode(exception));
		}		
		Block body= node.getBody();
		if (isReplaced(body)) {
			replaceParagraph(body, getReplacingNode(body));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		checkNoModification(node); // no modification possible
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		Expression expression= node.getExpression();
		if (expression != null) {
			rewriteOptionalQualifier(expression, node.getStartPosition());
		}
		
		Name name= node.getName();
		if (isReplaced(name)) {
			replaceNode(name, getReplacingNode(name));
		}
		
		List arguments= node.arguments();
		if (hasChanges(arguments)) {
			try {
				int startpos= ASTResolving.getPositionAfter(getScanner(name.getStartPosition() + name.getLength()), ITerminalSymbols.TokenNameLPAREN);
				rewriteList(startpos, "", arguments, false);
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments);
		}
		
		AnonymousClassDeclaration decl= node.getAnonymousClassDeclaration();
		if (decl != null) {
			rewriteNode(decl, node.getStartPosition() + node.getLength());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		Expression expression= node.getExpression();
		if (isReplaced(expression)) {
			replaceNode(expression, getReplacingNode(expression));
		} else {
			expression.accept(this);
		}
		
		Expression thenExpression= node.getThenExpression();
		if (isReplaced(thenExpression)) {
			replaceNode(thenExpression, getReplacingNode(thenExpression));
		} else {
			thenExpression.accept(this);
		}
		
		Expression elseExpression= node.getElseExpression();
		if (isReplaced(elseExpression)) {
			replaceNode(elseExpression, getReplacingNode(elseExpression));
		} else {
			elseExpression.accept(this);
		}		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		List arguments= node.arguments();
		if (hasChanges(arguments)) {
			try {
				int startpos= ASTResolving.getPositionAfter(getScanner(node.getStartPosition()), ITerminalSymbols.TokenNameLPAREN);
				rewriteList(startpos, "", arguments, false);
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		rewriteNode(node.getLabel(), node.getStartPosition() + node.getLength());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		Statement body= node.getBody();
		if (isReplaced(body)) {
			replaceParagraph(body, getReplacingNode(body));
		}
		ASTNode expression= node.getExpression();
		if (isReplaced(expression)) {
			replaceNode(expression, getReplacingNode(expression));
		}		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(Javadoc node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		return super.visit(node);
	}

}
