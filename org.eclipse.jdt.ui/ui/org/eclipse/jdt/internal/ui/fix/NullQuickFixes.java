/*******************************************************************************
 * Copyright (c) 2011, 2012 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - [quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

/**
 * Quick Fixes for null-annotation related problems.
 */
public class NullQuickFixes {

	/** Small adaptation just to make available the 'compilationUnit' passed at instantiation time. */
	private static class MyCURewriteOperationsFix extends CompilationUnitRewriteOperationsFix {
		CompilationUnit cu;

		public MyCURewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
			super(name, compilationUnit, operations);
			this.cu= compilationUnit;
		}
	}

	public static void addReturnAndArgumentTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);

		if (isComplainingAboutArgument(selectedNode) || isComplainingAboutReturn(selectedNode))
			addNullAnnotationInSignatureProposal(context, problem, proposals, false);
	}

	public static void addNullAnnotationInSignatureProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals, boolean modifyOverridden) {
		MyCURewriteOperationsFix fix= createNullAnnotationInSignatureFix(context.getASTRoot(), problem, modifyOverridden);

		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<String, String>();
			if (fix.cu != context.getASTRoot()) {
				// workaround: adjust the unit to operate on, depending on the findings of RewriteOperations.createAddAnnotationOperation(..)
				final CompilationUnit cu= fix.cu;
				final IInvocationContext originalContext= context;
				context= new IInvocationContext() {
					public int getSelectionOffset() {
						return originalContext.getSelectionOffset();
					}

					public int getSelectionLength() {
						return originalContext.getSelectionLength();
					}

					public ASTNode getCoveringNode() {
						return originalContext.getCoveringNode();
					}

					public ASTNode getCoveredNode() {
						return originalContext.getCoveredNode();
					}

					public ICompilationUnit getCompilationUnit() {
						return (ICompilationUnit) cu.getJavaElement();
					}

					public CompilationUnit getASTRoot() {
						return cu;
					}
				};
			}
			int relevance= modifyOverridden ? 9 : 10; //raise local change above change in overridden method
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new NullAnnotationsCleanUp(options, problem.getProblemId()), relevance, image, context);
			proposals.add(proposal);
		}
	}

	public static boolean isComplainingAboutArgument(ASTNode selectedNode) {
		if (!(selectedNode instanceof SimpleName))
			return false;
		SimpleName nameNode= (SimpleName) selectedNode;
		IBinding binding= nameNode.resolveBinding();
		if (binding.getKind() == IBinding.VARIABLE && ((IVariableBinding) binding).isParameter())
			return true;
		VariableDeclaration argDecl= (VariableDeclaration) ASTNodes.getParent(selectedNode, VariableDeclaration.class);
		if (argDecl != null)
			binding= argDecl.resolveBinding();
		if (binding.getKind() == IBinding.VARIABLE && ((IVariableBinding) binding).isParameter())
			return true;
		return false;
	}

	public static boolean isComplainingAboutReturn(ASTNode selectedNode) {
		return selectedNode.getParent().getNodeType() == ASTNode.RETURN_STATEMENT;
	}

	private static MyCURewriteOperationsFix createNullAnnotationInSignatureFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean modifyOverridden) {
		String nullableAnnotationName= getNullableAnnotationName(compilationUnit.getJavaElement(), false);
		String nonNullAnnotationName= getNonNullAnnotationName(compilationUnit.getJavaElement(), false);
		String annotationToAdd= nullableAnnotationName;
		String annotationToRemove= nonNullAnnotationName;

		switch (problem.getProblemId()) {
			case IProblem.IllegalDefinitionToNonNullParameter:
			case IProblem.IllegalRedefinitionToNonNullParameter:
				// case ParameterLackingNullableAnnotation: // never proposed with modifyOverridden
				if (modifyOverridden) {
					annotationToAdd= nonNullAnnotationName;
					annotationToRemove= nullableAnnotationName;
				}
				break;
			case IProblem.ParameterLackingNonNullAnnotation:
			case IProblem.IllegalReturnNullityRedefinition:
				if (!modifyOverridden) {
					annotationToAdd= nonNullAnnotationName;
					annotationToRemove= nullableAnnotationName;
				}
				break;
			case IProblem.RequiredNonNullButProvidedNull:
			case IProblem.RequiredNonNullButProvidedPotentialNull:
			case IProblem.RequiredNonNullButProvidedUnknown:
				annotationToAdd= nonNullAnnotationName;
				break;
		// all others propose to add @Nullable
		}

		// when performing one change at a time we can actually modify another CU than the current one:
		NullRewriteOperations.SignatureAnnotationRewriteOperation operation= NullRewriteOperations.createAddAnnotationOperation(compilationUnit, problem, annotationToAdd, annotationToRemove, null,
				false/*thisUnitOnly*/, true/*allowRemove*/, modifyOverridden);
		if (operation == null)
			return null;

		return new MyCURewriteOperationsFix(operation.getMessage(), operation.getCompilationUnit(), // note that this uses the findings from createAddAnnotationOperation(..)
				new NullRewriteOperations.SignatureAnnotationRewriteOperation[] { operation });
	}

	// Entry for NullAnnotationsCleanup:
	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] locations, int problemID) {
		ICompilationUnit cu= (ICompilationUnit) compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		List<CompilationUnitRewriteOperation> operations= new ArrayList<CompilationUnitRewriteOperation>();
		if (locations == null) {
			org.eclipse.jdt.core.compiler.IProblem[] problems= compilationUnit.getProblems();
			locations= new IProblemLocation[problems.length];
			for (int i= 0; i < problems.length; i++) {
				if (problems[i].getID() == problemID)
					locations[i]= new ProblemLocation(problems[i]);
			}
		}

		createAddNullAnnotationOperations(compilationUnit, locations, operations);
		if (operations.size() == 0)
			return null;
		CompilationUnitRewriteOperation[] operationsArray= operations.toArray(new CompilationUnitRewriteOperation[operations.size()]);
		return new MyCURewriteOperationsFix(NullFixMessages.QuickFixes_add_annotation_change_name, compilationUnit, operationsArray);
	}

	private static void createAddNullAnnotationOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List<CompilationUnitRewriteOperation> result) {
		String nullableAnnotationName= getNullableAnnotationName(compilationUnit.getJavaElement(), false);
		String nonNullAnnotationName= getNonNullAnnotationName(compilationUnit.getJavaElement(), false);
		Set<String> handledPositions= new HashSet<String>();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation problem= locations[i];
			if (problem == null)
				continue; // problem was filtered out by createCleanUp()
			String annotationToAdd= nullableAnnotationName;
			String annotationToRemove= nonNullAnnotationName;
			switch (problem.getProblemId()) {
				case IProblem.IllegalDefinitionToNonNullParameter:
				case IProblem.IllegalRedefinitionToNonNullParameter:
				case IProblem.ParameterLackingNonNullAnnotation:
				case IProblem.IllegalReturnNullityRedefinition:
					annotationToAdd= nonNullAnnotationName;
					annotationToRemove= nullableAnnotationName;
					break;
				case IProblem.RequiredNonNullButProvidedNull:
				case IProblem.RequiredNonNullButProvidedPotentialNull:
				case IProblem.RequiredNonNullButProvidedUnknown:
					annotationToAdd= nonNullAnnotationName;
					break;
			// all others propose to add @Nullable
			}
			// when performing multiple changes we can only modify the one CU that the CleanUp infrastructure provides to the operation.
			CompilationUnitRewriteOperation fix= NullRewriteOperations.createAddAnnotationOperation(compilationUnit, problem, annotationToAdd, annotationToRemove, handledPositions,
					true/*thisUnitOnly*/, false/*allowRemove*/, false/*modifyOverridden*/);
			if (fix != null)
				result.add(fix);
		}
	}

//	private static boolean isMissingNullAnnotationProblem(int id) {
//		return id == IProblem.RequiredNonNullButProvidedNull || id == IProblem.RequiredNonNullButProvidedPotentialNull || id == IProblem.IllegalReturnNullityRedefinition
//				|| mayIndicateParameterNullcheck(id);
//	}
//
//	private static boolean mayIndicateParameterNullcheck(int problemId) {
//		return problemId == IProblem.NonNullLocalVariableComparisonYieldsFalse || problemId == IProblem.RedundantNullCheckOnNonNullLocalVariable;
//	}

	public static boolean hasExplicitNullAnnotation(ICompilationUnit compilationUnit, int offset) {
// FIXME(SH): check for existing annotations disabled due to lack of precision:
//		      should distinguish what is actually annotated (return? param? which?)
//		try {
//			IJavaElement problemElement = compilationUnit.getElementAt(offset);
//			if (problemElement.getElementType() == IJavaElement.METHOD) {
//				IMethod method = (IMethod) problemElement;
//				String nullable = getNullableAnnotationName(compilationUnit, true);
//				String nonnull = getNonNullAnnotationName(compilationUnit, true);
//				for (IAnnotation annotation : method.getAnnotations()) {
//					if (   annotation.getElementName().equals(nonnull)
//						|| annotation.getElementName().equals(nullable))
//						return true;
//				}
//			}
//		} catch (JavaModelException jme) {
//			/* nop */
//		}
		return false;
	}

	public static String getNullableAnnotationName(IJavaElement javaElement, boolean makeSimple) {
		String qualifiedName= javaElement.getJavaProject().getOption(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, true);
		int lastDot;
		if (makeSimple && qualifiedName != null && (lastDot= qualifiedName.lastIndexOf('.')) != -1)
			return qualifiedName.substring(lastDot + 1);
		return qualifiedName;
	}

	public static String getNonNullAnnotationName(IJavaElement javaElement, boolean makeSimple) {
		String qualifiedName= javaElement.getJavaProject().getOption(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, true);
		int lastDot;
		if (makeSimple && qualifiedName != null && (lastDot= qualifiedName.lastIndexOf('.')) != -1)
			return qualifiedName.substring(lastDot + 1);
		return qualifiedName;
	}
}
