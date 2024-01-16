/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ILinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeInLambdaCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;


public class ReturnTypeSubProcessor extends ReturnTypeBaseSubProcessor<ICommandAccess> {
	protected ReturnTypeSubProcessor() {
	}

	public static void addMethodWithConstrNameProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectMethodWithConstrNameProposals(context, problem, proposals);
	}

	public static void addVoidMethodReturnsProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectVoidMethodReturnsProposals(context, problem, proposals);
	}



	public static void addMissingReturnTypeProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectMissingReturnTypeProposals(context, problem, proposals);
	}

	public static void addMissingReturnStatementProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectMissingReturnStatementProposals(context, problem, proposals);
	}

	public static void replaceReturnWithYieldStatementProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectReplaceReturnWithYieldStatementProposals(context, problem, proposals);
	}

	public static void addMethodRetunsVoidProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws JavaModelException {
		new ReturnTypeSubProcessor().collectMethodReturnsVoidProposals(context, problem, proposals);
	}

	@Override
	protected ICommandAccess createMethodWithConstrNameProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
	}

	@Override
	protected ICommandAccess voidMethodReturnsProposal1ToT(ILinkedCorrectionProposalCore prop) {
		return (ICommandAccess)prop;
	}

	@Override
	protected ICommandAccess createVoidMethodReturnsProposal2(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
	}

	@Override
	protected ILinkedCorrectionProposalCore createVoidMethodReturnsProposal1(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new LinkedCorrectionProposal(label, cu, rewrite, relevance, image);
	}

	@Override
	protected ICommandAccess createWrongConstructorNameProposal(String label, ICompilationUnit cu, int startPosition, int length, String constructorName, int relevance) {
		return new ReplaceCorrectionProposal(label, cu, startPosition, length, constructorName, relevance);
	}

	@Override
	protected ICommandAccess missingReturnTypeProposal1ToT(ILinkedCorrectionProposalCore proposal) {
		return (ICommandAccess)proposal;
	}

	@Override
	protected ILinkedCorrectionProposalCore createMissingReturnTypeProposal1(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new LinkedCorrectionProposal(label, cu, rewrite, relevance, image);
	}

	@Override
	protected ICommandAccess changeReturnTypeToVoidProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
	}

	@Override
	protected ICommandAccess createMissingReturnTypeInMethodCorrectionProposal(ICompilationUnit cu, MethodDeclaration methodDecl, ReturnStatement existingStatement, int relevance) {
		return new MissingReturnTypeCorrectionProposal(cu, methodDecl, existingStatement, relevance);
	}

	@Override
	protected ICommandAccess createMissingReturnTypeInLambdaCorrectionProposal(ICompilationUnit cu, LambdaExpression selectedNode, ReturnStatement existingStatement, int relevance) {
		return new MissingReturnTypeInLambdaCorrectionProposal(cu, selectedNode, existingStatement, relevance);
	}

	@Override
	protected ICommandAccess createReplaceReturnWithYieldStatementProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
	}

	@Override
	protected TypeMismatchBaseSubProcessor<ICommandAccess> getTypeMismatchSubProcessor() {
		return new TypeMismatchSubProcessor();
	}

}
