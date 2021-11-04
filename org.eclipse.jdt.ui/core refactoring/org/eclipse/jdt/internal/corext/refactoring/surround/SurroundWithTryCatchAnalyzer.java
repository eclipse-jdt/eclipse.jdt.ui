/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.SurroundWithAnalyzer;

public class SurroundWithTryCatchAnalyzer extends SurroundWithAnalyzer {
	private ITypeBinding[] fExceptions;

	public SurroundWithTryCatchAnalyzer(ICompilationUnit unit, Selection selection) throws CoreException {
		super(unit, selection, true);
	}

	public ITypeBinding[] getExceptions() {
		return fExceptions;
	}

	@Override
	public void endVisit(CompilationUnit node) {
		ASTNode enclosingNode= null;
		if (!getStatus().hasFatalError() && hasSelectedNodes())
			enclosingNode= SurroundWithAnalyzer.getEnclosingNode(getFirstSelectedNode());

		super.endVisit(node);
		if (enclosingNode != null && !getStatus().hasFatalError()) {
			fExceptions= ExceptionAnalyzer.perform(enclosingNode, getSelection(), false);
			if (fExceptions == null || fExceptions.length == 0) {
				if (enclosingNode instanceof MethodReference) {
					invalidSelection(RefactoringCoreMessages.SurroundWithTryCatchAnalyzer_doesNotContain);
				} else {
					fExceptions= new ITypeBinding[] { node.getAST().resolveWellKnownType("java.lang.Exception") }; //$NON-NLS-1$
				}
			}
		}
	}

	public Map<SimpleName, IVariableBinding> getVariableStatementBinding(ASTNode astNode) {
		Map<SimpleName, IVariableBinding> variableBindings= new HashMap<>();
		astNode.accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationStatement node) {
				for (Object o : node.fragments()) {
					if (o instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment vdf= (VariableDeclarationFragment) o;
						SimpleName name= vdf.getName();
						IBinding binding= name.resolveBinding();
						if (binding instanceof IVariableBinding) {
							variableBindings.put(name, (IVariableBinding) binding);
							break;
						}
					}
				}
				return false;
			}

			@Override
			public boolean visit(PatternInstanceofExpression node) {
				SingleVariableDeclaration svd= node.getRightOperand();
				SimpleName name= svd.getName();
				IBinding binding= name.resolveBinding();
				if (binding instanceof IVariableBinding) {
					variableBindings.put(name, (IVariableBinding) binding);
				}
				return false;
			}
		});
		return variableBindings;
	}

}
