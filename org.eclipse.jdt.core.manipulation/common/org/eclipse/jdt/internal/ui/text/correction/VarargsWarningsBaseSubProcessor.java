/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

public abstract class VarargsWarningsBaseSubProcessor<T> {
	public VarargsWarningsBaseSubProcessor() {
	}

	public void createAddSafeVarargsProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());

		MethodDeclaration methodDeclaration= ASTResolving.findParentMethodDeclaration(coveringNode);
		if (methodDeclaration == null)
			return;

		IMethodBinding methodBinding= methodDeclaration.resolveBinding();
		if (methodBinding == null)
			return;

		int modifiers= methodBinding.getModifiers();
		if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers) && !methodBinding.isConstructor())
			return;

		String label= CorrectionMessages.VarargsWarningsSubProcessor_add_safevarargs_label;
		T proposal= createAddSafeVarargsProposal1(label, context.getCompilationUnit(), methodDeclaration, null, IProposalRelevance.ADD_SAFEVARARGS);
		if (proposal != null)
			proposals.add(proposal);
	}

	public void createAddSafeVarargsToDeclarationProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		IMethodBinding methodBinding;
		if (coveringNode instanceof MethodInvocation) {
			methodBinding= ((MethodInvocation) coveringNode).resolveMethodBinding();
		} else if (coveringNode instanceof ClassInstanceCreation) {
			methodBinding= ((ClassInstanceCreation) coveringNode).resolveConstructorBinding();
		} else {
			return;
		}
		if (methodBinding == null)
			return;

		String label= Messages.format(CorrectionMessages.VarargsWarningsSubProcessor_add_safevarargs_to_method_label, methodBinding.getName());

		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		CompilationUnit astRoot= (CompilationUnit) coveringNode.getRoot();
		if (declaringType != null && declaringType.isFromSource()) {
			try {
				ICompilationUnit targetCu= ASTResolving.findCompilationUnitForBinding(context.getCompilationUnit(), astRoot, declaringType);
				if (targetCu != null) {
					T proposal= createAddSafeVarargsToDeclarationProposal1(label, targetCu, null, methodBinding.getMethodDeclaration(), IProposalRelevance.ADD_SAFEVARARGS);
					if (proposal != null)
						proposals.add(proposal);
				}
			} catch (JavaModelException e) {
				return;
			}
		}
	}

	public void createRemoveSafeVarargsProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (!(coveringNode instanceof MethodDeclaration methodDeclaration))
			return;

		MarkerAnnotation annotation= null;

		for (ASTNode node : (List<? extends ASTNode>) methodDeclaration.modifiers()) {
			if (node instanceof MarkerAnnotation) {
				annotation= (MarkerAnnotation) node;
				if ("SafeVarargs".equals(annotation.resolveAnnotationBinding().getName())) { //$NON-NLS-1$
					break;
				}
			}
		}

		if (annotation == null)
			return;

		ASTRewrite rewrite= ASTRewrite.create(coveringNode.getAST());
		rewrite.remove(annotation, null);

		String label= CorrectionMessages.VarargsWarningsSubProcessor_remove_safevarargs_label;
		T proposal= createRemoveSafeVarargsProposal1(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_SAFEVARARGS);
		if (proposal != null)
			proposals.add(proposal);
	}

	protected abstract T createAddSafeVarargsProposal1(String label, ICompilationUnit compilationUnit, MethodDeclaration methodDeclaration, IMethodBinding methodBinding, int relevance);
	protected abstract T createAddSafeVarargsToDeclarationProposal1(String label, ICompilationUnit targetCu, Object object, IMethodBinding methodDeclaration, int relevance);
	protected abstract T createRemoveSafeVarargsProposal1(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int removeSafevarargs);
}
