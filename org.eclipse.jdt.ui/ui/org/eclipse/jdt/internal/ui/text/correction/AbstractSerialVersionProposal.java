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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Partial implementation of a serial version correction proposal.
 * 
 * @since 3.1
 */
public abstract class AbstractSerialVersionProposal extends LinkedCorrectionProposal {

	/** The long literal suffix */
	protected static final String LONG_SUFFIX= "L"; //$NON-NLS-1$

	/** The default serial value */
	protected static final long SERIAL_VALUE= 1;

	/** The default serial id expression */
	protected static final String DEFAULT_EXPRESSION= SERIAL_VALUE + LONG_SUFFIX; //$NON-NLS-1$

	/** The name of the serial version field */
	protected static final String NAME_FIELD= "serialVersionUID"; //$NON-NLS-1$

	/** The proposal relevance */
	private static final int PROPOSAL_RELEVANCE= 9;

	/** The originally selected node */
	private final ASTNode fNode;

	/**
	 * Creates a new abstract serial version proposal.
	 * 
	 * @param label the label of this proposal
	 * @param unit the compilation unit
	 * @param node the originally selected node
	 */
	protected AbstractSerialVersionProposal(final String label, final ICompilationUnit unit, final ASTNode node) {
		super(label, unit, null, PROPOSAL_RELEVANCE, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD));

		Assert.isNotNull(node);

		fNode= node;
	}

	/**
	 * Adds an initializer to the specified variable declaration fragment.
	 * 
	 * @param fragment the variable declaration fragment to add an initializer
	 */
	protected abstract void addInitializer(final VariableDeclarationFragment fragment);

	/**
	 * Adds the necessary linked positions for the specified fragment.
	 * 
	 * @param rewrite the ast rewrite to operate on
	 * @param fragment the fragment to add linked positions to
	 */
	protected abstract void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment);

	/**
	 * Computes the default expression to initialize the serial version id with.
	 * 
	 * @param monitor the progress monitor to use
	 * 
	 * @return the default expression for the serial version id
	 */
	protected abstract Expression computeDefaultExpression(final IProgressMonitor monitor);

	/**
	 * Returns the AST to operate on.
	 * 
	 * @return the AST to operate on
	 */
	protected final AST getAST() {
		return fNode.getAST();
	}

	/**
	 * Returns the declaration node for the originally selected node.
	 * 
	 * @return the declaration node
	 */
	protected final ASTNode getDeclarationNode() {

		ASTNode parent= fNode.getParent();
		if (!(parent instanceof AbstractTypeDeclaration)) {

			parent= parent.getParent();
			if (parent instanceof ParameterizedType || parent instanceof Type)
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

		final ASTNode node= getDeclarationNode();

		final AST ast= node.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);

		final VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();

		fragment.setName(ast.newSimpleName(NAME_FIELD));

		final FieldDeclaration declaration= ast.newFieldDeclaration(fragment);
		declaration.setType(ast.newPrimitiveType(PrimitiveType.LONG));
		declaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL));

		addInitializer(fragment);

		if (fragment.getInitializer() != null) {

			if (node instanceof AbstractTypeDeclaration)
				rewrite.getListRewrite(node, ((AbstractTypeDeclaration) node).getBodyDeclarationsProperty()).insertAt(declaration, 0, null);
			else if (node instanceof AnonymousClassDeclaration)
				rewrite.getListRewrite(node, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(declaration, 0, null);
			else
				Assert.isTrue(false);

			addLinkedPositions(rewrite, fragment);
		}

		final String comment= CodeGeneration.getFieldComment(getCompilationUnit(), declaration.getType().toString(), NAME_FIELD, StubUtility.getLineDelimiterUsed(getCompilationUnit()));
		if (comment != null && comment.length() > 0) {
			final Javadoc doc= (Javadoc) rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
			declaration.setJavadoc(doc);
		}
		return rewrite;
	}
}
