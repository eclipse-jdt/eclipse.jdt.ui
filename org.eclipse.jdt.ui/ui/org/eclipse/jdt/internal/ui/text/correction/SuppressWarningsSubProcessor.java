/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.fix.IProposableFix;

import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

public class SuppressWarningsSubProcessor extends SuppressWarningsBaseSubProcessor<ICommandAccess> {

	public static void addSuppressWarningsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new SuppressWarningsSubProcessor().getSuppressWarningsProposals(context, problem, proposals);
	}


	private static class SuppressWarningsProposal extends ASTRewriteCorrectionProposal {
		public SuppressWarningsProposal(String warningToken, String label, ICompilationUnit cu, ASTNode node, ChildListPropertyDescriptor property, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new SuppressWarningsProposalCore(warningToken, label, cu, node, property, relevance));
		}
	}

	public static void addUnknownSuppressWarningProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new SuppressWarningsSubProcessor().getUnknownSuppressWarningProposals(context, problem, proposals);
	}


	public static void addRemoveUnusedSuppressWarningProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new SuppressWarningsSubProcessor().getRemoveUnusedSuppressWarningProposals(context, problem, proposals);
	}

	@Override
	protected ICommandAccess createSuppressWarningsProposal(String warningToken, String label, ICompilationUnit cu, ASTNode node, ChildListPropertyDescriptor property, int relevance) {
		return new SuppressWarningsProposal(warningToken, label, cu, node, property, relevance);
	}

	@Override
	protected ICommandAccess createASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		// Initialize as default image, though it should always trigger one of the two if statements below
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		if(relevance == IProposalRelevance.FIX_SUPPRESS_TOKEN) {
			image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		} else if (relevance == IProposalRelevance.REMOVE_ANNOTATION) {
			image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		}
		return new ASTRewriteCorrectionProposal(name, cu, rewrite, relevance, image);
	}

	@Override
	protected ICommandAccess createFixCorrectionProposal(IProposableFix fix, ICleanUp cleanUp, int relevance, IInvocationContext context) {
		// Initialize as default image, though it should always trigger one of the two if statements below
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		if (relevance == IProposalRelevance.REMOVE_ANNOTATION) {
			image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		}
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, relevance, image, context);
		proposal.setCommandId(ADD_SUPPRESSWARNINGS_ID);
		return proposal;
	}

	SuppressWarningsSubProcessor() {
	}

}
