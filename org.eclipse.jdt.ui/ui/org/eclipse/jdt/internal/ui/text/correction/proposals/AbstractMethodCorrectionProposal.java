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
 *     Jerome Cambon <jerome.cambon@oracle.com> - [code style] don't generate redundant modifiers "public static final abstract" for interface members - https://bugs.eclipse.org/71627
 *     Microsoft Corporation - read preferences from the compilation unit
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

public abstract class AbstractMethodCorrectionProposal extends LinkedCorrectionProposal {

	public AbstractMethodCorrectionProposal(String label, ICompilationUnit targetCU, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
	}

	public AbstractMethodCorrectionProposal(String label, ICompilationUnit targetCU, int relevance, Image image, AbstractMethodCorrectionProposalCore core) {
		super(label, targetCU, null, relevance, image, core);
	}

	protected ASTNode getInvocationNode() {
		return ((AbstractMethodCorrectionProposalCore) getDelegate()).getInvocationNode();
	}

	/**
	 * @return The binding of the type declaration (generic type)
	 */
	protected ITypeBinding getSenderBinding() {
		return ((AbstractMethodCorrectionProposalCore) getDelegate()).getSenderBinding();
	}

	protected abstract boolean isConstructor();

	protected abstract void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers);

	protected abstract void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> params, ImportRewriteContext context) throws CoreException;

	protected abstract void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params, ImportRewriteContext context) throws CoreException;

	protected abstract void addNewExceptions(ASTRewrite rewrite, List<Type> exceptions, ImportRewriteContext context) throws CoreException;

	/**
	 * Add implementation in sub classes.
	 *
	 * @param rewrite The rewrite node
	 * @param decl The method declaration to add JavaDoc to
	 * @throws CoreException Might throw Exception
	 */
	protected void addNewJavaDoc(ASTRewrite rewrite, MethodDeclaration decl) throws CoreException {
		// no default action
	}

	protected abstract SimpleName getNewName(ASTRewrite rewrite);

	protected abstract Type getNewMethodType(ASTRewrite rewrite, ImportRewriteContext context) throws CoreException;
}
