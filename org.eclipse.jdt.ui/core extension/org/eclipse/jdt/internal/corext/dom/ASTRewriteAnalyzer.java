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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.CopySourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
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

	private TextBuffer fTextBuffer;
	private TextEdit fCurrentEdit;
	
	private IScanner fScanner; // shared scanner
	private ASTRewrite fRewrite;
	
	private HashMap fGroupDescriptions;
	
	private final int[] MODIFIERS= new int[] { ITerminalSymbols.TokenNamepublic, ITerminalSymbols.TokenNameprotected, ITerminalSymbols.TokenNameprivate,
		ITerminalSymbols.TokenNamestatic, ITerminalSymbols.TokenNamefinal, ITerminalSymbols.TokenNameabstract, ITerminalSymbols.TokenNamenative,
		ITerminalSymbols.TokenNamevolatile, ITerminalSymbols.TokenNamestrictfp, ITerminalSymbols.TokenNametransient, ITerminalSymbols.TokenNamesynchronized };

	/**
	 * Constructor for ASTChangeAnalyzer.
	 */
	public ASTRewriteAnalyzer(TextBuffer textBuffer, TextEdit rootEdit, ASTRewrite rewrite, HashMap resGroupDescriptions) {
		fTextBuffer= textBuffer;
		fScanner= null;
		fRewrite= rewrite;
		fCurrentEdit= rootEdit;
		fGroupDescriptions= resGroupDescriptions;
	}
	
	public static Object createSourceCopy(ASTNode node) {
		return new CopySourceEdit(node.getStartPosition(), node.getLength());
	}
	
	private boolean isChanged(ASTNode node) {
		return isInserted(node) || isRemoved(node) || isReplaced(node);
	}
	
	private boolean isInsertOrRemove(ASTNode node) {
		return isInserted(node) || isRemoved(node);
	}
	
	private boolean isInserted(ASTNode node) {
		return fRewrite.isInserted(node);
	}
	
	private boolean isReplaced(ASTNode node) {
		return fRewrite.isReplaced(node);
	}
	
	private boolean isRemoved(ASTNode node) {
		return fRewrite.isRemoved(node);
	}	
	
	private boolean isModified(ASTNode node) {
		return fRewrite.isModified(node);
	}
	
	private ASTNode getModifiedNode(ASTNode node) {
		return fRewrite.getModifiedNode(node);
	}

	private ASTNode getReplacingNode(ASTNode node) {
		return fRewrite.getReplacingNode(node);
	}
	
	private final String getDescription(ASTNode node) {
		if (fGroupDescriptions != null) {
			Assert.isTrue(isChanged(node) || isModified(node), "Tries to get description of node that is not changed or modified");
			return fRewrite.getDescription(node);
		}
		return null;
	}
	
	private TextEdit doTextInsert(int offset, String insertString, String description) {
		TextEdit edit= SimpleTextEdit.createInsert(offset, insertString);
		fCurrentEdit.add(edit);
		if (description != null) {
			addDescription(description, edit);
		}
		return edit;
	}

	private void addDescription(String description, TextEdit edit) {
		GroupDescription groupDesc= (GroupDescription) fGroupDescriptions.get(description);
		if (groupDesc == null) {
			groupDesc= new GroupDescription(description);
			fGroupDescriptions.put(description, groupDesc);
		}
		groupDesc.addTextEdit(edit);
	}

	
	private TextEdit doTextRemove(int offset, int len, String description) {
		TextEdit edit= SimpleTextEdit.createDelete(offset, len);
		fCurrentEdit.add(edit);
		if (description != null) {
			addDescription(description, edit);
		}
		return edit;
	}
	
	private TextEdit doTextRemoveAndVisit(int offset, int len, ASTNode node) {
		TextEdit edit= doTextRemove(offset, len, getDescription(node));

		fCurrentEdit= edit;
		node.accept(this);
		fCurrentEdit= edit.getParent();

		return edit;
	}	
	
	
	private TextEdit doTextReplace(int offset, int len, String insertString, String description) {
		TextEdit edit= SimpleTextEdit.createReplace(offset, len, insertString);
		fCurrentEdit.add(edit);
		if (description != null) {
			addDescription(description, edit);
		}
		return edit;
	}
		
	private TextEdit doTextCopy(ASTNode copiedNode, int destOffset, int sourceIndentLevel, String destIndentString, int tabWidth, String description) {
		CopySourceEdit sourceEdit= fRewrite.getCopySourceEdit(copiedNode);
		Assert.isTrue(sourceEdit != null, "Copy source not annotated" + copiedNode.toString());
		
		CopyIndentedTargetEdit targetEdit= new CopyIndentedTargetEdit(destOffset, sourceIndentLevel, destIndentString, tabWidth);
		targetEdit.setSourceEdit(sourceEdit);
		
		fCurrentEdit.add(targetEdit);
		
		if (description != null) {
			addDescription(description, sourceEdit);
			addDescription(description, targetEdit);
		}
		return targetEdit;
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
	
	private int getPosBeforeSpaces(int pos) {
		while (pos > 0 && Strings.isIndentChar(fTextBuffer.getChar(pos - 1))) {
			pos--;
		}
		return pos;
	}	
	
	private void checkNoModification(ASTNode node) {
		Assert.isTrue(!isModified(node), "Can not modify " + node.getClass().getName());
	}
	
	private void checkNoChange(ASTNode node) {
		Assert.isTrue(!isChanged(node), "Can not insert or replace node in " + node.getParent().getClass().getName());
	}
	
	private void checkNoInsertOrRemove(ASTNode node) {
		Assert.isTrue(!isInserted(node) && !isRemoved(node), "Can not insert or remove node " + node + " in " + node.getParent().getClass().getName());
	}	
	
	private void checkNoInsert(ASTNode node) {
		Assert.isTrue(!isInserted(node), "Can not insert node in " + node.getParent().getClass().getName());
	}		
	
	
	private int rewriteRequiredNode(ASTNode node) {
		checkNoInsertOrRemove(node);
		if (isReplaced(node)) {
			int offset= node.getStartPosition();
			doTextRemoveAndVisit(offset, node.getLength(), node);
			doTextInsert(offset, getReplacingNode(node), getIndent(offset), true, getDescription(node));
		} else {
			node.accept(this);
		}
		return node.getStartPosition() + node.getLength();
	}
	
	private int rewriteNode(ASTNode node, int offset, String prefix) {
		if (isInserted(node)) {
			String description= getDescription(node);
			doTextInsert(offset, prefix, description);
			doTextInsert(offset, node, getIndent(offset), true, description);
			return offset;
		} else if (isRemoved(node)) {
			// if there is a prefix, remove the prefix as well
			int len= node.getStartPosition() + node.getLength() - offset;
			doTextRemoveAndVisit(offset, len, node);
		} else if (isReplaced(node)) {
			doTextRemoveAndVisit(node.getStartPosition(), node.getLength(), node);
			doTextInsert(node.getStartPosition(), getReplacingNode(node), getIndent(offset), true, getDescription(node));
		} else {
			node.accept(this);
		}
		return node.getStartPosition() + node.getLength();
	}		

	private int rewriteOptionalQualifier(ASTNode node, int startPos) {
		if (isInserted(node)) {
			String description= getDescription(node);
			doTextInsert(startPos, node, getIndent(startPos), true, description);
			doTextInsert(startPos, ".", description);
			return startPos;
		} else {
			if (isRemoved(node)) {
				try {
					int dotStart= node.getStartPosition() + node.getLength();
					int dotEnd= ASTResolving.getPositionAfter(getScanner(dotStart), ITerminalSymbols.TokenNameDOT);
					doTextRemoveAndVisit(startPos, dotEnd - startPos, node);
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			} else if (isReplaced(node)) {
				doTextRemoveAndVisit(startPos, node.getLength(), node);
				doTextInsert(startPos, getReplacingNode(node), getIndent(startPos), true, getDescription(node));
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
		checkNoInsertOrRemove(elem);
		if (isReplaced(elem)) {
			replaceParagraph(elem);
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
			if (isRemoved(elem)) {
				removeParagraph(elem);
			} else if (isReplaced(elem)) {
				replaceParagraph(elem);
			} else {
				elem.accept(this);
			}
			return elem;
		}
	}
	

	private void rewriteMethodBody(MethodDeclaration methodDecl, Block body) {
		if (isInserted(body)) {
			try {	
				IScanner scanner= getScanner(methodDecl.getStartPosition());
				ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameSEMICOLON);
				int startPos= scanner.getCurrentTokenStartPosition();
				int endPos= methodDecl.getStartPosition() + methodDecl.getLength();
				String description= getDescription(body);
				doTextRemove(startPos, endPos - startPos, description);
				doTextInsert(startPos, " ", description);
				doTextInsert(startPos, body, getIndent(methodDecl.getStartPosition()), true, description);
			} catch (InvalidInputException e) {
				// ignore
			}
		} else if (isRemoved(body)) {
			doTextRemoveAndVisit(body.getStartPosition(), body.getLength(), body);
			doTextInsert(body.getStartPosition(), ";", getDescription(body));
		} else if (isReplaced(body)) {
			doTextRemoveAndVisit(body.getStartPosition(), body.getLength(), body);
			doTextInsert(body.getStartPosition(), getReplacingNode(body), getIndent(body.getStartPosition()), true,getDescription(body));
		} else {
			body.accept(this);
		}
	}
	
	private void rewriteExtraDimensions(int oldDim, int newDim, int pos, String description) {
		if (oldDim < newDim) {
			for (int i= oldDim; i < newDim; i++) {
				doTextInsert(pos, "[]", description);
			}
		} else if (newDim < oldDim) {
			try {
				IScanner scanner= getScanner(pos);
				for (int i= newDim; i < oldDim; i++) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameRBRACKET);
				}
				doTextRemove(pos, scanner.getCurrentTokenEndPosition() + 1 - pos, description);
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
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
			if (isChanged(elem)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isAllRemoved(List list) {
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (!isRemoved(elem)) {
				return false;
			}
		}
		return true;
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
	
	private int rewriteList(List list, int startPos, String keyword) {

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
			} else {
				return startPos; // before == 0 && after == 0
			}
		}
		
		if (before == 0) { // creating a new list -> insert keyword first (e.g. " throws ")
			String description= getDescription((ASTNode) list.get(0)); // first node is insert
			doTextInsert(startPos, keyword, description);
		}
		int initIndent= getIndent(startPos);
		
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isInserted(elem)) {
				String description= getDescription(elem);
				after--;
				doTextInsert(currPos, elem, initIndent, true, description);
				if (after != 0) { // not the last that will be entered
					doTextInsert(currPos, ", ", description);
				}
			} else {
				before--;
				int currEnd= elem.getStartPosition() + elem.getLength();
				int nextStart= getNextExistingStartPos(list, i + 1, currEnd); // start of next 
				
				if (isRemoved(elem)) {
					// delete including the comma
					doTextRemoveAndVisit(currPos, nextStart - currPos, elem);
				} else if (isReplaced(elem)) {
					String description= getDescription(elem);
					ASTNode changed= getReplacingNode(elem);
					after--;
					if (after == 0) { // will be last node -> remove comma
						doTextRemoveAndVisit(currPos, nextStart - currPos, elem);
						doTextInsert(currPos, changed, initIndent, true, description);
					} else if (before == 0) { // was last, but not anymore -> add comma
						doTextRemoveAndVisit(currPos, currEnd - currPos, elem);
						doTextInsert(currPos, changed, initIndent, true, description);
						doTextInsert(currPos, ", ", description);
					} else {
						doTextRemoveAndVisit(currPos, currEnd - currPos, elem);
						doTextInsert(currPos, changed, initIndent, true, description);
					}
				} else { // no change
					elem.accept(this);

					after--;
					if (after == 0 && before != 0) { // will be last node -> remove comma
						String description= getDescription((ASTNode) list.get(i + 1)); // next node is removed
						doTextRemove(currEnd, nextStart - currEnd, description);
					} else if (after != 0 && before == 0) { // was last, but not anymore -> add comma
						String description= getDescription((ASTNode) list.get(i + 1)); // next node is changed
						doTextInsert(currEnd, ", ", description);
					}					
				}
				currPos= nextStart;
			}
		}
		return currPos;
	}
	
	private void removeParagraph(ASTNode elem) {
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
		doTextRemoveAndVisit(start, end - start, elem);
	}	
	
	
	private void replaceParagraph(ASTNode elem) {
		int startLine= fTextBuffer.getLineOfOffset(elem.getStartPosition());
		int indent= fTextBuffer.getLineIndent(startLine, CodeFormatterUtil.getTabWidth());
			
		doTextRemoveAndVisit(elem.getStartPosition(), elem.getLength(), elem);
		doTextInsert(elem.getStartPosition(), getReplacingNode(elem), indent, true, getDescription(elem));
	}

	private void insertParagraph(ASTNode elem, ASTNode sibling, int insertPos, int indent, boolean additionalNewLine, boolean useIndentOfSibling) {
		if (sibling != null) {
			if (useIndentOfSibling) {
				indent= getIndent(sibling.getStartPosition());
			}
			insertPos= sibling.getStartPosition() + sibling.getLength();
		}
		String description= getDescription(elem);
		doTextInsert(insertPos, fTextBuffer.getLineDelimiter(), description);
		doTextInsert(insertPos, elem, indent, false, description);
		if (additionalNewLine) {
			doTextInsert(insertPos, fTextBuffer.getLineDelimiter(), description);
		}
	}

	private int getIndent(int pos) {
		int line= fTextBuffer.getLineOfOffset(pos);
		return fTextBuffer.getLineIndent(line, CodeFormatterUtil.getTabWidth());
	}

	private void doTextInsert(int insertOffset, ASTNode node, int initialIndentLevel, boolean removeLeadingIndent, String description) {		
		Assert.isTrue(node.getStartPosition() == -1, "Node to generate must be new nodes");
		
		ASTWithExistingFlattener flattener= new ASTWithExistingFlattener();
		node.accept(flattener);
		String formatted= flattener.getFormattedResult(initialIndentLevel, fTextBuffer.getLineDelimiter());
		
		ASTWithExistingFlattener.NodeMarker[] markers= flattener.getNodeMarkers();
		
		int tabWidth= CodeFormatterUtil.getTabWidth();
		int currPos= 0;
		if (removeLeadingIndent) {
			while (currPos < formatted.length() && Strings.isIndentChar(formatted.charAt(currPos))) {
				currPos++;
			}
		}
		for (int i= 0; i < markers.length; i++) {
			int offset= markers[i].offset;
			
			String insertStr= formatted.substring(currPos, offset);
			doTextInsert(insertOffset, insertStr, description);
			String destIndentString=  Strings.getIndentString(getCurrentLine(formatted, offset), tabWidth);
			
			Object data= markers[i].data;
			if (data instanceof ASTNode) {
				ASTNode existingNode= (ASTNode) data;
				int srcIndentLevel= getIndent(existingNode.getStartPosition());
				doTextCopy(existingNode, insertOffset, srcIndentLevel, destIndentString, tabWidth, description);
			} else if (data instanceof String) {
				String str= Strings.changeIndent((String) data, 0, tabWidth, destIndentString); 
				doTextInsert(insertOffset, str, description);
			}
		
			currPos= offset + markers[i].length;
		}
		String insertStr= formatted.substring(currPos);
		doTextInsert(insertOffset, insertStr, description);
	}
	
	private String getCurrentLine(String str, int pos) {
		for (int i= pos - 1; i>= 0; i--) {
			char ch= str.charAt(i);
			if (Strings.isLineDelimiterChar(ch)) {
				return str.substring(i + 1, pos);
			}
		}
		return str.substring(0, pos);
	}	
	

	private void rewriteModifiers(int startPos, int oldModifiers, int newModifiers, String description) {
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
					doTextRemove(currPos, endPos - currPos, description);
				}
			} 
			int addedModifiers= (newModifiers ^ oldModifiers) & newModifiers;
			if (addedModifiers != 0) {
				StringBuffer buf= new StringBuffer();
				ASTFlattener.printModifiers(addedModifiers, buf);
				doTextInsert(endPos, buf.toString(), description);
			}
		} catch (InvalidInputException e) {
			// ignore
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#postVisit(ASTNode)
	 */
	public void postVisit(ASTNode node) {
		TextEdit edit= fRewrite.getCopySourceEdit(node);
		if (edit != null) {
			fCurrentEdit= fCurrentEdit.getParent();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#preVisit(ASTNode)
	 */
	public void preVisit(ASTNode node) {
		Assert.isTrue(node.getStartPosition() != -1, "Node inserted but not marked as inserted: " + node.toString());
		TextEdit edit= fRewrite.getCopySourceEdit(node);
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
			
			rewriteModifiers(typeDecl.getStartPosition(), typeDecl.getModifiers(), modfiedModifiers, getDescription(typeDecl));
			if (modifiedNode.isInterface() != typeDecl.isInterface()) { // change from class to interface or reverse
				invertType= true;
				try {
					IScanner scanner= getScanner(typeDecl.getStartPosition());
					int typeToken= typeDecl.isInterface() ? ITerminalSymbols.TokenNameinterface : ITerminalSymbols.TokenNameclass;
					ASTResolving.readToToken(scanner, typeToken);
					
					String str= typeDecl.isInterface() ? "class" : "interface";
					int start= scanner.getCurrentTokenStartPosition();
					int end= scanner.getCurrentTokenEndPosition() + 1;
					
					doTextReplace(start, end - start, str, getDescription(typeDecl));
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
				doTextInsert(pos, str, getDescription(superClass));
			} else {
				if (isRemoved(superClass)) {
					int endPos= superClass.getStartPosition() + superClass.getLength();
					doTextRemoveAndVisit(pos, endPos - pos, superClass);
				} else if (isReplaced(superClass)) {
					doTextRemoveAndVisit(superClass.getStartPosition(), superClass.getLength(), superClass);
					doTextInsert(superClass.getStartPosition(), getReplacingNode(superClass), 0, false, getDescription(superClass));
				} else {
					superClass.accept(this);
				}
				pos= superClass.getStartPosition() + superClass.getLength();
			}
		}
		// extended interfaces
		List interfaces= typeDecl.superInterfaces();
		
		String keyword= (typeDecl.isInterface() != invertType) ? " extends " : " implements ";
		if (invertType && !isAllRemoved(interfaces)) {
			int firstStart= getNextExistingStartPos(interfaces, 0, pos);
			doTextReplace(pos, firstStart - pos, keyword, getDescription(typeDecl));
			keyword= "";
			pos= firstStart;
		}
		
		if (hasChanges(interfaces)) {
			rewriteList(interfaces, pos, keyword);
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
			rewriteModifiers(methodDecl.getStartPosition(), methodDecl.getModifiers(), modifiedNode.getModifiers(), getDescription(methodDecl));
			willBeConstructor= modifiedNode.isConstructor();
		}
		
		Type returnType= methodDecl.getReturnType();
		if (!willBeConstructor || isRemoved(returnType)) {
			try {
				int startPos= methodDecl.getStartPosition();
				if (isInsertOrRemove(returnType)) {
					startPos= ASTResolving.getPositionAfter(getScanner(startPos), startPos, MODIFIERS);
				}
				rewriteNode(returnType, startPos, " ");
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
		}
	
		int pos= rewriteRequiredNode(methodDecl.getName());
		try {
			List parameters= methodDecl.parameters();
			if (hasChanges(parameters)) {
				pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameLPAREN);
				pos= rewriteList(parameters, pos, "");
			} else {
				pos= visitList(parameters, pos);
			}
			
			if (isModified(methodDecl)) {
				MethodDeclaration modifedNode= (MethodDeclaration) getModifiedNode(methodDecl);
				int oldDim= methodDecl.getExtraDimensions();
				int newDim= modifedNode.getExtraDimensions();
				if (oldDim != newDim) {
					int offset= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameRPAREN);
					rewriteExtraDimensions(oldDim, newDim, offset, getDescription(methodDecl));
				}
			}				
			
			List exceptions= methodDecl.thrownExceptions();
			if (hasChanges(exceptions)) {
				pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameRPAREN);
				int dim= methodDecl.getExtraDimensions();
				while (dim > 0) {
					pos= ASTResolving.getPositionAfter(getScanner(pos), ITerminalSymbols.TokenNameRBRACKET);
					dim--;
				}
				rewriteList(exceptions, pos, " throws ");
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
			if (isChanged(expression)) {
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
		
		String description= null;
		checkNoInsertOrRemove(arrayType);
		if (isReplaced(arrayType)) { // changed arraytype can have different dimension or type name
			ArrayType replacingType= (ArrayType) getReplacingNode(arrayType);
			description= getDescription(arrayType);
			
			Assert.isTrue(replacingType != null, "Cant remove array type in ArrayCreation");
			Type newType= replacingType.getElementType();
			Type oldType= arrayType.getElementType();
			if (!newType.equals(oldType)) {
				doTextRemove(oldType.getStartPosition(), oldType.getLength(), description);
				doTextInsert(oldType.getStartPosition(), newType, 0, false, description);
			}
			nNewBrackets= replacingType.getDimensions();
		}
		arrayType.accept(this);
		List dimExpressions= node.dimensions(); // dimension node with expressions
		try {
			// offset on first opening brace
			int offset= ASTResolving.getPositionBefore(getScanner(arrayType.getStartPosition()), ITerminalSymbols.TokenNameLBRACKET);
			for (int i= 0; i < dimExpressions.size(); i++) {
				ASTNode elem = (ASTNode) dimExpressions.get(i);
				if (isInserted(elem)) { // insert new dimension
					description= getDescription(elem);
					doTextInsert(offset, "[", description);
					doTextInsert(offset, elem, 0, false, description);
					doTextInsert(offset, "]", description);
					nNewBrackets--;
				} else {
					int elemEnd= elem.getStartPosition() + elem.getLength();
					int endPos= ASTResolving.getPositionAfter(getScanner(elemEnd), ITerminalSymbols.TokenNameRBRACKET);
					if (isRemoved(elem)) {
						// remove includes open and closing brace
						description= getDescription(elem);
						doTextRemoveAndVisit(offset, endPos - offset, elem);
					} else if (isReplaced(elem)) {
						description= getDescription(elem);
						doTextRemoveAndVisit(elem.getStartPosition(), elem.getLength(), elem);
						doTextInsert(elem.getStartPosition(), getReplacingNode(elem), 0, false, description);
						nNewBrackets--;
					} else {
						elem.accept(this);
						nNewBrackets--;
					}
					offset= endPos;
					nOldBrackets--;
				}
			}
			rewriteExtraDimensions(nOldBrackets, nNewBrackets, offset, description);
		
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
		rewriteList(expressions, node.getStartPosition() + 1, "");
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		checkNoModification(node);
		checkNoChange(node.getComponentType());
		// no changes possible // need to check
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		checkNoChange(node.getExpression());
		checkNoChange(node.getMessage());
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
				doTextReplace(scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenSource().length, modifiedOp.toString(), getDescription(node));
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
			if (isChanged(label)) {
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
				rewriteList(arguments, startpos, "");
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
				rewriteList(arguments, startpos, "");
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
			if (isChanged(label)) {
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
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers(), getDescription(node));
		}
		
		Type type= node.getType();
		rewriteRequiredNode(type);
		
		List fragments= node.fragments();
		int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
		rewriteList(fragments, startPos, "");

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
				pos= rewriteList(initializers, startOffset, "");
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
				rewriteList(updaters, startOffset, "");
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
				doTextInsert(name.getStartPosition() + name.getLength(), ".*", getDescription(node));
			} else if (node.isOnDemand() && !modifiedNode.isOnDemand()) {
				try {
					int startPos= name.getStartPosition() + name.getLength();
					int endPos= ASTResolving.getPositionBefore(getScanner(startPos), ITerminalSymbols.TokenNameSEMICOLON);
					doTextRemove(startPos, endPos - startPos, getDescription(node));
				} catch (InvalidInputException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return false;
	}
	
	private void replaceOperation(int posBeforeOperation, String newOperation, String description) {
		try {
			IScanner scanner= getScanner(posBeforeOperation);
			scanner.getNextToken(); // op			
			doTextReplace(scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenSource().length, newOperation, description);
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
				replaceOperation(pos, operation, getDescription(node));
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
					replaceOperation(pos, operation, getDescription(node));
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
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers(), getDescription(node));
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
		checkNoChange(node);
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
				rewriteList(arguments, startOffset, "");
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
				replaceOperation(pos, newOperation, getDescription(node));
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
				replaceOperation(node.getStartPosition(), newOperation, getDescription(node));
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
		checkNoChange(node.getName());
		checkNoChange(node.getQualifier());
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
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers(), getDescription(node));
		}
		
		rewriteRequiredNode(node.getType());
		int pos= rewriteRequiredNode(node.getName());
		if (isModified(node)) {
			String description= getDescription(node);
			SingleVariableDeclaration modifedNode= (SingleVariableDeclaration) getModifiedNode(node);
			rewriteExtraDimensions(node.getExtraDimensions(), modifedNode.getExtraDimensions(), pos, description);
		}			
		
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
				rewriteList(arguments, pos, "");
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
				rewriteList(arguments, pos, "");
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
		pos= rewriteList(catchBlocks, pos, " ");
		
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
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers(),getDescription(node));
		}
		
		Type type= node.getType();
		rewriteRequiredNode(type);
		
		List fragments= node.fragments();
		int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
		rewriteList(fragments, startPos, "");

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		int pos= rewriteRequiredNode(node.getName());
		
		if (isModified(node)) {
			String description= getDescription(node);
			VariableDeclarationFragment modifedNode= (VariableDeclarationFragment) getModifiedNode(node);
			rewriteExtraDimensions(node.getExtraDimensions(), modifedNode.getExtraDimensions(), pos, description);
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
			rewriteModifiers(node.getStartPosition(), node.getModifiers(), modifedNode.getModifiers(), getDescription(node));
		}
		
		Type type= node.getType();
		rewriteRequiredNode(type);
		
		List fragments= node.fragments();
		int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
		rewriteList(fragments, startPos, "");

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
