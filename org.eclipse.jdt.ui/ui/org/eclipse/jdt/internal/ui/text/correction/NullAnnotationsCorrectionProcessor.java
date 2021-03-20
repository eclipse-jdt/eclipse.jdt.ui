/*******************************************************************************
 * Copyright (c) 2011, 2017 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - [quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsFix;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.ChangeKind;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.NullAnnotationsCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreatePackageInfoWithDefaultNullnessProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ExtractToNullCheckedLocalProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MakeLocalVariableNonNullProposal;

/**
 * Quick Fixes for null-annotation related problems.
 */
public class NullAnnotationsCorrectionProcessor {

	// pre: changeKind != OVERRIDDEN
	public static void addReturnAndArgumentTypeProposal(IInvocationContext context, IProblemLocation problem, ChangeKind changeKind,
			Collection<ICommandAccess> proposals) {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);

		boolean isArgumentProblem= NullAnnotationsFix.isComplainingAboutArgument(selectedNode);
		if (isArgumentProblem || NullAnnotationsFix.isComplainingAboutReturn(selectedNode))
			addNullAnnotationInSignatureProposal(context, problem, proposals, changeKind, isArgumentProblem);
	}

	public static void addNullAnnotationInSignatureProposal(IInvocationContext context, IProblemLocation problem,
			Collection<ICommandAccess> proposals, ChangeKind changeKind, boolean isArgumentProblem) {
		NullAnnotationsFix fix= NullAnnotationsFix.createNullAnnotationInSignatureFix(context.getASTRoot(), problem,
				changeKind, isArgumentProblem);

		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<>();
			if (fix.getCu() != context.getASTRoot()) {
				// workaround: adjust the unit to operate on, depending on the findings of RewriteOperations.createAddAnnotationOperation(..)
				final CompilationUnit cu= fix.getCu();
				final IInvocationContext originalContext= context;
				context= new IInvocationContext() {
					@Override
					public int getSelectionOffset() {
						return originalContext.getSelectionOffset();
					}

					@Override
					public int getSelectionLength() {
						return originalContext.getSelectionLength();
					}

					@Override
					public ASTNode getCoveringNode() {
						return originalContext.getCoveringNode();
					}

					@Override
					public ASTNode getCoveredNode() {
						return originalContext.getCoveredNode();
					}

					@Override
					public ICompilationUnit getCompilationUnit() {
						return (ICompilationUnit) cu.getJavaElement();
					}

					@Override
					public CompilationUnit getASTRoot() {
						return cu;
					}
				};
			}
			int relevance= (changeKind == ChangeKind.OVERRIDDEN) ? IProposalRelevance.CHANGE_NULLNESS_ANNOTATION_IN_OVERRIDDEN_METHOD : IProposalRelevance.CHANGE_NULLNESS_ANNOTATION; //raise local change above change in overridden method
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new NullAnnotationsCleanUp(options, problem.getProblemId()), relevance, image, context);
			proposals.add(proposal);
		}
	}

	public static void addRemoveRedundantAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		NullAnnotationsFix fix= NullAnnotationsFix.createRemoveRedundantNullAnnotationsFix(context.getASTRoot(), problem);
		if (fix == null)
			return;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new Hashtable<>();
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new NullAnnotationsCleanUp(options, problem.getProblemId()), IProposalRelevance.REMOVE_REDUNDANT_NULLNESS_ANNOTATION, image, context);
		proposals.add(proposal);
	}

	public static void addAddMissingDefaultNullnessProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		final CompilationUnit astRoot= context.getASTRoot();
		if (JavaModelUtil.PACKAGE_INFO_JAVA.equals(astRoot.getJavaElement().getElementName())) {
			NullAnnotationsFix fix= NullAnnotationsFix.createAddMissingDefaultNullnessAnnotationsFix(astRoot, problem);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<>();
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new NullAnnotationsCleanUp(options, problem.getProblemId()), IProposalRelevance.ADD_MISSING_NULLNESS_ANNOTATION, image,
					context);
			proposals.add(proposal);
		} else {
			final IPackageFragment pack= (IPackageFragment) astRoot.getJavaElement().getParent();
			String nonNullByDefaultAnnotationname= NullAnnotationsFix.getNonNullByDefaultAnnotationName(pack, true);
			String label= Messages.format(CorrectionMessages.NullAnnotationsCorrectionProcessor_create_packageInfo_with_defaultnullness, new String[] { nonNullByDefaultAnnotationname });
			proposals.add(CreatePackageInfoWithDefaultNullnessProposal.createFor(problem.getProblemId(), label, pack));
		}
	}

	/**
	 * Fix for {@link IProblem#NullableFieldReference}
	 * @param context context
	 * @param problem problem to be fixed
	 * @param proposals accumulator for computed proposals
	 */
	public static void addExtractCheckedLocalProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		CompilationUnit compilationUnit = context.getASTRoot();
		ICompilationUnit cu= (ICompilationUnit) compilationUnit.getJavaElement();

		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		SimpleName name= findProblemFieldName(selectedNode, problem.getProblemId());
		if (name == null)
			return;

		ASTNode method= ASTNodes.getParent(selectedNode, MethodDeclaration.class);
		if (method == null)
			method= ASTNodes.getParent(selectedNode, Initializer.class);
		if (method == null)
			return;

		proposals.add(new ExtractToNullCheckedLocalProposal(cu, compilationUnit, name, method));
	}
	private static SimpleName findProblemFieldName(ASTNode selectedNode, int problemID) {
		// if a field access occurs in an compatibility situation (assignment/return/argument)
		// with expected type declared @NonNull we first need to find the SimpleName inside:
		if (selectedNode instanceof FieldAccess)
			selectedNode= ((FieldAccess) selectedNode).getName();
		else if (selectedNode instanceof QualifiedName)
			selectedNode= ((QualifiedName) selectedNode).getName();

		if (selectedNode instanceof SimpleName) {
			SimpleName name= (SimpleName) selectedNode;
			if (problemID == IProblem.NullableFieldReference)
				return name;
			// not field dereference, but compatibility issue - is value a field reference?
			IBinding binding= name.resolveBinding();
			if ((binding instanceof IVariableBinding) && ((IVariableBinding) binding).isField())
				return name;
		}
		return null;
	}

	public static void addLocalVariableAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		String nonNullAnnotationName= NullAnnotationsFix.getNonNullAnnotationName(astRoot.getJavaElement(), false);

		if (nonNullAnnotationName == null) {
			return;
		}

		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (!(selectedNode instanceof Expression)) {
			return;
		}
		Expression nodeToCast= (Expression) selectedNode;
		IBinding callerBinding= Bindings.resolveExpressionBinding(nodeToCast, false);
		if (callerBinding instanceof IVariableBinding) {
			IVariableBinding variableBinding= (IVariableBinding) callerBinding;

			if (variableBinding.isField()) {
				return;
			}
			ITypeBinding type= variableBinding.getType();
			if (type == null || type.isArray()) {
				return;
			}
			proposals.add(new MakeLocalVariableNonNullProposal(cu, variableBinding, astRoot, IProposalRelevance.CHANGE_NULLNESS_ANNOTATION, nonNullAnnotationName));
		}
	}

	private NullAnnotationsCorrectionProcessor() {
	}
}
