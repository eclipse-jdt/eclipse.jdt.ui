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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

/**
 * A special text selection that gives access to the resloved and
 * enclosing element.
 */
public class JavaTextSelection extends TextSelection {

	private IJavaElement fElement;
	private IJavaElement[] fResolvedElements;
	
	private boolean fEnclosingElementRequested;
	private IJavaElement fEnclosingElement;
	
	private boolean fPartialASTRequested;
	private CompilationUnit fPartialAST;
	
	private boolean fNodesRequested;
	private ASTNode[] fSelectedNodes;
	private ASTNode fCoveringNode;
	
	private boolean fInMethodBodyRequested;
	private boolean fInMethodBody;
	
	private boolean fInInitializeRequested;
	private boolean fInInitializer;
	
	/**
	 * Creates a new text selection at the given offset and length.
	 */
	public JavaTextSelection(IJavaElement element, IDocument document, int offset, int length) {
		super(document, offset, length);
		fElement= element;
	}
	
	/**
	 * Resolves the <code>IJavaElement</code>s at the current offset. Returns
	 * an empty array if the string under the offset doesn't resolve to a
	 * <code>IJavaElement</code>.
	 * 
	 * @return the resolved java elements at the current offset
	 * @throws JavaModelException passed from the underlying code resolve API 
	 */
	public IJavaElement[] resolveElementAtOffset() throws JavaModelException {
		if (fResolvedElements != null)
			return fResolvedElements;
		// long start= System.currentTimeMillis();
		fResolvedElements= SelectionConverter.codeResolve(fElement, this);
		// System.out.println("Time resolving element: " + (System.currentTimeMillis() - start));
		return fResolvedElements;
	}
	
	public IJavaElement resolveEnclosingElement() throws JavaModelException {
		if (fEnclosingElementRequested)
			return fEnclosingElement;
		fEnclosingElementRequested= true;
		fEnclosingElement= SelectionConverter.resolveEnclosingElement(fElement, this);
		return fEnclosingElement;
	}
	
	public CompilationUnit resolvePartialAstAtOffset() {
		if (fPartialASTRequested)
			return fPartialAST;
		fPartialASTRequested= true;
		if (! (fElement instanceof ICompilationUnit))
			return null;
		// long start= System.currentTimeMillis();
		fPartialAST= AST.parsePartialCompilationUnit((ICompilationUnit)fElement, getOffset(), true, null, null);
		// System.out.println("Time requesting partial AST: " + (System.currentTimeMillis() - start));
		return fPartialAST;
	}
	
	public ASTNode[] resolveSelectedNodes() {
		if (fNodesRequested)
			return fSelectedNodes;
		fNodesRequested= true;
		CompilationUnit root= resolvePartialAstAtOffset();
		if (root == null)
			return null;
		Selection ds= Selection.createFromStartLength(getOffset(), getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(ds, false);
		root.accept(analyzer);
		fSelectedNodes= analyzer.getSelectedNodes();
		fCoveringNode= analyzer.getLastCoveringNode();
		return fSelectedNodes;
	}
	
	public ASTNode resolveCoveringNode() {
		if (fNodesRequested)
			return fCoveringNode;
		resolveSelectedNodes();
		return fCoveringNode;
	}
	
	public boolean resolveInMethodBody() {
		if (fInMethodBodyRequested)
			return fInMethodBody;
		fInMethodBodyRequested= true;
		resolveSelectedNodes();
		ASTNode node= getStartNode();
		if (node == null) {
			fInMethodBody= true;
		} else {
			while (node != null) {
				int nodeType= node.getNodeType();
				if (nodeType == ASTNode.BLOCK && node.getParent() instanceof BodyDeclaration) {
					fInMethodBody= node.getParent().getNodeType() == ASTNode.METHOD_DECLARATION;
					break;
				} else if (nodeType == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
					fInMethodBody= false;
					break;
				}
				node= node.getParent();
			}
		}
		return fInMethodBody;
	}
	
	public boolean resolveInInitializer() {
		if (fInInitializeRequested)
			return fInInitializer;
		fInInitializeRequested= true;
		resolveSelectedNodes();
		ASTNode node= getStartNode();
		if (node == null) {
			fInInitializer= true;
		} else {
			while (node != null) {
				int nodeType= node.getNodeType();
				if (nodeType == ASTNode.BLOCK && node.getParent() instanceof BodyDeclaration) {
					fInInitializer= node.getParent().getNodeType() == ASTNode.INITIALIZER;
					break;
				} else if (nodeType == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
					fInInitializer= false;
					break;
				}
				node= node.getParent();
			}
		}
		return fInInitializer;
	}
	
	private ASTNode getStartNode() {
		if (fSelectedNodes != null && fSelectedNodes.length > 0)
			return fSelectedNodes[0];
		else
			return fCoveringNode;
	}
}
