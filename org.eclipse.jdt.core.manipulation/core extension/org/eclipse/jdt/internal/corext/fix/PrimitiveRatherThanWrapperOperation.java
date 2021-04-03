/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class PrimitiveRatherThanWrapperOperation extends CompilationUnitRewriteOperation {
	private final VariableDeclarationStatement node;
	private final String description;
	private final PrimitiveType.Code primitiveType;

	public PrimitiveRatherThanWrapperOperation(final VariableDeclarationStatement node, final String description, final PrimitiveType.Code primitiveType) {
		this.node= node;
		this.description= description;
		this.primitiveType= primitiveType;
	}

	@Override
	public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		TextEditGroup group= createTextEditGroup(description, cuRewrite);
		rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
			@Override
			public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
				if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
					return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
				}

				return super.computeSourceRange(nodeWithComment);
			}
		});

		Type newPrimitiveType= ast.newPrimitiveType(primitiveType);

		ASTNodes.replaceButKeepComment(rewrite, node.getType(), newPrimitiveType, group);
	}
}
