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
package org.eclipse.jdt.internal.corext.dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.CopyTargetEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Infrastructure to support code modifications. Existing code must stay untouched, new code
 * added with correct formatting, moved code left with the users formattings / comments.
 * Idea:
 * - Get the AST for existing code 
 * - Inserting new nodes or mark existing nodes as replaced, removed, copied or moved.
 * - This visitor analyses the changes or annotations and generates text edits
 * (text manipulation API) that describe the required code changes. See test
 * cases in org.eclipse.jdt.ui.tests / package org.eclipse.jdt.ui.tests.
 * astrewrite for examples
 */
public class ASTRewriteAnalyzer extends ASTVisitor {

	protected TextBuffer fTextBuffer;
	private TextEdit fCurrentEdit;
	
	private TokenScanner fTokenScanner; // shared scanner
	private ASTRewrite fRewrite;
	
	private HashMap fGroupDescriptions;
	
	/**
	 * Constructor for ASTChangeAnalyzer.
	 */
	public ASTRewriteAnalyzer(TextBuffer textBuffer, TextEdit rootEdit, ASTRewrite rewrite, HashMap resGroupDescriptions) {
		fTextBuffer= textBuffer;
		fTokenScanner= null;
		fRewrite= rewrite;
		fCurrentEdit= rootEdit;
		fGroupDescriptions= resGroupDescriptions;
	}
	
	final ListRewriter getDefaultRewriter() {
		return new ListRewriter();
	}	
	
	final TokenScanner getScanner() {
		if (fTokenScanner == null) {
			IScanner scanner= ToolFactory.createScanner(true, false, false, false);
			scanner.setSource(fTextBuffer.getContent().toCharArray());
			fTokenScanner= new TokenScanner(scanner);
		}
		return fTokenScanner;
	}	
	
	public static Object createSourceCopy(int offset, int length) {
		return new CopyIndentedSourceEdit(offset, length);
	}
	
	final boolean isChanged(ASTNode node) {
		return isInserted(node) || isRemoved(node) || isReplaced(node);
	}
	
	final boolean isInsertOrRemove(ASTNode node) {
		return isInserted(node) || isRemoved(node);
	}
	
	final boolean isInserted(ASTNode node) {
		return fRewrite.isInserted(node);
	}
	
	final boolean isReplaced(ASTNode node) {
		return fRewrite.isReplaced(node);
	}
	
	final boolean isRemoved(ASTNode node) {
		return fRewrite.isRemoved(node);
	}	
	
	final boolean isModified(ASTNode node) {
		return fRewrite.isModified(node);
	}
	
	final ASTNode getModifiedNode(ASTNode node) {
		return fRewrite.getModifiedNode(node);
	}

	final ASTNode getReplacingNode(ASTNode node) {
		return fRewrite.getReplacingNode(node);
	}
	
	final String getDescription(ASTNode node) {
		if (fGroupDescriptions != null) {
			Assert.isTrue(isChanged(node) || isModified(node), "Tries to get description of node that is not changed or modified"); //$NON-NLS-1$
			return fRewrite.getDescription(node);
		}
		return null;
	}
	
	final TextEdit doTextInsert(int offset, String insertString, String description) {
		TextEdit edit= SimpleTextEdit.createInsert(offset, insertString);
		fCurrentEdit.add(edit);
		if (description != null) {
			addDescription(description, edit);
		}
		return edit;
	}
	
	final void addDescription(String description, TextEdit edit) {
		GroupDescription groupDesc= (GroupDescription) fGroupDescriptions.get(description);
		if (groupDesc == null) {
			groupDesc= new GroupDescription(description);
			fGroupDescriptions.put(description, groupDesc);
		}
		groupDesc.addTextEdit(edit);
	}

	
	final TextEdit doTextRemove(int offset, int len, String description) {
		TextEdit edit= SimpleTextEdit.createDelete(offset, len);
		fCurrentEdit.add(edit);
		if (description != null) {
			addDescription(description, edit);
		}
		return edit;
	}
	
	final TextEdit doTextRemoveAndVisit(int offset, int len, ASTNode node) {
		TextEdit edit= doTextRemove(offset, len, getDescription(node));

		fCurrentEdit= edit;
		doVisit(node);
		fCurrentEdit= edit.getParent();

		return edit;
	}
	
	final void doVisit(ASTNode node) {
		node.accept(this);
	}	
	
	
	final TextEdit doTextReplace(int offset, int len, String insertString, String description) {
		TextEdit edit= SimpleTextEdit.createReplace(offset, len, insertString);
		fCurrentEdit.add(edit);
		if (description != null) {
			addDescription(description, edit);
		}
		return edit;
	}
		
	final TextEdit doTextCopy(ASTNode copiedNode, int destOffset, int sourceIndentLevel, String destIndentString, int tabWidth, String description) {
		CopyIndentedSourceEdit sourceEdit= (CopyIndentedSourceEdit) fRewrite.getCopySourceEdit(copiedNode);
		if (sourceEdit == null) {
			Assert.isTrue(false, "Copy source not annotated" + copiedNode.toString()); //$NON-NLS-1$
		}
		sourceEdit.initialize(sourceIndentLevel, destIndentString, tabWidth);
		
		CopyTargetEdit targetEdit= new CopyTargetEdit(destOffset, sourceEdit);
		fCurrentEdit.add(targetEdit);
		
		if (description != null) {
			addDescription(description, sourceEdit);
			addDescription(description, targetEdit);
		}
		return targetEdit;
	}
			
	private int getPosBeforeSpaces(int pos) {
		while (pos > 0 && Strings.isIndentChar(fTextBuffer.getChar(pos - 1))) {
			pos--;
		}
		return pos;
	}	
	
	private void checkNoModification(ASTNode node) {
		if (isModified(node)) {
			Assert.isTrue(false, "Can not modify " + node.getClass().getName()); //$NON-NLS-1$
		}
	}
	
	private void checkNoChange(ASTNode node) {
		if (isChanged(node)) {
			Assert.isTrue(false, "Can not insert or replace node in " + node.getParent().getClass().getName()); //$NON-NLS-1$
		}
	}
	
	private void checkNoInsertOrRemove(ASTNode node) {
		if (isInserted(node) || isRemoved(node)) {	
			Assert.isTrue(false, "Can not insert or remove node " + node + " in " + node.getParent().getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private class ListRewriter {
		protected String fContantSeparator;
		protected int fStartPos;
		
		protected List fList;
		
		protected String getSeparatorString(ASTNode node) {
			return fContantSeparator;
		}
		
		protected int getInitialIndent() {
			return getIndent(fStartPos);
		}
		
		protected int getNodeIndent(ASTNode node) {
			if (isInserted(node)) {
				ASTNode prev= findPreviousExistingNode(fList, node);
				if (prev == null) {
					return getInitialIndent();
				}
				return getIndent(prev.getStartPosition());
			}
			return getIndent(node.getStartPosition());
		}
		
		protected int getStartOfNextNode(int nextIndex, int defaultPos) {
			return getNextExistingStartPos(fList, nextIndex, defaultPos);
		}
		
		public int rewriteList(List list, int startPos, String keyword, String separator) {
			fContantSeparator= separator;
			return rewriteList(list, startPos, keyword);
		}
		
		public int rewriteList(List list, int startPos, String keyword) {
			fList= list;
			fStartPos= startPos;
			
			int currPos= startPos;
			
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
						currPos= getStartOfNextNode(0, startPos);
					}
					if (!isRemoved(elem)) {
						after++;
					}
				}
			}
		
			if (after == 0) {
				if (before != 0) { // all deleted
					currPos= startPos;
				} else {
					return startPos; // before == 0 && after == 0
				}
			}
		
			if (before == 0 && keyword.length() > 0) { // creating a new list -> insert keyword first (e.g. " throws ")
				String description= getDescription((ASTNode) list.get(0)); // first node is insert
				doTextInsert(startPos, keyword, description);
			}
	
			for (int i= 0; i < list.size(); i++) {
				ASTNode elem= (ASTNode) list.get(i);
				if (isInserted(elem)) {
					String description= getDescription(elem);
					after--;
					doTextInsert(currPos, elem, getNodeIndent(elem), true, description);
					if (after != 0) { // not the last that will be entered
						doTextInsert(currPos, getSeparatorString(elem), description);
					}
				} else {
					before--;
					int currEnd= elem.getStartPosition() + elem.getLength();
					int nextStart= getStartOfNextNode(i + 1, currEnd); // start of next 
				
					if (isRemoved(elem)) {
						// delete including the comma
						doTextRemoveAndVisit(currPos, nextStart - currPos, elem);
					} else if (isReplaced(elem)) {
						String description= getDescription(elem);
						ASTNode changed= getReplacingNode(elem);
						after--;
					
						if (after == 0) { // will be last node -> remove comma
							doTextRemoveAndVisit(currPos, nextStart - currPos, elem);
							doTextInsert(currPos, changed, getNodeIndent(elem), true, description);
						} else if (before == 0) { // was last, but not anymore -> add comma
							doTextRemoveAndVisit(currPos, currEnd - currPos, elem);
							doTextInsert(currPos, changed, getNodeIndent(elem), true, description);
							doTextInsert(currPos, getSeparatorString(elem), description);
						} else {
							doTextRemoveAndVisit(currPos, currEnd - currPos, elem);
							doTextInsert(currPos, changed, getNodeIndent(elem), true, description);
						}
					} else { // no change
						doVisit(elem);

						after--;
						if (after == 0 && before != 0) { // will be last node -> remove comma
							String description= getDescription((ASTNode) list.get(i + 1)); // next node is removed
							doTextRemove(currEnd, nextStart - currEnd, description);
						} else if (after != 0 && before == 0) { // was last, but not anymore -> add comma
							String description= getDescription((ASTNode) list.get(i + 1)); // next node is changed
							doTextInsert(currEnd, getSeparatorString(elem), description);
						}					
					}
					currPos= nextStart;
				}
			}
			return currPos;
		}
	}
		
	private int rewriteRequiredNode(ASTNode node) {
		checkNoInsertOrRemove(node);
		if (isReplaced(node)) {
			int offset= node.getStartPosition();
			doTextRemoveAndVisit(offset, node.getLength(), node);
			doTextInsert(offset, getReplacingNode(node), getIndent(offset), true, getDescription(node));
		} else {
			doVisit(node);
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
			doVisit(node);
		}
		return node.getStartPosition() + node.getLength();
	}		

	private int rewriteOptionalQualifier(ASTNode node, int startPos) {
		if (isInserted(node)) {
			String description= getDescription(node);
			doTextInsert(startPos, node, getIndent(startPos), true, description);
			doTextInsert(startPos, ".", description); //$NON-NLS-1$
			return startPos;
		} else {
			if (isRemoved(node)) {
				try {
					int dotStart= node.getStartPosition() + node.getLength();
					int dotEnd= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameDOT, dotStart);
					doTextRemoveAndVisit(startPos, dotEnd - startPos, node);
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			} else if (isReplaced(node)) {
				doTextRemoveAndVisit(startPos, node.getLength(), node);
				doTextInsert(startPos, getReplacingNode(node), getIndent(startPos), true, getDescription(node));
			} else {
				doVisit(node);
			}
			return node.getStartPosition() + node.getLength();
		}
	}
	
	private class ParagraphListRewriter extends ListRewriter {
		
		public final int DEFAULT_SPACING= 1;
		
		private int fInitialIndent;
		private int fSeparatorLines;
		
		public ParagraphListRewriter(int initialIndent, int separator) {
			fInitialIndent= initialIndent;
			fSeparatorLines= separator;
		}
		
		protected int getInitialIndent() {
			return fInitialIndent;
		}		
				
		protected String getSeparatorString(ASTNode node) {
			int newLines= fSeparatorLines == -1 ? getNewLines(node) : fSeparatorLines;
			
			String lineDelim= fTextBuffer.getLineDelimiter();
			
			StringBuffer buf= new StringBuffer(lineDelim);
			for (int i= 0; i < newLines; i++) {
				buf.append(lineDelim);
			}
			buf.append(CodeFormatterUtil.createIndentString(getNodeIndent(node)));
			return buf.toString();
		}
		
		private int getNewLines(ASTNode curr) {
			int nodeType= curr.getNodeType();
			ASTNode last= null;
			int additionalLines= -1;
			for (int i= 0; i < fList.size(); i++) {
				ASTNode elem= (ASTNode) fList.get(i);
				if (!isInserted(elem)) {
					if (last != null) {
						if (elem.getNodeType() == nodeType && last.getNodeType() == nodeType) {
							return countEmptyLines(last);
						} else if (additionalLines == -1) {
							additionalLines= countEmptyLines(last);
						}
					}
					last= elem;
				}
			}
			if (additionalLines != -1) {
				return additionalLines;
			}
			return DEFAULT_SPACING;
		}

		private int countEmptyLines(ASTNode last) {
			int lastLine= fTextBuffer.getLineOfOffset(last.getStartPosition() + last.getLength());
			int scanLine= lastLine + 1;
			int numLines= fTextBuffer.getNumberOfLines();
			while(scanLine < numLines && Strings.containsOnlyWhitespaces(fTextBuffer.getLineContent(scanLine))){
				scanLine++;
			}
			return scanLine - lastLine - 1;	
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer.ListRewriter#getStartPosOfNextNode(int, int)
		 */
		protected int getStartOfNextNode(int nextIndex, int currEnd) {
			try {
				for (int i= nextIndex; i < fList.size(); i++) {
					if (!isInserted((ASTNode) fList.get(i))) {
						return getScanner().getTokenCommentStart(currEnd, fTextBuffer);
					}
				}
			} catch (CoreException e) {
				// ignore
			}
			return currEnd;
		}
	}
	
	private int rewriteParagraphList(List list, int insertPos, int insertIndent, int separator) {
		boolean hasChanges= false;
		boolean hasExisting= false;
		
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			hasChanges |= isChanged(elem);
			hasExisting |= !isInserted(elem);
		}
		if (!hasChanges) {
			return visitList(list, insertPos);
		}
		
		ParagraphListRewriter listRewriter= new ParagraphListRewriter(insertIndent, separator);
		StringBuffer lead= new StringBuffer();
		if (!hasExisting) {
			lead.append(fTextBuffer.getLineDelimiter());
			lead.append(fTextBuffer.getLineDelimiter());
			lead.append(CodeFormatterUtil.createIndentString(insertIndent));
		}
		return listRewriter.rewriteList(list, insertPos, lead.toString());
	}
	
	private int rewriteRequiredParagraph(ASTNode elem) {
		checkNoInsertOrRemove(elem);
		if (isReplaced(elem)) {
			replaceParagraph(elem);
		} else {
			doVisit(elem);
		}
		return elem.getStartPosition() + elem.getLength();
	}	

	/**
	 * Rewrite a paragraph (node that is on a new line and has same indent as previous
	 */
	private ASTNode rewriteParagraph(ASTNode elem, ASTNode sibling, int insertPos, int insertIndent, int additionalNewLine, boolean useIndentOfSibling) {
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
				doVisit(elem);
			}
			return elem;
		}
	}
	

	private void rewriteMethodBody(MethodDeclaration methodDecl, Block body, int startPos) { 
		if (isInserted(body)) {
			int endPos= methodDecl.getStartPosition() + methodDecl.getLength();
			String description= getDescription(body);
			doTextRemove(startPos, endPos - startPos, description);
			doTextInsert(startPos, " ", description); //$NON-NLS-1$
			doTextInsert(startPos, body, getIndent(methodDecl.getStartPosition()), true, description);
		} else if (isRemoved(body)) {
			int endPos= methodDecl.getStartPosition() + methodDecl.getLength();
			doTextRemoveAndVisit(startPos, endPos - startPos, body);
			doTextInsert(startPos, ";", getDescription(body)); //$NON-NLS-1$
		} else if (isReplaced(body)) {
			doTextRemoveAndVisit(body.getStartPosition(), body.getLength(), body);
			doTextInsert(body.getStartPosition(), getReplacingNode(body), getIndent(body.getStartPosition()), true,getDescription(body));
		} else {
			doVisit(body);
		}
	}
	
	private void rewriteExtraDimensions(int oldDim, int newDim, int pos, String description) {
		if (oldDim < newDim) {
			for (int i= oldDim; i < newDim; i++) {
				doTextInsert(pos, "[]", description); //$NON-NLS-1$
			}
		} else if (newDim < oldDim) {
			try {
				getScanner().setOffset(pos);
				for (int i= newDim; i < oldDim; i++) {
					getScanner().readToToken(ITerminalSymbols.TokenNameRBRACKET);
				}
				doTextRemove(pos, getScanner().getCurrentEndOffset() - pos, description);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}	
	
	final int getNextExistingStartPos(List list, int listStartIndex, int defaultOffset) {
		for (int i= listStartIndex; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (!isInserted(elem)) {
				return elem.getStartPosition();
			}
		}
		return defaultOffset;
	}
	
	final ASTNode findPreviousExistingNode(List list, ASTNode node) {
		ASTNode res= null;
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (elem == node) {
				return res;
			}
			if (!isInserted(elem)) {
				res= elem;
			}
		}
		return null;		
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
			doVisit(curr);
		}
		if (curr == null) {
			return defaultPos;
		}
		return curr.getStartPosition() + curr.getLength();
	}
	
	private int getEndLineOffset(int offset) {
		TextRegion lineRegion= fTextBuffer.getLineInformationOfOffset(offset);
		int pos= lineRegion.getOffset() + lineRegion.getLength();
		for (int i= offset; i < pos; i++) {
			char ch= fTextBuffer.getChar(i);
			if (!Character.isWhitespace(ch)) {
				if (ch == '/' && i + 1 < pos && fTextBuffer.getChar(i + 1) == '/') {
					return pos;
				}
				return i;
			}
		}
		return pos;
	}
	
	
	private void removeParagraph(ASTNode elem) {
		int start= elem.getStartPosition();
		int end= start + elem.getLength();
		
		// move end to include all spaces, tabs
		end= getEndLineOffset(end);
		
		int startLine= fTextBuffer.getLineOfOffset(start);
		if (startLine > 0) {
			TextRegion prevRegion= fTextBuffer.getLineInformation(startLine - 1);
			int cutPos= prevRegion.getOffset() + prevRegion.getLength();
			try {
				if (getScanner().getNextStartOffset(cutPos, true) == start) {
					start= cutPos;
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
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

	private void insertParagraph(ASTNode elem, ASTNode sibling, int insertPos, int indent, int additionalNewLines, boolean useIndentOfSibling) {
		String description= getDescription(elem);
		String newLines= null;
		if (additionalNewLines > 0) {
			StringBuffer buf= new StringBuffer();
			for (int i= 0; i < additionalNewLines; i++) {
				buf.append(fTextBuffer.getLineDelimiter());
			}
			newLines= buf.toString();
		}

		if (sibling != null) {
			if (useIndentOfSibling) {
				indent= getIndent(sibling.getStartPosition());
			}
			int end= sibling.getStartPosition() + sibling.getLength();
			
			insertPos= getEndLineOffset(end);
			if (newLines != null) {
				doTextInsert(insertPos, newLines, description);
				newLines= null;
			}
		}
		doTextInsert(insertPos, fTextBuffer.getLineDelimiter(), description);
		doTextInsert(insertPos, elem, indent, false, description);
		if (newLines != null) {
			doTextInsert(insertPos, newLines, description);
		}
	}

	final int getIndent(int pos) {
		int line= fTextBuffer.getLineOfOffset(pos);
		return fTextBuffer.getLineIndent(line, CodeFormatterUtil.getTabWidth());
	}

	final void doTextInsert(int insertOffset, ASTNode node, int initialIndentLevel, boolean removeLeadingIndent, String description) {		
		
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
			if (offset != currPos) {
				String insertStr= formatted.substring(currPos, offset);
				doTextInsert(insertOffset, insertStr, description);
			}
			String destIndentString=  Strings.getIndentString(getCurrentLine(formatted, offset), tabWidth);
			
			Object data= markers[i].data;
			if (data instanceof ASTNode) {
				ASTNode existingNode= (ASTNode) data;
				int srcIndentLevel= getIndent(existingNode.getStartPosition());
				doTextCopy(existingNode, insertOffset, srcIndentLevel, destIndentString, tabWidth, description);
			} else if (data instanceof String) {
				String str= Strings.changeIndent((String) data, 0, tabWidth, destIndentString, fTextBuffer.getLineDelimiter()); 
				doTextInsert(insertOffset, str, description);
			}
		
			currPos= offset + markers[i].length;
		}
		if (currPos < formatted.length()) {
			String insertStr= formatted.substring(currPos);
			doTextInsert(insertOffset, insertStr, description);
		}
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
	

	private void rewriteModifiers(int offset, int oldModifiers, int newModifiers, String description) {
		if (oldModifiers == newModifiers) {
			return;
		}
		
		try {
			int tok= getScanner().readNext(offset, true);
			int startPos= getScanner().getCurrentStartOffset();
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
				tok= getScanner().readNext(true);
				int currPos= endPos;
				endPos= getScanner().getCurrentStartOffset();
				if (!keep) {
					doTextRemove(currPos, endPos - currPos, description);
				}
			} 
			int addedModifiers= newModifiers & ~oldModifiers;
			if (addedModifiers != 0) {
				if (startPos != endPos) {
					int visibilityModifiers= addedModifiers & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED);
					if (visibilityModifiers != 0) {
						StringBuffer buf= new StringBuffer();
						ASTFlattener.printModifiers(visibilityModifiers, buf);
						doTextInsert(startPos, buf.toString(), description);
						addedModifiers &= ~visibilityModifiers;
					}
				}
				StringBuffer buf= new StringBuffer();
				ASTFlattener.printModifiers(addedModifiers, buf);
				doTextInsert(endPos, buf.toString(), description);
			}
		} catch (CoreException e) {
			// ignore
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#postVisit(ASTNode)
	 */
	public void postVisit(ASTNode node) {
		TextEdit edit= (TextEdit) fRewrite.getCopySourceEdit(node);
		if (edit != null) {
			fCurrentEdit= fCurrentEdit.getParent();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#preVisit(ASTNode)
	 */
	public void preVisit(ASTNode node) {
		TextEdit edit= (TextEdit) fRewrite.getCopySourceEdit(node);
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
		ASTNode last= rewriteParagraph(packageDeclaration, null, 0, 0, 1, false);
				
		List imports= node.imports();
		int startPos= last != null ? last.getStartPosition() + last.getLength() : 0;
		startPos= rewriteParagraphList(imports, startPos, 0, 0);

		List types= node.types();
		rewriteParagraphList(types, startPos, 0, -1);
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
					int typeToken= typeDecl.isInterface() ? ITerminalSymbols.TokenNameinterface : ITerminalSymbols.TokenNameclass;
					getScanner().readToToken(typeToken, typeDecl.getStartPosition());
					
					String str= typeDecl.isInterface() ? "class" : "interface"; //$NON-NLS-1$ //$NON-NLS-2$
					int start= getScanner().getCurrentStartOffset();
					int end= getScanner().getCurrentEndOffset();
					
					doTextReplace(start, end - start, str, getDescription(typeDecl));
				} catch (CoreException e) {
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
				String str= " extends " + ASTNodes.asString(superClass); //$NON-NLS-1$
				doTextInsert(pos, str, getDescription(superClass));
			} else {
				if (isRemoved(superClass)) {
					int endPos= superClass.getStartPosition() + superClass.getLength();
					doTextRemoveAndVisit(pos, endPos - pos, superClass);
				} else if (isReplaced(superClass)) {
					doTextRemoveAndVisit(superClass.getStartPosition(), superClass.getLength(), superClass);
					doTextInsert(superClass.getStartPosition(), getReplacingNode(superClass), 0, false, getDescription(superClass));
				} else {
					doVisit(superClass);
				}
				pos= superClass.getStartPosition() + superClass.getLength();
			}
		}
		// extended interfaces
		List interfaces= typeDecl.superInterfaces();
		
		String keyword= (typeDecl.isInterface() != invertType) ? " extends " : " implements "; //$NON-NLS-1$ //$NON-NLS-2$
		if (invertType && !isAllRemoved(interfaces)) {
			int firstStart= getNextExistingStartPos(interfaces, 0, pos);
			doTextReplace(pos, firstStart - pos, keyword, getDescription(typeDecl));
			keyword= ""; //$NON-NLS-1$
			pos= firstStart;
		}
		
		if (hasChanges(interfaces)) {
			pos= getDefaultRewriter().rewriteList(interfaces, pos, keyword, ", "); //$NON-NLS-1$
		} else {
			pos= visitList(interfaces, pos);
		}
		
		// type members
		List members= typeDecl.bodyDeclarations();

		// startPos : find position after left brace of type
		try {
			int startIndent= getIndent(typeDecl.getStartPosition()) + 1;
			int startPos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLBRACE, pos);		
			rewriteParagraphList(members, startPos, startIndent, -1);
		} catch (CoreException e) {
			// ignore
		}
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
					getScanner().setOffset(startPos);
					int token= getScanner().readNext(true);
					while (TokenScanner.isModifier(token)) {
						startPos= getScanner().getCurrentEndOffset();
						token= getScanner().readNext(true);
					}
				}
				if (startPos == methodDecl.getName().getStartPosition()) {
					rewriteNode(returnType, startPos, ""); //$NON-NLS-1$
					if (isInserted(returnType)) {
						doTextInsert(startPos, " ", getDescription(returnType)); //$NON-NLS-1$
					}
				} else {
					rewriteNode(returnType, startPos, " "); //$NON-NLS-1$
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	
		int pos= rewriteRequiredNode(methodDecl.getName());
		try {
			List parameters= methodDecl.parameters();
			if (hasChanges(parameters)) {
				pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLPAREN, pos);
				pos= getDefaultRewriter().rewriteList(parameters, pos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				pos= visitList(parameters, pos);
			}
			
			if (isModified(methodDecl)) {
				MethodDeclaration modifedNode= (MethodDeclaration) getModifiedNode(methodDecl);
				int oldDim= methodDecl.getExtraDimensions();
				int newDim= modifedNode.getExtraDimensions();
				if (oldDim != newDim) {
					int offset= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameRPAREN, pos);
					rewriteExtraDimensions(oldDim, newDim, offset, getDescription(methodDecl));
				}
			}				
			
			List exceptions= methodDecl.thrownExceptions();
			boolean hasExceptionChanges= hasChanges(exceptions);
			Block body= methodDecl.getBody();
			
			if (hasExceptionChanges || body != null && isInsertOrRemove(body)) {
				pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameRPAREN, pos);
				int dim= methodDecl.getExtraDimensions();
				while (dim > 0) {
					pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameRBRACKET, pos);
					dim--;
				}
			}				
			if (hasExceptionChanges) {
				pos= getDefaultRewriter().rewriteList(exceptions, pos, " throws ", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				pos= visitList(exceptions, pos);
			}
			if (body != null) {
				rewriteMethodBody(methodDecl, body, pos);
			}				
		} catch (CoreException e) {
			// ignore
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Block)
	 */
	public boolean visit(Block block) {
		List list=  block.statements();
		
		List collapsedChildren= fRewrite.getCollapsedNodes(block);
		if (collapsedChildren != null) {
			list= collapsedChildren; // not a real block, but a placeholder
		}

		int startPos= block.getStartPosition() + 1; // insert after left brace
		int startIndent= 0;
		if (!list.isEmpty() && isInserted((ASTNode) list.get(0))) { // calculate only when needed
			startIndent= getIndent(block.getStartPosition()) + 1;
		}
		ASTNode last= null; 
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem = (ASTNode) list.get(i);
			last= rewriteParagraph(elem, last, startPos, startIndent, 0, true);
		}
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
					int offset= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNamereturn, node.getStartPosition());
					rewriteNode(expression, offset, " "); //$NON-NLS-1$
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			} else {
				doVisit(expression);
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
		int startIndent= getIndent(node.getStartPosition()) + 1;
		rewriteParagraphList(declarations, startPos, startIndent, -1);
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
			
			Assert.isTrue(replacingType != null, "Cant remove array type in ArrayCreation"); //$NON-NLS-1$
			Type newType= replacingType.getElementType();
			Type oldType= arrayType.getElementType();
			if (!newType.equals(oldType)) {
				doTextRemove(oldType.getStartPosition(), oldType.getLength(), description);
				doTextInsert(oldType.getStartPosition(), newType, 0, false, description);
			}
			nNewBrackets= replacingType.getDimensions();
		}
		doVisit(arrayType);
		List dimExpressions= node.dimensions(); // dimension node with expressions
		try {
			// offset on first opening brace
			int offset= getScanner().getTokenStartOffset(ITerminalSymbols.TokenNameLBRACKET, arrayType.getStartPosition());
			for (int i= 0; i < dimExpressions.size(); i++) {
				ASTNode elem = (ASTNode) dimExpressions.get(i);
				if (isInserted(elem)) { // insert new dimension
					description= getDescription(elem);
					doTextInsert(offset, "[", description); //$NON-NLS-1$
					doTextInsert(offset, elem, 0, false, description);
					doTextInsert(offset, "]", description); //$NON-NLS-1$
					nNewBrackets--;
				} else {
					int elemEnd= elem.getStartPosition() + elem.getLength();
					int endPos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameRBRACKET, elemEnd);
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
						doVisit(elem);
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
				rewriteNode(initializer, pos, " "); //$NON-NLS-1$
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		List expressions= node.expressions();
		if (hasChanges(expressions)) {
			getDefaultRewriter().rewriteList(expressions, node.getStartPosition() + 1, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			visitList(expressions, node.getStartPosition() + 1);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		rewriteRequiredNode(node.getComponentType());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		int offset= rewriteRequiredNode(node.getExpression());
		if (node.getMessage() != null) { 
			rewriteNode(node.getMessage(), offset, " : "); //$NON-NLS-1$
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		Expression leftHand= node.getLeftHandSide();
		int pos= rewriteRequiredNode(leftHand);
		
		if (isModified(node)) {
			try {
				getScanner().readNext(pos, true);
				Assignment.Operator modifiedOp= ((Assignment) getModifiedNode(node)).getOperator();
				doTextReplace(getScanner().getCurrentStartOffset(), getScanner().getCurrentLength(), modifiedOp.toString(), getDescription(node));
			} catch (CoreException e) {
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
					int offset= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNamebreak, node.getStartPosition());
					rewriteNode(label, offset, " "); // space between break and label //$NON-NLS-1$
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			} else {
				doVisit(label);
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
				int startpos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLPAREN, pos);
				getDefaultRewriter().rewriteList(arguments, startpos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
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
			rewriteNode(decl, pos, " "); //$NON-NLS-1$
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
				int startpos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLPAREN, node.getStartPosition());
				getDefaultRewriter().rewriteList(arguments, startpos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
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
					int offset= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNamecontinue, node.getStartPosition());
					rewriteNode(label, offset, " "); // space between continue and label //$NON-NLS-1$
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			} else {
				doVisit(label);
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
		if (hasChanges(fragments)) {
			int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
			getDefaultRewriter().rewriteList(fragments, startPos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			visitList(fragments, 0);
		}
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
				int startOffset= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLPAREN, pos);
				pos= getDefaultRewriter().rewriteList(initializers, startOffset, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				pos= visitList(initializers, pos);
			}
			
			// position after first semicolon
			pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameSEMICOLON, pos);
			
			Expression expression= node.getExpression();
			if (expression != null) {
				rewriteNode(expression, pos, ""); //$NON-NLS-1$
			}
			
			List updaters= node.updaters();
			if (hasChanges(updaters)) {
				int startOffset= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameSEMICOLON, pos);
				getDefaultRewriter().rewriteList(updaters, startOffset, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				visitList(updaters, 0);
			}
		} catch (CoreException e) {
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
			rewriteNode(elseStatement, startPos, " else "); //$NON-NLS-1$
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
				doTextInsert(name.getStartPosition() + name.getLength(), ".*", getDescription(node)); //$NON-NLS-1$
			} else if (node.isOnDemand() && !modifiedNode.isOnDemand()) {
				try {
					int startPos= name.getStartPosition() + name.getLength();
					int endPos= getScanner().getTokenStartOffset(ITerminalSymbols.TokenNameSEMICOLON, startPos);
					doTextRemove(startPos, endPos - startPos, getDescription(node));
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return false;
	}
	
	private void replaceOperation(int posBeforeOperation, String newOperation, String description) {
		try {
			getScanner().readNext(posBeforeOperation, true);
			doTextReplace(getScanner().getCurrentStartOffset(), getScanner().getCurrentLength(), newOperation, description);
		} catch (CoreException e) {
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
				int startOffset= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLPAREN, pos);
				getDefaultRewriter().rewriteList(arguments, startOffset, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments, 0);
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
		rewriteRequiredNode(node.getQualifier());
		rewriteRequiredNode(node.getName());
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
		rewriteRequiredNode(node.getName());
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
			rewriteNode(node, pos, " = "); //$NON-NLS-1$
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
				pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLPAREN, pos);
				getDefaultRewriter().rewriteList(arguments, pos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments, 0);
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
				pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLPAREN, pos);
				getDefaultRewriter().rewriteList(arguments, pos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		} else {
			visitList(arguments, 0);
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
				pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameLBRACE, pos);
				
				int insertIndent= getIndent(node.getStartPosition()) + 1;
				ASTNode last= null; 
				for (int i= 0; i < statements.size(); i++) {
					ASTNode elem = (ASTNode) statements.get(i);
					// non-case statements are indented with one extra indent
					int currIndent= elem.getNodeType() == ASTNode.SWITCH_CASE ? insertIndent : insertIndent + 1;
					last= rewriteParagraph(elem, last, pos, currIndent, 0, false);
				}
			} catch (CoreException e) {
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
		if (hasChanges(catchBlocks)) {
			pos= getDefaultRewriter().rewriteList(catchBlocks, pos, " ", " "); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			pos= visitList(catchBlocks, pos);
		}
		Block finallyBlock= node.getFinally();
		if (finallyBlock != null) {
			rewriteNode(finallyBlock, pos, " finally "); //$NON-NLS-1$
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
		if (hasChanges(fragments)) {
			int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
			getDefaultRewriter().rewriteList(fragments, startPos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			visitList(fragments, 0);
		}
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
						for (int i= 0; i < dim; i++) {
							pos= getScanner().getTokenEndOffset(ITerminalSymbols.TokenNameRBRACKET, pos);
						}
					} catch (CoreException e) {
						JavaPlugin.log(e);
					}
				}
			} else {
				pos= node.getStartPosition() + node.getLength(); // insert pos
			}
			rewriteNode(initializer, pos, " = "); //$NON-NLS-1$
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
		if (hasChanges(fragments)) {
			int startPos= getNextExistingStartPos(fragments, 0, type.getStartPosition() + type.getLength());
			getDefaultRewriter().rewriteList(fragments, startPos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			visitList(fragments, 0);
		}
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
