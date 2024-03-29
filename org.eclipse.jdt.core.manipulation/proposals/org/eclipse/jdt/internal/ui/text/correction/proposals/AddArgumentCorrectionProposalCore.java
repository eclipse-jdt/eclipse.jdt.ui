/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.AddArgumentCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.lang.reflect.Modifier;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;

public class AddArgumentCorrectionProposalCore extends LinkedCorrectionProposalCore {
	private int[] fInsertIndexes;
	private ITypeBinding[] fParamTypes;
	private ASTNode fCallerNode;

	public AddArgumentCorrectionProposalCore(String label, ICompilationUnit cu, ASTNode callerNode, int[] insertIdx, ITypeBinding[] expectedTypes, int relevance) {
		super(label, cu, null, relevance);
		fCallerNode= callerNode;
		fInsertIndexes= insertIdx;
		fParamTypes= expectedTypes;
	}
	@Override
	protected ASTRewrite getRewrite() {
		AST ast= fCallerNode.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ChildListPropertyDescriptor property= getProperty();

		for (int i= 0; i < fInsertIndexes.length; i++) {
			int idx= fInsertIndexes[i];
			String key= "newarg_" + i; //$NON-NLS-1$
			Expression newArg= evaluateArgumentExpressions(ast, fParamTypes[idx], key);
			ListRewrite listRewriter= rewrite.getListRewrite(fCallerNode, property);
			listRewriter.insertAt(newArg, idx, null);

			addLinkedPosition(rewrite.track(newArg), i == 0, key);
		}
		return rewrite;
	}

	private ChildListPropertyDescriptor getProperty() {
		List<StructuralPropertyDescriptor> list= fCallerNode.structuralPropertiesForType();
		for (StructuralPropertyDescriptor curr : list) {
			if (curr.isChildListProperty() && "arguments".equals(curr.getId())) { //$NON-NLS-1$
				return (ChildListPropertyDescriptor) curr;
			}
		}
		return null;

	}


	private Expression evaluateArgumentExpressions(AST ast, ITypeBinding requiredType, String key) {
		CompilationUnit root= (CompilationUnit) fCallerNode.getRoot();

		int offset= fCallerNode.getStartPosition();
		Expression best= null;
		ITypeBinding bestType= null;

		ScopeAnalyzer analyzer= new ScopeAnalyzer(root);
		for (IBinding binding : analyzer.getDeclarationsInScope(offset, ScopeAnalyzer.VARIABLES)) {
			IVariableBinding curr= (IVariableBinding) binding;
			ITypeBinding type= curr.getType();
			if (type != null && canAssign(type, requiredType) && testModifier(curr)) {
				if (best == null || isMoreSpecific(bestType, type)) {
					best= ast.newSimpleName(curr.getName());
					bestType= type;
				}
				addLinkedPositionProposal(key, curr.getName());
			}
		}
		Expression defaultExpression= ASTNodeFactory.newDefaultExpression(ast, requiredType);
		if (best == null) {
			best= defaultExpression;
		}
		addLinkedPositionProposal(key, ASTNodes.asString(defaultExpression));
		return best;
	}

	private boolean isMoreSpecific(ITypeBinding best, ITypeBinding curr) {
		return (canAssign(best, curr) && !canAssign(curr, best));
	}


	private boolean canAssign(ITypeBinding curr, ITypeBinding best) {
		return curr.isAssignmentCompatible(best);
	}

	private boolean testModifier(IVariableBinding curr) {
		int modifiers= curr.getModifiers();
		int staticFinal= Modifier.STATIC | Modifier.FINAL;
		if ((modifiers & staticFinal) == staticFinal) {
			return false;
		}
		if (Modifier.isStatic(modifiers) && !ASTResolving.isInStaticContext(fCallerNode)) {
			return false;
		}
		return true;
	}
}