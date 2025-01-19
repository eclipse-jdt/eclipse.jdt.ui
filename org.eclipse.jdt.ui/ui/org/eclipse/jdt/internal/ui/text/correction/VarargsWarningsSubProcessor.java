/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;

public class VarargsWarningsSubProcessor extends VarargsWarningsBaseSubProcessor<ICommandAccess> {

	private static class AddSafeVarargsProposal extends LinkedCorrectionProposal {
		public AddSafeVarargsProposal(String label, ICompilationUnit cu, MethodDeclaration methodDeclaration, IMethodBinding methodBinding, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new AddSafeVarargsProposalCore(label, cu, methodDeclaration, methodBinding, relevance));
		}
	}

	public static void addAddSafeVarargsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new VarargsWarningsSubProcessor().createAddSafeVarargsProposals(context, problem, proposals);
	}

	public static void addAddSafeVarargsToDeclarationProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new VarargsWarningsSubProcessor().createAddSafeVarargsToDeclarationProposals(context, problem, proposals);
	}

	public static void addRemoveSafeVarargsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new VarargsWarningsSubProcessor().createRemoveSafeVarargsProposals(context, problem, proposals);
	}

	private VarargsWarningsSubProcessor() {
	}

	@Override
	protected ICommandAccess createAddSafeVarargsProposal1(String label, ICompilationUnit compilationUnit, MethodDeclaration methodDeclaration, IMethodBinding methodBinding, int relevance) {
		return new AddSafeVarargsProposal(label, compilationUnit, methodDeclaration, methodBinding, relevance);
	}

	@Override
	protected ICommandAccess createAddSafeVarargsToDeclarationProposal1(String label, ICompilationUnit targetCu, Object object, IMethodBinding methodDeclaration, int relevance) {
		return new AddSafeVarargsProposal(label, targetCu, null, methodDeclaration, relevance);
	}

	@Override
	protected ICommandAccess createRemoveSafeVarargsProposal1(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int relevance) {
		Image image= ISharedImages.get().getImage(ISharedImages.IMG_TOOL_DELETE);
		return new ASTRewriteCorrectionProposal(label, compilationUnit, rewrite, relevance, image);
	}
}
