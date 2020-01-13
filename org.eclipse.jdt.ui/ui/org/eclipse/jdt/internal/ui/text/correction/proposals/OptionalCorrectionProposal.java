/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class OptionalCorrectionProposal extends ASTRewriteCorrectionProposal {

	public static final String ADD_OPTIONAL_ID= "org.eclipse.jdt.ui.correction.addOptional"; //$NON-NLS-1$

	public static final int OPTIONAL_EMPTY= 0;

	public static final int OPTIONAL_OF= 1;

	public static final int OPTIONAL_OF_NULLABLE= 2;

	private final Expression fNodeToWrap;

	private int fCorrectionType;

	/**
	 * Creates a 'wrap in optional' correction proposal.
	 *
	 * @param label the display name of the proposal
	 * @param targetCU the compilation unit that is modified
	 * @param nodeToWrap the node to wrap in Optional
	 * @param relevance the relevance of this proposal
	 * @param correctionType 0= Optional.empty(), 1= Optional.of(), 2= Optional.ofNullable()
	 */
	public OptionalCorrectionProposal(String label, ICompilationUnit targetCU, Expression nodeToWrap, int relevance, int correctionType) {
		super(label, targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST));
		fNodeToWrap= nodeToWrap;
		fCorrectionType= correctionType;
		setCommandId(ADD_OPTIONAL_ID);
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		AST ast= fNodeToWrap.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		MethodInvocation newMethodInvocation= ast.newMethodInvocation();
		newMethodInvocation.setExpression(ASTNodeFactory.newName(ast, "Optional")); //$NON-NLS-1$
		switch (fCorrectionType) {
			case OPTIONAL_EMPTY:
				newMethodInvocation.setName(ast.newSimpleName("empty")); //$NON-NLS-1$
				break;
			case OPTIONAL_OF:
				newMethodInvocation.setName(ast.newSimpleName("of")); //$NON-NLS-1$
				newMethodInvocation.arguments().add(rewrite.createCopyTarget(fNodeToWrap));
				break;
			case OPTIONAL_OF_NULLABLE:
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
