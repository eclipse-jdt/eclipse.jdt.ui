/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.CodeAnalyzer;

public class SurroundWithTryCatchAnalyzer extends CodeAnalyzer {

	private ISurroundWithTryCatchQuery fQuery;
	private ITypeBinding[] fExceptions;
	private VariableDeclaration[] fLocals;

	public SurroundWithTryCatchAnalyzer(ICompilationUnit unit, Selection selection, ISurroundWithTryCatchQuery query) throws JavaModelException {
		super(unit, selection, false);
		fQuery= query;
	}
	
	public ITypeBinding[] getExceptions() {
		return fExceptions;
	}
	
	public VariableDeclaration[] getAffectedLocals() {
		return fLocals;
	}
	
	public BodyDeclaration getEnclosingBodyDeclaration() {
		ASTNode node= getFirstSelectedNode();
		if (node == null)
			return null;
		return (BodyDeclaration)ASTNodes.getParent(node, BodyDeclaration.class);
	}
	
	protected boolean handleSelectionEndsIn(ASTNode node) {
		return true;
	}
	
	public void endVisit(CompilationUnit node) {
		postProcessSelectedNodes(internalGetSelectedNodes());
		BodyDeclaration enclosingNode= null;
		superCall: {
			if (getStatus().hasFatalError())
				break superCall;
			if (!hasSelectedNodes()) {
				ASTNode coveringNode= getLastCoveringNode();
				if (coveringNode instanceof Block) {
					Block block= (Block)coveringNode;
					Message[] messages= ASTNodes.getMessages(block, ASTNodes.NODE_ONLY);
					if (messages.length > 0) {
						invalidSelection(RefactoringCoreMessages.getString("SurroundWithTryCatchAnalyzer.compile_errors"), //$NON-NLS-1$
							JavaStatusContext.create(getCompilationUnit(), block)); //$NON-NLS-1$
						break superCall;
					}
				}
				invalidSelection(RefactoringCoreMessages.getString("SurroundWithTryCatchAnalyzer.doesNotCover")); //$NON-NLS-1$
				break superCall;
			}
			enclosingNode= (BodyDeclaration)ASTNodes.getParent(getFirstSelectedNode(), BodyDeclaration.class);
			if (!(enclosingNode instanceof MethodDeclaration) && !(enclosingNode instanceof Initializer)) {
				invalidSelection(RefactoringCoreMessages.getString("SurroundWithTryCatchAnalyzer.doesNotContain"));  //$NON-NLS-1$
				break superCall;
			}
			if (!onlyStatements()) {
				invalidSelection(RefactoringCoreMessages.getString("SurroundWithTryCatchAnalyzer.onlyStatements")); //$NON-NLS-1$
			}
			fLocals= LocalDeclarationAnalyzer.perform(enclosingNode, getSelection());
		}
		super.endVisit(node);
		if (enclosingNode != null && !getStatus().hasFatalError()) {
			fExceptions= ExceptionAnalyzer.perform(enclosingNode, getSelection());
			if (fExceptions == null || fExceptions.length == 0) {
				if (fQuery == null) {
					invalidSelection(RefactoringCoreMessages.getString("SurroundWithTryCatchAnalyzer.noUncaughtExceptions")); //$NON-NLS-1$
				} else if (fQuery.catchRuntimeException()) {
					fExceptions= new ITypeBinding[] {node.getAST().resolveWellKnownType("java.lang.RuntimeException")}; //$NON-NLS-1$
				}
			}
		}
	}
	
	public void endVisit(SuperConstructorInvocation node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
			invalidSelection(RefactoringCoreMessages.getString("SurroundWithTryCatchAnalyzer.cannotHandleSuper"), JavaStatusContext.create(fCUnit, node)); //$NON-NLS-1$
		}
		super.endVisit(node);
	}
	
	public void endVisit(ConstructorInvocation node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
			invalidSelection(RefactoringCoreMessages.getString("SurroundWithTryCatchAnalyzer.cannotHandleThis"), JavaStatusContext.create(fCUnit, node)); //$NON-NLS-1$
		}
		super.endVisit(node);
	}
	
	protected void postProcessSelectedNodes(List selectedNodes) {
		if (selectedNodes == null || selectedNodes.size() == 0)
			return;
		if (selectedNodes.size() == 1) {
			ASTNode node= (ASTNode)selectedNodes.get(0);
			if (node instanceof Expression && node.getParent() instanceof ExpressionStatement) {
				selectedNodes.clear();
				selectedNodes.add(node.getParent());
			}
		}
	}
	
	private boolean onlyStatements() {
		ASTNode[] nodes= getSelectedNodes();
		for (int i= 0; i < nodes.length; i++) {
			if (!(nodes[i] instanceof Statement))
				return false;
		}
		return true;
	}	
}
