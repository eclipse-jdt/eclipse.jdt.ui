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
import org.eclipse.jdt.internal.corext.refactoring.changes.enhanced.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.CopySourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.TextRegion;
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

	private static final String CHANGEKEY= "ASTChangeData";
	private static final String COPYSOURCEKEY= "ASTCopySource";
	
	private static final String EDITNODE= "EditNode";

	public static ASTNode createCopyTarget(ASTNode node) {
		Assert.isTrue(node.getProperty(COPYSOURCEKEY) == null, "Node used as more than one copy source");
		CopySourceEdit edit= new CopySourceEdit(node.getStartPosition(), node.getLength());
		node.setProperty(COPYSOURCEKEY, edit);
		return ASTWithExistingFlattener.getPlaceholder(node);
	}

	public static void markAsInserted(ASTNode node) {
		Assert.isTrue(node.getStartPosition() == -1, "Tries to mark existing node as inserted");
		node.setSourceRange(-1, 0);
	}
	
	public static void markAsRemoved(ASTNode node) {
		markAsReplaced(node, null);
	}
	
	public static void markAsReplaced(ASTNode node, ASTNode replacingNode) {
		ASTReplace replace= new ASTReplace();
		replace.replacingNode= replacingNode;
		node.setProperty(CHANGEKEY, replace);
	}
	
	public static void markAsModified(ASTNode node, ASTNode modifiedNode) {
		Assert.isTrue(node.getClass().equals(modifiedNode.getClass()), "Tries to modify with a node of different type");
		ASTModify modify= new ASTModify();
		modify.modifiedNode= modifiedNode;
		node.setProperty(CHANGEKEY, modify);
	}
	
	private static void setEdit(ASTNode node, TextEdit edit) {
		Assert.isTrue(getEdit(node) == null, "Two edits added to teh same node");
		node.setProperty(EDITNODE, edit);
	}
	
	private static TextEdit getEdit(ASTNode node) {
		return (TextEdit) node.getProperty(EDITNODE);
	}
	
	/* package */ static boolean isInsertOrReplace(ASTNode node) {
		return isInserted(node) || isReplaced(node);
	}
	
	/* package */ static boolean isInsertOrRemove(ASTNode node) {
		return isInserted(node) || isRemoved(node);
	}
	
	/* package */ static boolean isInserted(ASTNode node) {
		return node.getStartPosition() == -1;
	}
	
	/* package */ static boolean isReplaced(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTReplace;
	}
	
	/* package */ static boolean isRemoved(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		return info instanceof ASTReplace && (((ASTReplace) info).replacingNode == null);
	}	
	
	/* package */ static boolean isModified(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTModify;
	}	
	
	/* package */ static ASTNode getModifiedNode(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		if (info instanceof ASTModify) {
			return ((ASTModify) info).modifiedNode;
		}
		return null;
	}

	/* package */ static ASTNode getReplacingNode(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		if (info instanceof ASTReplace) {
			return ((ASTReplace) info).replacingNode;
		}
		return null;
	}
	
	/* package */ static CopySourceEdit getCopySourceEdit(ASTNode node) {
		return (CopySourceEdit) node.getProperty(COPYSOURCEKEY);
	}
	

	private static final class ASTCopySource {
		public CopySourceEdit copySource;
	}	
		
	private static final class ASTReplace {
		public ASTNode replacingNode;
	}
	
	private static final class ASTModify {
		public ASTNode modifiedNode;
	}
	
	private TextBuffer fTextBuffer;
	private CompilationUnitChange fChange;
	
	private TextEdit fCurrentEdit;
	
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
		
		fCurrentEdit= new MultiTextEdit();
		fChange.addTextEdit("Root", fCurrentEdit);
	}
	
	public void addInsert(int offset, String insertString, String description) {
		fCurrentEdit.add(SimpleTextEdit.createInsert(offset, insertString));
	}
	
	public void addDelete(int offset, int len, String description) {
		TextEdit edit= SimpleTextEdit.createDelete(offset, len);
		fCurrentEdit.add(edit);
	}
	
	public void addDeleteAndVisit(int offset, int len, String description, ASTNode node) {
		TextEdit parentEdit= fCurrentEdit;
		fCurrentEdit= SimpleTextEdit.createDelete(offset, len);
		parentEdit.add(fCurrentEdit);
		node.accept(this);
		fCurrentEdit= parentEdit;
	}	
	
	
	public void addReplace(int offset, int len, String insertString, String description) {
		TextEdit edit= SimpleTextEdit.createReplace(offset, len, insertString);
		fCurrentEdit.add(edit);
	}
		
	public void addCopy(ASTNode copiedNode, int destOffset, int sourceIndentLevel, String destIndentString, int tabWidth, String description) {
		CopySourceEdit sourceEdit= getCopySourceEdit(copiedNode);
		Assert.isTrue(sourceEdit != null, "Copy source not annotated" + copiedNode.toString());
		
		CopyIndentedTargetEdit targetEdit= new CopyIndentedTargetEdit(destOffset, sourceIndentLevel, destIndentString, tabWidth);
		targetEdit.setSourceEdit(sourceEdit);
		
		fCurrentEdit.add(targetEdit);
	}
		
	private IScanner getScanner(int pos) {
		if (fScanner == null) {
			fScanner= ToolFactory.createScanner(false, false, false, false);
			String content= fTextBuffer.getContent();
			char[] chars= new char[content.length()];
			content.getChars(0, chars.length, chars, 0);
			fScanner.setSource(chars);
		}
		fScanner.resetTo(pos, fTextBuffer.getLength());
		return fScanner;
	}
	
	private void checkNoModification(ASTNode node) {
		Assert.isTrue(!isModified(node), "Can not modify " + node.getClass().getName());
	}
	
	private void checkNoInsertOrReplace(ASTNode node) {
		Assert.isTrue(!isInsertOrReplace(node), "Can not insert or replace node in " + node.getParent().getClass().getName());
	}
	
	private void checkNoInsert(ASTNode node) {
		Assert.isTrue(!isInserted(node), "Can not insert node in " + node.getParent().getClass().getName());
	}		
	
	
	private int rewriteRequiredNode(ASTNode node) {
		// disabled (bug 22161) checkNoInsert(node);
		if (isReplaced(node)) {
			int offset= node.getStartPosition();
			addDeleteAndVisit(offset, node.getLength(), "Delete existing", node);
			addGenerated(offset, getReplacingNode(node), getIndent(offset), true);
		} else {
			node.accept(this);
		}
		return node.getStartPosition() + node.getLength();
	}
	
	private int rewriteNode(ASTNode node, int offset, String prefix) {
		if (isInserted(node)) {
			addInsert(offset, prefix, "Insert prefix");
			addGenerated(offset, node, getIndent(offset), true);
			return offset;
		} else if (isReplaced(node)) {
			ASTNode replacing= getReplacingNode(node);
			if (replacing == null) {
				// if there is a prefix, delete the prefix as well
				int len= node.getStartPosition() + node.getLength() - offset;
				addDeleteAndVisit(offset, len, "Remove Node", node);
			} else {
				addDeleteAndVisit(node.getStartPosition(), node.getLength(), "Delete existing", node);
				addGenerated(node.getStartPosition(), replacing, getIndent(offset), true);
			}
		} else {
			node.accept(this);
		}
		return node.getStartPosition() + node.getLength();
	}		

	
	private int getPosBeforeSpaces(int pos) {
		while (pos > 0 && Strings.isIndentChar(fTextBuffer.getChar(pos - 1))) {
			pos--;
		}
		return pos;
	}
	
	private int rewriteOptionalQualifier(ASTNode node, int startPos) {
		if (isInserted(node)) {
			addGenerated(startPos, node, getIndent(startPos), true);
			addInsert(startPos, ".", "Dot");
			return startPos;
		} else {
			if (isReplaced(node)) {
				ASTNode replacingNode= getReplacingNode(node);
				if (replacingNode == null) {
					try {
						int dotStart= node.getStartPosition() + node.getLength();
						int dotEnd= ASTResolving.getPositionAfter(getScanner(dotStart), ITerminalSymbols.TokenNameDOT);
						addDeleteAndVisit(startPos, dotEnd - startPos, "remove node and dot", node);
					} catch (InvalidInputException e) {
						JavaPlugin.log(e);
					}
				} else {
					addDeleteAndVisit(startPos, node.getLength(), "Remove Node", node);
					addGenerated(startPos, replacingNode, getIndent(startPos), true);
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
			last= rewriteParagraph(elem, last, insertPos, insertIndent, false, true);
		}
		if (last != null) {
			return last.getStartPosition() + last.getLength();
		}
		return insertPos;
	}
	
	private int rewriteRequiredParagraph(ASTNode elem) {
		if (isReplaced(elem)) {
			replaceParagraph(elem, getReplacingNode(elem));
		} else {
			elem.accept(this);
		}
		return elem.getStartPosition() + elem.getLength();
	}	
	
	

	/**
	 * Rewrite a paragraph (node that is on a new line and has same indent as previous
	 */
	private ASTNode rewriteParagraph(ASTNode elem, ASTNode sibling, int insertPos, int insertIndent, boolean additionalNewLine, boolean useIndentOfSibling) {
		if (elem == null) {
			return sibling;
		} else if (isInserted(elem)) {
			insertParagraph(elem, sibling, insertPos, insertIndent, additionalNewLine, useIndentOfSibling);
			return sibling;
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
					addGenerated(startPos, body, getIndent(methodDecl.getStartPosition()), true);
				} else if (isReplaced(body)) {
					ASTNode changed= getReplacingNode(body);
					if (changed == null) {
						addDeleteAndVisit(body.getStartPosition(), body.getLength(), "Remove body", body);
						addInsert(body.getStartPosition(), ";", "Insert semicolon");
					} else {
						addDeleteAndVisit(body.getStartPosition(), body.getLength(), "Remove Node", body);
						addGenerated(body.getStartPosition(), changed, getIndent(body.getStartPosition()), true);
					}
				}
			} catch (InvalidInputException e) {
				// ignore
			}	
		} else {
			body.accept(this);
		}
	}
	
	private int getNextExistingStartPos(List list, int listStartIndex, int defaultOffset) {
		for (int i= listStartIndex; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (!isInserted(elem)) {
				return elem.getStartPosition();
			}
		}
		return defaultOffset;
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
	
	private int visitList(List list, int defaultPos) {
		ASTNode curr= null;
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			curr= ((ASTNode) iter.next());
			curr.accept(this);
		}
		if (curr == null) {
			return defaultPos;
		}
		return curr.getStartPosition() + curr.getLength();
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
			if (before != 0) { // all deleted
				currPos= startPos;
				updateKeyword= false; // delete keyword by extending range of the first node
			} else {
				return startPos; // before == 0 && after == 0
			}
		}
		
		if (before == 0) { // creating a new list -> insert keyword first (e.g. " throws ")
			addInsert(startPos, keyword, "keyword");
		} else if (updateKeyword) {
			int firstStart= getNextExistingStartPos(list, 0, startPos);
			addReplace(startPos, firstStart - startPos, keyword, "Update keyword");
		}
		int initIndent= getIndent(startPos);
		
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isInserted(elem)) {
				after--;
				addGenerated(currPos, elem, initIndent, true);
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
						// delete including the comma
						addDeleteAndVisit(currPos, nextStart - currPos, "Remove", elem);
					} else {
						after--;
						if (after == 0) { // will be last node -> remove comma
							addDeleteAndVisit(currPos, nextStart - currPos, "Remove", elem);
							addGenerated(currPos, changed, initIndent, true);
						} else if (before == 0) { // was last, but not anymore -> add comma
							addDeleteAndVisit(currPos, currEnd - currPos, "Remove", elem);
							addGenerated(currPos, changed, initIndent, true);
							addInsert(currPos, ", ", "Add comma");
						} else {
							addDeleteAndVisit(currPos, currEnd - currPos, "Remove", elem);
							addGenerated(currPos, changed, initIndent, true);
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
			
			addDeleteAndVisit(start, end - start, "Remove", elem);
		} else {
			int startLine= fTextBuffer.getLineOfOffset(elem.getStartPosition());
			int indent= fTextBuffer.getLineIndent(startLine, CodeFormatterUtil.getTabWidth());
			
			addDeleteAndVisit(elem.getStartPosition(), elem.getLength(), "Remove", elem);
			addGenerated(elem.getStartPosition(), changed, indent, true);
		}
	}

	private void insertParagraph(ASTNode elem, ASTNode sibling, int insertPos, int indent, boolean additionalNewLine, boolean useIndentOfSibling) {
		if (sibling != null) {
			if (useIndentOfSibling) {
				indent= getIndent(sibling.getStartPosition());
			}
			insertPos= sibling.getStartPosition() + sibling.getLength();
		}
		addInsert(insertPos, fTextBuffer.getLineDelimiter(), "Add new line");
		addGenerated(insertPos, elem, indent, false);
		if (additionalNewLine) {
			addInsert(insertPos, fTextBuffer.getLineDelimiter(), "Additional new line");
		}
	}

	private int getIndent(int pos) {
		int line= fTextBuffer.getLineOfOffset(pos);
		return fTextBuffer.getLineIndent(line, CodeFormatterUtil.getTabWidth());
	}

	private void addGenerated(int insertOffset, ASTNode node, int initialIndentLevel, boolean removeLeadingIndent) {		
		Assert.isTrue(node.getStartPosition() == -1, "Node to generate must be new nodes");
		
		ASTWithExistingFlattener flattener= new ASTWithExistingFlattener();
		node.accept(flattener);
		String formatted= flattener.getFormattedResult(initialIndentLevel, fTextBuffer.getLineDelimiter());
		
		ASTWithExistingFlattener.ExistingNodeMarker[] markers= flattener.getExistingNodeMarkers();
		
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
			String destIndentString=  Strings.getIndentString(getCurrentLine(formatted, offset), tabWidth);
			
			int srcIndentLevel= getIndent(movedNode.getStartPosition());
			addCopy(movedNode, insertOffset, srcIndentLevel, destIndentString, tabWidth, "Move");
		
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
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#postVisit(ASTNode)
	 */
	public void postVisit(ASTNode node) {
		TextEdit edit= getCopySourceEdit(node);
		if (edit != null) {
			fCurrentEdit= fCurrentEdit.getParent();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#preVisit(ASTNode)
	 */
	public void preVisit(ASTNode node) {
		TextEdit edit= getCopySourceEdit(node);
		if (edit != null) {
			fCurrentEdit.add(edit);
			fCurrentEdit= edit;
		}
	}	


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(CompilationUnit)
	 */ 
	public boolean visit(CompilationUnit node) {
		PackageDeclaration packageDeclaration= node.getPackage();
		ASTNode last= rewriteParagraph(packageDeclaration, null, 0, 0, true, false);
				
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
					
					addReplace(start, end - start, str, "Invert class keyword");
				} catch (InvalidInputException e) {
					// ignore
				}
			}
		}
		
		// name
		int pos= rewriteRequiredNode(typeDecl.getName());
		
		// superclass
		Name superClass= typeDecl.getSuperclass();
		if ((!typeDecl.isInterface() || invertType) && superClass != null) {
			if (isInserted(superClass)) {
				String str= " extends " + ASTNodes.asString(superClass);
				addInsert(pos, str, "Insert Supertype");
			} else {
				if (isReplaced(superClass)) {
					ASTNode changed= getReplacingNode(superClass);
					if (changed == null) {
						int endPos= superClass.getStartPosition() + superClass.getLength();
						addDeleteAndVisit(pos, endPos - pos, "Remove Supertype and extends", superClass);
					} else {
						addDeleteAndVisit(superClass.getStartPosition(), superClass.getLength(), "Remove Supertype", superClass);
						addGenerated(superClass.getStartPosition(), changed, 0, false);
					}
				} else {
					superClass.accept(this);
				}
				pos= superClass.getStartPosition() + superClass.getLength();
			}
		}
		// extended interfaces
		List interfaces= typeDecl.superInterfaces();
		if (hasChanges(interfaces) || invertType) {
			String keyword= (typeDecl.isInterface() != invertType) ? " extends " : " implements ";
			rewriteList(pos, keyword, interfaces, invertType);
		} else {
			visitList(interfaces, pos);
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
				int offset= typeDecl.getStartPosition(); //simpleName.getStartPosition() + simpleName.getLength();
				IScanner scanner= getScanner(offset);
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
		try {
			int startPos= methodDecl.getStartPosition();
			if (isInsertOrRemove(returnType)) {
				startPos= ASTResolving.getPositionAfter(getScanner(startPos), startPos, MODIFIERS);
			}
			if (!willBeConstructor || !isInserted(returnType)) {
				rewriteNode(returnType, startPos, " ");
			}
		} catch (InvalidInputException e) {
			JavaPlugin.log(e);
		}
	
		int pos= rewriteRequiredNode(methodDecl.getName());
		try {
			pos= methodDecl.getStartPosition(); // workaround for bug 22161
			
			List parameters= methodDecl.parameters();
			if (hasChanges(parameters)) {
				pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLPAREN);
				rewriteList(pos, "", parameters, false);
			} else {
				visitList(parameters, pos);
			}
			
			List exceptions= methodDecl.thrownExceptions();
			if (hasChanges(exceptions)) {
				pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameRPAREN);
				rewriteList(pos, " throws ", exceptions, false);
			} else {
				visitList(exceptions, pos);
			}
		} catch (InvalidInputException e) {
			// ignore
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
			if (isInsertOrReplace(expression)) {
				try {
					int offset= ASTResolving.getPositionAfter(getScanner(node.getStartPosition()), ITerminalSymbols.TokenNamereturn);
					rewriteNode(expression, offset, " ");
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			} else {
				expression.accept(this);
			}
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
		rewriteRequiredNode(node.getArray());
		rewriteRequiredNode(node.getIndex());
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
			Assert.isTrue(replacingType != null, "Cant remove array type in ArrayCreation");
			Type newType= replacingType.getElementType();
			Type oldType= arrayType.getElementType();
			if (!newType.equals(oldType)) {
				addDeleteAndVisit(oldType.getStartPosition(), oldType.getLength(), "Remove old", oldType);
				addGenerated(oldType.getStartPosition(), newType, 0, false);
			} else {
				oldType.accept(this);
			}
			nNewBrackets= replacingType.getDimensions();
		} else {
			arrayType.accept(this);
		}
		List dimExpressions= node.dimensions(); // dimension node with expressions
		try {
			// offset on first opening brace
			int offset= ASTResolving.getPositionBefore(getScanner(arrayType.getStartPosition()), ITerminalSymbols.TokenNameLBRACKET);
			for (int i= 0; i < dimExpressions.size(); i++) {
				ASTNode elem = (ASTNode) dimExpressions.get(i);
				if (isInserted(elem)) { // insert new dimension
					addInsert(offset, "[", "Left bracket");
					addGenerated(offset, elem, 0, false);
					addInsert(offset, "]", "Right bracket");
					nNewBrackets--;
				} else {
					int elemEnd= elem.getStartPosition() + elem.getLength();
					int endPos= ASTResolving.getPositionAfter(getScanner(elemEnd), ITerminalSymbols.TokenNameRBRACKET);
					if (isReplaced(elem)) {
						ASTNode replacing= getReplacingNode(elem);
						if (replacing == null) { // remove includes open and closing brace
							addDeleteAndVisit(offset, endPos - offset, "Remove dimension", elem);
						} else {
							addDeleteAndVisit(elem.getStartPosition(), elem.getLength(), "Remove old", elem);
							addGenerated(elem.getStartPosition(), replacing, 0, false);
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
				int pos= node.getStartPosition() + node.getLength(); // insert pos
				if (isRemoved(initializer)) {
					pos= getPosBeforeSpaces(initializer.getStartPosition()); // remove pos
				}
				rewriteNode(initializer, pos, " ");
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
		int pos= rewriteRequiredNode(leftHand);
		
		if (isModified(node)) {
			try {
				IScanner scanner= getScanner(pos);
				scanner.getNextToken(); // op
				Assignment.Operator modifiedOp= ((Assignment) getModifiedNode(node)).getOperator();
				addReplace(scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenSource().length, modifiedOp.toString(), "Replace operator");
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		}
			
		Expression rightHand= node.getRightHandSide();
		rewriteRequiredNode(rightHand);
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
		ASTNode label= node.getLabel();
		if (label != null) {
			if (isInsertOrReplace(label)) {
				try {
					int offset= ASTResolving.getPositionAfter(getScanner(node.getStartPosition()), ITerminalSymbols.TokenNamebreak);
					rewriteNode(label, offset, " "); // space between break and label
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			} else {
				label.accept(this);
			}
		}
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		rewriteRequiredNode(node.getType());
		rewriteRequiredNode(node.getExpression());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) { // catch (Exception) Block
		rewriteRequiredNode(node.getException());
		rewriteRequiredParagraph(node.getBody());
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
		int pos= rewriteRequiredNode(name);

		List arguments= node.arguments();
		if (hasChanges(arguments)) {
			try {
				int startpos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLPAREN);
				rewriteList(startpos, "", arguments, false);
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments, 0);
		}
		
		AnonymousClassDeclaration decl= node.getAnonymousClassDeclaration();
		if (decl != null) {
			pos= node.getStartPosition() + node.getLength(); // for insert
			if (isRemoved(decl)) {
				pos= getPosBeforeSpaces(decl.getStartPosition()); // for remove
			}
			rewriteNode(decl, pos, " ");
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) { // expression ? thenExpression : elseExpression
		rewriteRequiredNode(node.getExpression());
		rewriteRequiredNode(node.getThenExpression());
		rewriteRequiredNode(node.getElseExpression());	
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
			visitList(arguments, 0);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		ASTNode label= node.getLabel();
		if (label != null) {
			if (isInsertOrReplace(label)) {
				try {
					int offset= ASTResolving.getPositionAfter(getScanner(node.getStartPosition()), ITerminalSymbols.TokenNamecontinue);
					rewriteNode(label, offset, " "); // space between continue and label
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			} else {
				label.accept(this);
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) { // do statement while expression
		rewriteRequiredNode(node.getBody());	
		rewriteRequiredNode(node.getExpression());	
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		checkNoModification(node); // no modification possible
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) { // expression
		rewriteRequiredNode(node.getExpression());	
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) { // expression.name
		rewriteRequiredNode(node.getExpression()); // expression
		rewriteRequiredNode(node.getName()); // name
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) { //{ Modifier } Type VariableDeclarationFragment { ',' VariableDeclarationFragment } ';'
		if (isModified(node)) {
			FieldDeclaration modifedNode= (FieldDeclaration) getModifiedNode(node);
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers());
		}
		
		Type type= node.getType();
		rewriteRequiredNode(type);
		
		List fragments= node.fragments();
		int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
		rewriteList(startPos, "", fragments, false);

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		try {
			int pos= node.getStartPosition();
			
			List initializers= node.initializers();
			if (hasChanges(initializers)) { // position after opening parent
				int startOffset= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLPAREN);
				pos= rewriteList(startOffset, "", initializers, false);
			} else {
				pos= visitList(initializers, pos);
			}
			
			// position after first semicolon
			pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameSEMICOLON);
			
			Expression expression= node.getExpression();
			if (expression != null) {
				rewriteNode(expression, pos, "");
			}
			
			List updaters= node.updaters();
			if (hasChanges(updaters)) {
				int startOffset= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameSEMICOLON);
				rewriteList(startOffset, "", updaters, false);
			} else {
				visitList(updaters, 0);
			}
		} catch (InvalidInputException e) {
			JavaPlugin.log(e);
		}
			
		rewriteRequiredNode(node.getBody()); // body
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		rewriteRequiredNode(node.getExpression()); // statement
		
		Statement thenStatement= node.getThenStatement();
		rewriteRequiredNode(thenStatement); // then statement

		Statement elseStatement= node.getElseStatement();
		if (elseStatement != null) {
			int startPos= thenStatement.getStartPosition() + thenStatement.getLength();
			rewriteNode(elseStatement, startPos, " else ");
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		Name name= node.getName();
		rewriteRequiredNode(name); // statement
		
		if (isModified(node)) {
			ImportDeclaration modifiedNode= (ImportDeclaration) getModifiedNode(node);
			if (!node.isOnDemand() && modifiedNode.isOnDemand()) {
				addInsert(name.getStartPosition() + name.getLength(), ".*", "Add on demand");
			} else if (node.isOnDemand() && !modifiedNode.isOnDemand()) {
				try {
					int startPos= name.getStartPosition() + name.getLength();
					int endPos= ASTResolving.getPositionBefore(getScanner(startPos), ITerminalSymbols.TokenNameSEMICOLON);
					addDelete(startPos, endPos - startPos, "Remove on demand");
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return false;
	}
	
	private void replaceOperation(int posBeforeOperation, String newOperation) {
		try {
			IScanner scanner= getScanner(posBeforeOperation);
			scanner.getNextToken(); // op			
			addReplace(scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenSource().length, newOperation, "Replace operator");
		} catch (InvalidInputException e) {
			JavaPlugin.log(e);
		}
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		Expression leftHand= node.getLeftOperand();
		int pos= rewriteRequiredNode(leftHand);
		
		String operation= node.getOperator().toString();
		boolean needsNewOperation= false;
		if (isModified(node)) {
			InfixExpression.Operator modifiedOp= ((InfixExpression) getModifiedNode(node)).getOperator();
			String newOperation= modifiedOp.toString();
			if (!newOperation.equals(operation)) {
				operation= newOperation;
				needsNewOperation= true;
				replaceOperation(pos, operation);
			}
		}
			
		Expression rightHand= node.getRightOperand();
		pos= rewriteRequiredNode(rightHand);
		
		List extendedOperands= node.extendedOperands();
		if (needsNewOperation || hasChanges(extendedOperands)) {
			String prefixString= ' ' + operation + ' ';
			for (int i= 0; i < extendedOperands.size(); i++) {
				ASTNode elem= (ASTNode) extendedOperands.get(i);
				if (needsNewOperation && !isRemoved(elem) && !isInserted(elem)) {
					replaceOperation(pos, operation);
				}
				pos= rewriteNode(elem, pos, prefixString);
			}
		} else {
			visitList(extendedOperands, 0);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		if (isModified(node)) {
			Initializer modifedNode= (Initializer) getModifiedNode(node);
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers());
		}
		rewriteRequiredNode(node.getBody());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		rewriteRequiredNode(node.getLeftOperand());
		rewriteRequiredNode(node.getRightOperand());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(Javadoc node) {
		checkNoInsertOrReplace(node);
		checkNoModification(node);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		rewriteRequiredNode(node.getLabel());
		rewriteRequiredNode(node.getBody());		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		Expression optExpression= node.getExpression();
		if (optExpression != null) {
			rewriteOptionalQualifier(optExpression, node.getStartPosition());
		}

		int pos= rewriteRequiredNode(node.getName());
		List arguments= node.arguments();
		
		if (hasChanges(arguments)) { // eval position after opening parent
			try {
				int startOffset= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLPAREN);
				rewriteList(startOffset, "", arguments, false);
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments, pos);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		checkNoModification(node);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		checkNoModification(node);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		rewriteRequiredNode(node.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		rewriteRequiredNode(node.getExpression());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		int pos= rewriteRequiredNode(node.getOperand());

		if (isModified(node)) {
			PostfixExpression.Operator modifiedOp= ((PostfixExpression) getModifiedNode(node)).getOperator();
			String newOperation= modifiedOp.toString();
			String oldOperation= node.getOperator().toString();
			if (!newOperation.equals(oldOperation)) {
				replaceOperation(pos, newOperation);
			}
		}
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		if (isModified(node)) {
			PrefixExpression.Operator modifiedOp= ((PrefixExpression) getModifiedNode(node)).getOperator();
			String newOperation= modifiedOp.toString();
			String oldOperation= node.getOperator().toString();
			if (!newOperation.equals(oldOperation)) {
				replaceOperation(node.getStartPosition(), newOperation);
			}
		}
		rewriteRequiredNode(node.getOperand());
		return false;	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		checkNoModification(node);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		checkNoModification(node);
		checkNoInsertOrReplace(node.getName());
		checkNoInsertOrReplace(node.getQualifier());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		checkNoModification(node);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		checkNoModification(node);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		if (isModified(node)) {
			SingleVariableDeclaration modifedNode= (SingleVariableDeclaration) getModifiedNode(node);
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers());
		}
		rewriteRequiredNode(node.getType());
		int pos= rewriteRequiredNode(node.getName());
		
		Expression initializer= node.getInitializer();
		if (initializer != null) {
			rewriteNode(node, pos, " = ");
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		checkNoModification(node);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		int pos= node.getStartPosition();
		Expression optExpression= node.getExpression();
		if (optExpression != null) {
			pos= rewriteOptionalQualifier(optExpression, pos);
		}

		List arguments= node.arguments();
		if (hasChanges(arguments)) { // eval position after opening parent
			try {
				pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLPAREN);
				rewriteList(pos, "", arguments, false);
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments, pos);
		}
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		int pos= node.getStartPosition();
		Expression optExpression= node.getQualifier();
		if (optExpression != null) {
			pos= rewriteOptionalQualifier(optExpression, pos);
		}
		
		rewriteRequiredNode(node.getName());
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		int pos= node.getStartPosition();
		Expression optExpression= node.getQualifier();
		if (optExpression != null) {
			pos= rewriteOptionalQualifier(optExpression, pos);
		}
		
		rewriteRequiredNode(node.getName());
		List arguments= node.arguments();
		if (hasChanges(arguments)) { // eval position after opening parent
			try {
				pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLPAREN);
				rewriteList(pos, "", arguments, false);
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments, pos);
		}		
		return false;	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		// dont allow switching from case to default or back. New statements should be created.
		Expression expression= node.getExpression();
		if (expression != null) {
			rewriteRequiredNode(expression);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		int pos= rewriteRequiredNode(node.getExpression());
		
		List statements= node.statements();
		if (hasChanges(statements)) {
			try {
				pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLBRACE);
				
				int insertIndent= getIndent(node.getStartPosition()) + 1;
				ASTNode last= null; 
				for (int i= 0; i < statements.size(); i++) {
					ASTNode elem = (ASTNode) statements.get(i);
					// non-case statements are indented with one extra indent
					int currIndent= elem.getNodeType() == ASTNode.SWITCH_CASE ? insertIndent : insertIndent + 1;
					last= rewriteParagraph(elem, last, pos, currIndent, false, false);
				}
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(statements, pos);
		}
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		rewriteRequiredNode(node.getExpression());
		rewriteRequiredNode(node.getBody());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		Expression optExpression= node.getQualifier();
		if (optExpression != null) {
			rewriteOptionalQualifier(optExpression, node.getStartPosition());
		}
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		rewriteRequiredNode(node.getExpression());		
		return false;	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		int pos= rewriteRequiredNode(node.getBody());
		
		List catchBlocks= node.catchClauses();
		pos= rewriteList(pos, " ", catchBlocks, false);
		
		Block finallyBlock= node.getFinally();
		if (finallyBlock != null) {
			rewriteNode(finallyBlock, pos, " finally ");
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		rewriteRequiredNode(node.getTypeDeclaration());	
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		rewriteRequiredNode(node.getType());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		// same code as FieldDeclaration
		if (isModified(node)) {
			VariableDeclarationExpression modifedNode= (VariableDeclarationExpression) getModifiedNode(node);
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers());
		}
		
		Type type= node.getType();
		rewriteRequiredNode(type);
		
		List fragments= node.fragments();
		int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
		rewriteList(startPos, "", fragments, false);

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		int pos= rewriteRequiredNode(node.getName());
		
		if (isModified(node)) {
			VariableDeclarationFragment modifedNode= (VariableDeclarationFragment) getModifiedNode(node);
			int oldDim= node.getExtraDimensions();
			int newDim= modifedNode.getExtraDimensions();
			if (oldDim < newDim) {
				for (int i= oldDim; i < newDim; i++) {
					addInsert(pos, "[]", "Empty bracket");
				}
			} else if (newDim < oldDim) {
				try {
					IScanner scanner= getScanner(pos);
					for (int i= newDim; i < oldDim; i++) {
						ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameRBRACKET);
					}
					addDelete(pos, scanner.getCurrentTokenEndPosition() + 1 - pos, "Remove brackets");
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			}
		}
		Expression initializer= node.getInitializer();
		if (initializer != null) {
			if (isRemoved(initializer)) {
				int dim= node.getExtraDimensions();
				if (dim > 0) {
					try {
						IScanner scanner= getScanner(pos);
						for (int i= 0; i < dim; i++) {
							pos= ASTResolving.getPositionAfter(scanner, ITerminalSymbols.TokenNameRBRACKET);
						}
					} catch (InvalidInputException e) {
						JavaPlugin.log(e);
					}
				}
			} else {
				pos= node.getStartPosition() + node.getLength(); // insert pos
			}
			rewriteNode(initializer, pos, " = ");
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		// same code as FieldDeclaration		
		if (isModified(node)) {
			VariableDeclarationStatement modifedNode= (VariableDeclarationStatement) getModifiedNode(node);
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers());
		}
		
		Type type= node.getType();
		rewriteRequiredNode(type);
		
		List fragments= node.fragments();
		int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
		rewriteList(startPos, "", fragments, false);

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		rewriteRequiredNode(node.getExpression());
		rewriteRequiredNode(node.getBody()); // body
		return false;
	}



}
