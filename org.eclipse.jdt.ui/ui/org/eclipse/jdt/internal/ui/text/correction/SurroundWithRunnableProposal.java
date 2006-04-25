/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

public class SurroundWithRunnableProposal extends LinkedCorrectionProposal {
	
	private static class SurroundWithRunnable extends SurroundWith {
		
		private static final String PROPOSED_RUNNABLE_VAR_NAME= "runnable"; //$NON-NLS-1$
		private static final String TYPE_NAME= "Runnable"; //$NON-NLS-1$
		private static final String METHOD_NAME= "run"; //$NON-NLS-1$
		
		private ITrackedNodePosition fNamePosition;
		private ITrackedNodePosition fEndPosition;

		/**
		 * Set up a <code>SurroundWithRunnableProposal</code> which generates a @see <code>Runnable</code> arround
		 * <code>selectedNodes</code>
		 * @param context The context in which the proposal is applyed.
		 * @param selectedNodes The selected nodes to enclose with a Runnable. Returned by <code>isValidSelection</code>.
		 */
		public SurroundWithRunnable(IInvocationContext context, Statement[] selectedNodes) {
			super(context.getASTRoot(), selectedNodes);
		}
		
		/**
		 * Generate a new code skeleton.
		 * @param newBody The new body which will be filled with code.
		 * @param rewrite The rewrite to use to change the ast.
		 * @return The root of the new code.
		 */
		protected Statement generateCodeSkeleton(Block newBody, ASTRewrite rewrite) {
			AST ast= getAst();
			
			MethodDeclaration runMethod= ast.newMethodDeclaration();
			runMethod.setName(ast.newSimpleName(METHOD_NAME));
			runMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			runMethod.setBody(newBody);
			
			AnonymousClassDeclaration runnableClassDeclaration= ast.newAnonymousClassDeclaration();
			runnableClassDeclaration.bodyDeclarations().add(runMethod);
			
			ClassInstanceCreation runnableInstanziator= ast.newClassInstanceCreation();
			runnableInstanziator.setType(ast.newSimpleType(ast.newName(TYPE_NAME)));
			runnableInstanziator.setAnonymousClassDeclaration(runnableClassDeclaration);
			
			VariableDeclarationFragment variableDeclarationFragment= ast.newVariableDeclarationFragment();
			SimpleName variableName= ast.newSimpleName(PROPOSED_RUNNABLE_VAR_NAME);
			variableDeclarationFragment.setName(variableName);
			variableDeclarationFragment.setInitializer(runnableInstanziator);
			fNamePosition= rewrite.track(variableName);
			
			VariableDeclarationStatement variableDeclaration= ast.newVariableDeclarationStatement(variableDeclarationFragment);
			variableDeclaration.setType(ast.newSimpleType(ast.newName(TYPE_NAME)));
			
			fEndPosition= rewrite.track(variableDeclaration);
			
			return variableDeclaration;
		}

		/**
		 * @return Returns the endPosition.
		 */
		public ITrackedNodePosition getEndPosition() {
			return fEndPosition;
		}

		/**
		 * @return Returns the namePosition.
		 */
		public ITrackedNodePosition getNamePosition() {
			return fNamePosition;
		}

		/**
		 * {@inheritDoc}
		 */
		protected boolean isNewContext() {
			return true;
		}
	}
	
	private final SurroundWithRunnable fSurround;

	public SurroundWithRunnableProposal(String name, IInvocationContext context, int relevance, Image image, Statement[] selectedStatements) {
		super(name, context.getCompilationUnit(), null, relevance, image);
		fSurround= new SurroundWithRunnable(context, selectedStatements);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		ASTRewrite rewrite= fSurround.getRewrite();
		addLinkedPosition(fSurround.getNamePosition(), true, "nameId"); //$NON-NLS-1$
		setEndPosition(fSurround.getEndPosition());
		return rewrite;
	}

}
