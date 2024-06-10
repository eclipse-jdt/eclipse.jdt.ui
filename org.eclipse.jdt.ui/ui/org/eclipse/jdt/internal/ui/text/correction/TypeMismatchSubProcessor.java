/*******************************************************************************
s * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [quick fix] proposes wrong cast from Object to primitive int - https://bugs.eclipse.org/bugs/show_bug.cgi?id=100593
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [quick fix] "Add exceptions to..." quickfix does nothing - https://bugs.eclipse.org/bugs/show_bug.cgi?id=107924
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CastCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ImplementInterfaceProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.OptionalCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposal;


public class TypeMismatchSubProcessor extends TypeMismatchBaseSubProcessor<ICommandAccess> {

	public TypeMismatchSubProcessor() {
		super();
	}

	public static void addTypeMismatchProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new TypeMismatchSubProcessor().collectTypeMismatchProposals(context, problem, proposals);
	}

	public static ITypeBinding boxUnboxPrimitives(ITypeBinding castType, ITypeBinding toCast, AST ast) {
		return TypeMismatchBaseSubProcessor.boxOrUnboxPrimitives(castType, toCast, ast);
	}

	public static void addChangeSenderTypeProposals(IInvocationContext context, Expression nodeToCast, ITypeBinding castTypeBinding, boolean isAssignedNode, int relevance, Collection<ICommandAccess> proposals) throws JavaModelException {
		new TypeMismatchSubProcessor().collectChangeSenderTypeProposals(context, nodeToCast, castTypeBinding, isAssignedNode, relevance, proposals);
	}

	public static ASTRewriteCorrectionProposal createCastProposal(IInvocationContext context, ITypeBinding castTypeBinding, Expression nodeToCast, int relevance) {
		return (ASTRewriteCorrectionProposal)new TypeMismatchSubProcessor().collectCastProposals(context, castTypeBinding, nodeToCast, relevance);
	}

	public static void addIncompatibleReturnTypeProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws JavaModelException {
		new TypeMismatchSubProcessor().collectIncompatibleReturnTypeProposals(context, problem, proposals);
	}

	public static void addIncompatibleThrowsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws JavaModelException {
		new TypeMismatchSubProcessor().collectIncompatibleThrowsProposals(context, problem, proposals);
	}

	public static void addTypeMismatchInForEachProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new TypeMismatchSubProcessor().collectTypeMismatchInForEachProposals(context, problem, proposals);
	}

	@Override
	protected ICommandAccess createInsertNullCheckProposal(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposal(label, compilationUnit, rewrite, relevance, image);
	}

	@Override
	protected ICommandAccess createChangeReturnTypeProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance, ITypeBinding currBinding, AST ast, CompilationUnit astRoot, MethodDeclaration methodDeclaration, BodyDeclaration decl) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, relevance, image);
		ImportRewrite imports= proposal.createImportRewrite(astRoot);
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);

		if (currBinding.isCapture()) {
			currBinding= currBinding.getWildcard();
		}
		Type newReturnType= imports.addImport(currBinding, ast, importRewriteContext, TypeLocation.RETURN_TYPE);
		rewrite.replace(methodDeclaration.getReturnType2(), newReturnType, null);

		String returnKey= "return"; //$NON-NLS-1$
		proposal.addLinkedPosition(rewrite.track(newReturnType), true, returnKey);
		for (ITypeBinding typeSuggestion : ASTResolving.getRelaxingTypes(ast, currBinding)) {
			proposal.addLinkedPositionProposal(returnKey, typeSuggestion);
		}
		return proposal;
	}

	@Override
	protected ICommandAccess createOptionalProposal(String label0, ICompilationUnit cu, Expression nodeToCast, int relevance, int optionalType) {
		return new OptionalCorrectionProposal(label0, cu, nodeToCast, relevance, optionalType);
	}

	@Override
	protected ICommandAccess createImplementInterfaceProposal(ICompilationUnit nodeCu, ITypeBinding typeDecl, CompilationUnit astRoot, ITypeBinding castTypeBinding, int relevance) {
		return new ImplementInterfaceProposal(nodeCu, typeDecl, astRoot, castTypeBinding, relevance);
	}

	@Override
	protected ICommandAccess createChangeSenderTypeProposal(ICompilationUnit targetCu, IBinding callerBindingDecl, CompilationUnit astRoot, ITypeBinding castTypeBinding, boolean isAssignedNode,
			int relevance) {
		return new TypeChangeCorrectionProposal(targetCu, callerBindingDecl, astRoot, castTypeBinding, isAssignedNode, relevance);
	}

	@Override
	protected ICommandAccess createChangeConstructorTypeProposal(ICompilationUnit targetCu, ASTNode callerNode, CompilationUnit astRoot, ITypeBinding castTypeBinding, int relevance) {
		return new TypeChangeCorrectionProposal(targetCu, callerNode, astRoot, castTypeBinding, relevance);
	}

	@Override
	protected ICommandAccess createCastCorrectionProposal(String label, ICompilationUnit cu, Expression nodeToCast, ITypeBinding castTypeBinding, int relevance) {
		return new CastCorrectionProposal(label, cu, nodeToCast, castTypeBinding, relevance);
	}

	@Override
	protected ICommandAccess createChangeReturnTypeOfOverridden(ICompilationUnit targetCu, IMethodBinding overriddenDecl, CompilationUnit astRoot, ITypeBinding returnType, boolean offerSuperTypeProposals,
			int relevance, ITypeBinding overridenDeclType) {
		TypeChangeCorrectionProposal proposal= new TypeChangeCorrectionProposal(targetCu, overriddenDecl, astRoot, returnType, offerSuperTypeProposals, relevance);
		if (overridenDeclType.isInterface()) {
			proposal.setDisplayName(Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changereturnofimplemented_description, BasicElementLabels.getJavaElementName(overriddenDecl.getName())));
		} else {
			proposal.setDisplayName(Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changereturnofoverridden_description, BasicElementLabels.getJavaElementName(overriddenDecl.getName())));
		}
		return proposal;
	}

	@Override
	protected ICommandAccess createChangeIncompatibleReturnTypeProposal(ICompilationUnit cu, IMethodBinding methodDecl, CompilationUnit astRoot, ITypeBinding overriddenReturnType, boolean offerSuperTypeProposals,
			int relevance) {
		return new TypeChangeCorrectionProposal(cu, methodDecl, astRoot, overriddenReturnType, offerSuperTypeProposals, relevance);
	}

	@Override
	protected ICommandAccess createChangeMethodSignatureProposal(String label, ICompilationUnit cu, CompilationUnit astRoot, IMethodBinding methodDeclBinding, ChangeDescription[] paramChanges,
			ChangeDescription[] changes, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
		return new ChangeMethodSignatureProposal(label, cu, astRoot, methodDeclBinding, null, changes, relevance, image);
	}

	@Override
	protected ICommandAccess createNewVariableCorrectionProposal(String label, ICompilationUnit cu, int local, SimpleName simpleName, ITypeBinding senderBinding, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		return new NewVariableCorrectionProposal(label, cu, local, simpleName, null, relevance, image);
	}

	@Override
	protected ICommandAccess createIncompatibleForEachTypeProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance, CompilationUnit astRoot, AST ast,
			ITypeBinding expectedBinding, ASTNode selectedNode, SingleVariableDeclaration parameter) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
		ImportRewrite importRewrite= proposal.createImportRewrite(astRoot);
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(ASTResolving.findParentBodyDeclaration(selectedNode), importRewrite);
		Type newType= importRewrite.addImport(expectedBinding, ast, importRewriteContext, TypeLocation.LOCAL_VARIABLE);
		rewrite.replace(parameter.getType(), newType, null);
		return proposal;
	}


}
