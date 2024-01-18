/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Benjamin Muskalla - [quick fix] Create Method in void context should 'box' void. - https://bugs.eclipse.org/bugs/show_bug.cgi?id=107985
 *     Jens Reimann <jreimann@redhat.com> Bug 38201: [quick assist] Allow creating abstract method - https://bugs.eclipse.org/38201
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

public class NewMethodCorrectionProposal extends AbstractMethodCorrectionProposal {
	public NewMethodCorrectionProposal(NewMethodCorrectionProposalCore core, Image image) {
		super(core.getName(), core.getCompilationUnit(), core.getRelevance(), image, core);
	}

	public NewMethodCorrectionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, List<Expression> arguments, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, relevance, image, new NewMethodCorrectionProposalCore(label, targetCU, invocationNode, arguments, binding, relevance));
	}

	public NewMethodCorrectionProposal(String label, ICompilationUnit targetCU, int relevance, Image image, NewMethodCorrectionProposalCore delegate) {
		super(label, targetCU, relevance, image, delegate);
	}

	@Override
	protected boolean isConstructor() {
		return ((NewMethodCorrectionProposalCore) getDelegate()).isConstructor();
	}

	@Override
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers) {
		((NewMethodCorrectionProposalCore) getDelegate()).addNewModifiers(rewrite, targetTypeDecl, modifiers);
	}

	@Override
	protected void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> params, ImportRewriteContext context) throws CoreException {
		((NewMethodCorrectionProposalCore) getDelegate()).addNewTypeParameters(rewrite, takenNames, params, context);
	}

	@Override
	protected void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params, ImportRewriteContext context) throws CoreException {
		((NewMethodCorrectionProposalCore) getDelegate()).addNewParameters(rewrite, takenNames, params, context);
	}

	@Override
	protected void addNewExceptions(ASTRewrite rewrite, List<Type> exceptions, ImportRewriteContext context) throws CoreException {
		((NewMethodCorrectionProposalCore) getDelegate()).addNewExceptions(rewrite, exceptions, context);
	}

	@Override
	protected SimpleName getNewName(ASTRewrite rewrite) {
		return ((NewMethodCorrectionProposalCore) getDelegate()).getNewName(rewrite);
	}

	@Override
	protected Type getNewMethodType(ASTRewrite rewrite, ImportRewriteContext context) throws CoreException {
		return ((NewMethodCorrectionProposalCore) getDelegate()).getNewMethodType(rewrite, context);
	}

}
