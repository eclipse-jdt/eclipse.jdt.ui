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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

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

	public static ASTNode getPlaceholderForExisting(ASTNode node) {
		return ASTWithExistingFlattener.getPlaceholder(node);
	}

	public static void markAsInserted(ASTNode node) {
		node.setSourceRange(-1, 0);
	}
	
	public static void markAsReplaced(ASTNode node, ASTNode modifiedNode) {
		ASTReplace replace= new ASTReplace();
		replace.modifiedNode= modifiedNode;
		node.setProperty(KEY, replace);
	}
	
	public static void markFlagsChanged(ASTNode node, int changedModifiers, boolean invertType) {
		ASTFlagsModification modifiedFlags= new ASTFlagsModification();
		modifiedFlags.changedModifiers= changedModifiers;
		modifiedFlags.invertType= invertType;
		node.setProperty(KEY, modifiedFlags);
	}	
	
	/* package */ static boolean isModifiedNode(ASTNode node) {
		return isInserted(node) || isReplaced(node);
	}
	
	/* package */ static boolean isInserted(ASTNode node) {
		return node.getStartPosition() == -1;
	}
	
	/* package */ static boolean isReplaced(ASTNode node) {
		return node.getProperty(KEY) instanceof ASTReplace;
	}
	
	/* package */ static ASTFlagsModification getModifiedFlags(ASTNode node) {
		Object info= node.getProperty(KEY);
		if (info instanceof ASTFlagsModification) {
			return (ASTFlagsModification) info;
		}
		return null;
	}

	/* package */ static ASTNode getReplacingNode(ASTNode node) {
		Object info= node.getProperty(KEY);
		if (info instanceof ASTReplace) {
			return ((ASTReplace) info).modifiedNode;
		}
		return null;
	}
		
	private static final class ASTReplace {
		public ASTNode modifiedNode;
	}
	
	/* package */ static final class ASTFlagsModification {
		public int changedModifiers;
		public boolean invertType;
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
				replaceNode(expression, getReplacingNode(expression));
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
				str= str + ' ';
			}
			if (pos > 0 && Character.isLetterOrDigit(fTextBuffer.getChar(pos - 1))) {
				str= ' ' + str;
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
			fChange.addTextEdit("Remove Node", SimpleTextEdit.createDelete(startPos, endPos - startPos));
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
		int startPos= block.getStartPosition() + 1; // insert after left brace
		int startIndent= 0;
		if (!list.isEmpty() && isInserted((ASTNode) list.get(0))) { // calculate only when needed
			startIndent= getIndent(block.getStartPosition()) + 1;
		}
		rewriteParagraphList(list, startPos, startIndent);
		return false;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration methodDecl) {
		boolean invertKind= false; // from method to constructor or back
		ASTFlagsModification modfiedFlags= getModifiedFlags(methodDecl);
		if (modfiedFlags != null) {
			rewriteModifiers(methodDecl.getStartPosition(), methodDecl.getModifiers(), modfiedFlags.changedModifiers);
			invertKind= modfiedFlags.invertType;
		}
		
		boolean willBeConstructor= methodDecl.isConstructor() != invertKind;
		
		Type returnType= methodDecl.getReturnType();
		if (isReplaced(returnType)) {
			replaceNode(returnType, getReplacingNode(returnType));
		} else if (isInserted(returnType)) {
			if (!willBeConstructor) {
				insertNode(returnType, methodDecl.getStartPosition(), MODIFIERS);
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
				IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), offset);
				
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

	private void rewriteMethodBody(MethodDeclaration methodDecl, Block body) {
		if (isModifiedNode(body)) {
			try {
				IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), methodDecl.getStartPosition());
				if (isInserted(body)) {
					ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameSEMICOLON);
					int startPos= scanner.getCurrentTokenStartPosition();
					int endPos= methodDecl.getStartPosition() + methodDecl.getLength();					
					String str= " " + Strings.trimLeadingTabsAndSpaces(generateSource(body, getIndent(methodDecl.getStartPosition())));
					fChange.addTextEdit("Insert body", SimpleTextEdit.createReplace(startPos, endPos - startPos, str));
				} else if (isReplaced(body)) {
					ASTNode changed= getReplacingNode(body);
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
	
	private boolean isInsertFirst(List list) {
		return !list.isEmpty() && isInserted((ASTNode) list.get(0));
	}
	
	private boolean hasChanges(List list) {
		for (int i= 0; i < list.size(); i++) {
			ASTNode elem= (ASTNode) list.get(i);
			if (isModifiedNode(elem)) {
				return true;
			}
		}
		return false;
	}
	
	private void rewriteList(int startPos, String keyword, List list, boolean updateKeyword) {
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
				if (!isReplaced(elem) || getReplacingNode(elem) != null) {
					after++;
				}
				lastExisting= elem;
			}
		}
		
		if (after == 0) {
			if (before != 0) { // deleting the list
				int endPos= lastExisting.getStartPosition() + lastExisting.getLength();
				fChange.addTextEdit("Remove all", SimpleTextEdit.createDelete(startPos, endPos - startPos));
			}
			return;
		}
		
		if (before == 0) { // creating a new list -> insert keyword first (e.g. " throws ")
			fChange.addTextEdit("keyword", SimpleTextEdit.createInsert(startPos, keyword));
		} else if (updateKeyword) {
			int firstStart= getNextExistingStartPos(list, 0, startPos);
			fChange.addTextEdit("Update keyword", SimpleTextEdit.createReplace(startPos, firstStart - startPos, keyword));
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
					ASTNode changed= getReplacingNode(elem);
					if (changed == null) {
						fChange.addTextEdit("Remove", SimpleTextEdit.createDelete(currPos, nextStart - currPos));
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
						fChange.addTextEdit("Remove comma", SimpleTextEdit.createDelete(currEnd, nextStart - currEnd));
					} else if (after != 0 && before == 0) { // was last, but not anymore -> add comma
						fChange.addTextEdit("Add comma", SimpleTextEdit.createInsert(currEnd, ", "));
					}					
				}
				currPos= nextStart;
			}
		}
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
			
			fChange.addTextEdit("Remove", SimpleTextEdit.createDelete(start, end - start));
		} else {
			int startLine= fTextBuffer.getLineOfOffset(elem.getStartPosition());
			int indent= fTextBuffer.getLineIndent(startLine, CodeFormatterUtil.getTabWidth());
			String str= Strings.trimLeadingTabsAndSpaces(generateSource(changed, indent));
			fChange.addTextEdit("Replace", SimpleTextEdit.createReplace(elem.getStartPosition(), elem.getLength(), str));
		}
	}

	private void insertParagraph(ASTNode elem, ASTNode sibling, int insertPos, int indent, boolean additionalNewLine) {
		if (sibling != null) {
			indent= getIndent(sibling.getStartPosition());
			insertPos= sibling.getStartPosition() + sibling.getLength();
		}

		StringBuffer buf= new StringBuffer();
		buf.append(fTextBuffer.getLineDelimiter());
		buf.append(generateSource(elem, indent));
		if (additionalNewLine) {
			buf.append(fTextBuffer.getLineDelimiter());
		}
		
		fChange.addTextEdit("Add", SimpleTextEdit.createInsert(insertPos, buf.toString()));
	}

	private int getIndent(int pos) {
		int line= fTextBuffer.getLineOfOffset(pos);
		return fTextBuffer.getLineIndent(line, CodeFormatterUtil.getTabWidth());
	}

	
	private String generateSource(ASTNode node, int indent) {
		return  (new ASTWithExistingFlattener()).generateFormatted(node, fTextBuffer, indent);
	}
	

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration typeDecl) {
		
		// modifiers & class/interface
		boolean invertType= false;
		ASTFlagsModification modifiedFlags= getModifiedFlags(typeDecl);
		if (modifiedFlags != null) {
			rewriteModifiers(typeDecl.getStartPosition(), typeDecl.getModifiers(), modifiedFlags.changedModifiers);
			if (modifiedFlags.invertType) { // change from class to interface or reverse
				invertType= true;
				try {
					IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), typeDecl.getStartPosition());
					int typeToken= typeDecl.isInterface() ? ITerminalSymbols.TokenNameinterface : ITerminalSymbols.TokenNameclass;
					ASTResolving.readToToken(scanner, typeToken);
					
					String str= typeDecl.isInterface() ? "class" : "interface";
					int start= scanner.getCurrentTokenStartPosition();
					int end= scanner.getCurrentTokenEndPosition() + 1;
					
					fChange.addTextEdit("Invert Type", SimpleTextEdit.createReplace(start, end - start, str));
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
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
				fChange.addTextEdit("Insert Supertype", SimpleTextEdit.createInsert(pos, str));
			} else if (isReplaced(superClass)) {
				ASTNode changed= getReplacingNode(superClass);
				if (changed == null) {
					int startPos= simpleName.getStartPosition() + simpleName.getLength();
					int endPos= superClass.getStartPosition() + superClass.getLength();
					fChange.addTextEdit("Remove Supertype", SimpleTextEdit.createDelete(startPos, endPos - startPos));
				} else {
					String str= ASTNodes.asString(changed);
					fChange.addTextEdit("Replace Supertype", SimpleTextEdit.createReplace(superClass.getStartPosition(), superClass.getLength(), str));
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
				IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), pos);
				ASTResolving.readToToken(scanner, ITerminalSymbols.TokenNameLBRACE);		
				
				startPos= scanner.getCurrentTokenEndPosition() + 1;
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			} catch (InvalidInputException e) {
				// ignore
			}
		}
		rewriteParagraphList(members, startPos, startIndent);
		return false;
	}

	private void rewriteModifiers(int startPos, int oldModifiers, int newModifiers) {
		if (oldModifiers == newModifiers) {
			return;
		}
		
		try {
			IScanner scanner= ASTResolving.createScanner(fChange.getCompilationUnit(), startPos);
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
					fChange.addTextEdit("Remove Modifier", SimpleTextEdit.createDelete(currPos, endPos - currPos));
				}
			} 
			int addedModifiers= (newModifiers ^ oldModifiers) & newModifiers;
			if (addedModifiers != 0) {
				StringBuffer buf= new StringBuffer();
				ASTFlattener.printModifiers(addedModifiers, buf);
				fChange.addTextEdit("Add Modifier", SimpleTextEdit.createInsert(endPos, buf.toString()));
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
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

}
