/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

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
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;

public class VarargsWarningsSubProcessor {

	private static class AddSafeVarargsProposal extends LinkedCorrectionProposal {
		public AddSafeVarargsProposal(String label, ICompilationUnit cu, MethodDeclaration methodDeclaration, IMethodBinding methodBinding, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new AddSafeVarargsProposalCore(label, cu, methodDeclaration, methodBinding, relevance));
		}
	}

	public static void addAddSafeVarargsProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
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
		AddSafeVarargsProposal proposal= new AddSafeVarargsProposal(label, context.getCompilationUnit(), methodDeclaration, null, IProposalRelevance.ADD_SAFEVARARGS);
		proposals.add(proposal);
	}

	public static void addAddSafeVarargsToDeclarationProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		if (!JavaModelUtil.is1d7OrHigher(context.getCompilationUnit().getJavaProject()))
			return;

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
					AddSafeVarargsProposal proposal= new AddSafeVarargsProposal(label, targetCu, null, methodBinding.getMethodDeclaration(), IProposalRelevance.ADD_SAFEVARARGS);
					proposals.add(proposal);
				}
			} catch (JavaModelException e) {
				return;
			}
		}
	}

	public static void addRemoveSafeVarargsProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (!(coveringNode instanceof MethodDeclaration))
			return;

		MethodDeclaration methodDeclaration= (MethodDeclaration) coveringNode;
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
		Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_SAFEVARARGS, image);
		proposals.add(proposal);
	}

	private VarargsWarningsSubProcessor() {
	}

}
