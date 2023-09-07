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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.OptionalCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

public class OptionalCorrectionProposalCore extends ASTRewriteCorrectionProposalCore {

	public static final String ADD_OPTIONAL_ID= "org.eclipse.jdt.ui.correction.addOptional"; //$NON-NLS-1$

	public static final int OPTIONAL_EMPTY= 0;

	public static final int OPTIONAL_OF= 1;

	public static final int OPTIONAL_OF_NULLABLE= 2;

	private final Expression fNodeToWrap;
	private int fCorrectionType;

	public OptionalCorrectionProposalCore(String label, ICompilationUnit targetCU, Expression nodeToWrap, int relevance, int correctionType) {
		super(label, targetCU, null, relevance);
		fNodeToWrap= nodeToWrap;
		fCorrectionType= correctionType;
		setCommandId(OptionalCorrectionProposalCore.ADD_OPTIONAL_ID);
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		AST ast= fNodeToWrap.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		MethodInvocation newMethodInvocation= ast.newMethodInvocation();
		newMethodInvocation.setExpression(ASTNodeFactory.newName(ast, "Optional")); //$NON-NLS-1$
		switch (fCorrectionType) {
			case OptionalCorrectionProposalCore.OPTIONAL_EMPTY:
				newMethodInvocation.setName(ast.newSimpleName("empty")); //$NON-NLS-1$
				break;
			case OptionalCorrectionProposalCore.OPTIONAL_OF:
				newMethodInvocation.setName(ast.newSimpleName("of")); //$NON-NLS-1$
				newMethodInvocation.arguments().add(rewrite.createCopyTarget(fNodeToWrap));
				break;
			case OptionalCorrectionProposalCore.OPTIONAL_OF_NULLABLE:
				newMethodInvocation.setName(ast.newSimpleName("ofNullable")); //$NON-NLS-1$
				newMethodInvocation.arguments().add(rewrite.createCopyTarget(fNodeToWrap));
				break;
			default:
				break;
		}
		rewrite.replace(fNodeToWrap, newMethodInvocation, null);
		return rewrite;
	}
}