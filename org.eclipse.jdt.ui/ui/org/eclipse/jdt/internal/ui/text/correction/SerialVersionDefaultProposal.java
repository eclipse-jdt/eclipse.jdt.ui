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
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Proposal for a default serial version id.
 */
public class SerialVersionDefaultProposal extends LinkedCorrectionProposal {

	/** The initializer linked position group id */
	protected static final String GROUP_INITIALIZER= "initializer"; //$NON-NLS-1$

	/** The long literal suffix */
	public static final String LONG_SUFFIX= "L"; //$NON-NLS-1$

	/** The name of the serial version field */
	public static final String NAME_FIELD= "serialVersionUID"; //$NON-NLS-1$

	/** The proposal relevance */
	public static final int PROPOSAL_RELEVANCE= 9;

	/** The default serial id value */
	public static final int SERIAL_VALUE= 1;

	/** The default serial id expression */
	public static final String DEFAULT_EXPRESSION= SERIAL_VALUE + LONG_SUFFIX; //$NON-NLS-1$

	/** The originally selected node */
	protected final ASTNode fNode;

	/**
	 * Creates a new serial version default proposal.
	 * 
	 * @param label
	 *        the label of this proposal
	 * @param unit
	 *        the compilation unit
	 * @param node
	 *        the originally selected node
	 */
	public SerialVersionDefaultProposal(final String label, final ICompilationUnit unit, final ASTNode node) {
		super(label, unit, null, PROPOSAL_RELEVANCE, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD));

		Assert.isNotNull(node);

		fNode= node;
	}

	/**
	 * Adds the necessary linked positions for the specified fragment.
	 * 
	 * @param rewrite
	 *        the ast rewrite to operate on
	 * @param fragment
	 *        the fragment to add linked positions to
	 */
	protected void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment) {

		Assert.isNotNull(rewrite);
		Assert.isNotNull(fragment);

		final Expression initializer= fragment.getInitializer();
		if (initializer != null)
			addLinkedPosition(rewrite.track(initializer), true, GROUP_INITIALIZER);
	}

	/**
	 * Computes the default expression to initialize the serial version id with.
	 * 
	 * @return the default expression for the serial version id
	 */
	protected Expression computeDefaultExpression() {
		return fNode.getAST().newNumberLiteral(DEFAULT_EXPRESSION);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return CorrectionMessages.getString("SerialVersionDefaultProposal.message.default.info"); //$NON-NLS-1$
	}

	/**
	 * Returns the declaration node for the originally selected node.
	 * 
	 * @return the declaration node
	 */
	protected final ASTNode getDeclarationNode() {

		ASTNode parent= fNode.getParent();
		if (!(parent instanceof TypeDeclaration)) {

			parent= parent.getParent();
			if (parent instanceof ClassInstanceCreation) {

				final ClassInstanceCreation creation= (ClassInstanceCreation) parent;
				parent= creation.getAnonymousClassDeclaration();
			}
		}
		return parent;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected final ASTRewrite getRewrite() throws CoreException {

		final ASTNode declarations= getDeclarationNode();

		final AST ast= declarations.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);

		final VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(NAME_FIELD));

		final FieldDeclaration declaration= ast.newFieldDeclaration(fragment);
		declaration.setType(ast.newPrimitiveType(PrimitiveType.LONG));
		declaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL));

		fragment.setInitializer(computeDefaultExpression());

		final ChildListPropertyDescriptor descriptor= (declarations.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION) ? AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY : TypeDeclaration.BODY_DECLARATIONS_PROPERTY;
		rewrite.getListRewrite(declarations, descriptor).insertAt(declaration, 0, null);

		addLinkedPositions(rewrite, fragment);

		return rewrite;
	}
}
