/*******************************************************************************
 * Copyright (c) 2026 Daniel Schmid and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Daniel Schmid - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class MapOfToOfEntriesProposalCore extends LinkedCorrectionProposalCore {

	private final ASTNode selectedNode;
	private final List<Expression> arguments;

	public MapOfToOfEntriesProposalCore(String label, ICompilationUnit cu, ASTNode selectedNode, List<Expression> arguments, int relevance) {
		super(label, cu, null, relevance);
		this.selectedNode= Objects.requireNonNull(selectedNode);
		this.arguments = Objects.requireNonNull(arguments);
	}

	public boolean hasProposal() {
		return selectedNode instanceof MethodInvocation invocation && isMethodInvocationApplicable(invocation);
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		if (!(selectedNode instanceof MethodInvocation invocation) || !isMethodInvocationApplicable(invocation)) {
			return null;
		}

		AST ast= selectedNode.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		ListRewrite listRewriter= rewrite.getListRewrite(selectedNode, MethodInvocation.ARGUMENTS_PROPERTY);

		TextEditGroup desc= new TextEditGroup(getName());
		for (int i= 0; i < arguments.size() - 1; i+= 2) {
			Expression key= arguments.get(i);
			Expression value= arguments.get(i + 1);
			MethodInvocation entry= ast.newMethodInvocation();
			entry.setExpression((Expression) ASTNode.copySubtree(ast, invocation.getExpression()));
			entry.setName(ast.newSimpleName("entry")); //$NON-NLS-1$
			entry.arguments().add(ASTNode.copySubtree(ast, key));
			entry.arguments().add(ASTNode.copySubtree(ast, value));
			listRewriter.replace(value, entry, desc);
			listRewriter.remove(key, desc);
		}
		rewrite.replace(invocation.getName(), ast.newSimpleName("ofEntries"), desc); //$NON-NLS-1$

		return rewrite;
	}

	private boolean isMethodInvocationApplicable(MethodInvocation invocation) {
		if (!"of".equals(invocation.getName().getIdentifier())) { //$NON-NLS-1$
			return false;
		}
		int argCount= invocation.arguments().size();
		if (argCount <= 20 || argCount % 2 != 0) {
			return false;
		}
		ITypeBinding typeBinding= invocation.getExpression().resolveTypeBinding();
		return Map.class.getCanonicalName().equals(typeBinding.getBinaryName());
	}
}
