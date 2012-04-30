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

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

public class NullRewriteOperations {

	static abstract class SignatureAnnotationRewriteOperation extends CompilationUnitRewriteOperation {
		String fAnnotationToAdd;
		String fAnnotationToRemove;
		boolean fAllowRemove;
		CompilationUnit fUnit;
		protected String fKey;
		protected String fMessage;

		/* A globally unique key that identifies the position being annotated (for avoiding double annotations). */
		public String getKey() {
			return this.fKey;
		}

		public CompilationUnit getCompilationUnit() {
			return fUnit;
		}

		boolean checkExisting(List<IExtendedModifier> existingModifiers, ListRewrite listRewrite, TextEditGroup editGroup) {
			for (Object mod : existingModifiers) {
				if (mod instanceof MarkerAnnotation) {
					MarkerAnnotation annotation= (MarkerAnnotation) mod;
					String existingName= annotation.getTypeName().getFullyQualifiedName();
					int lastDot= fAnnotationToRemove.lastIndexOf('.');
					if (existingName.equals(fAnnotationToRemove) || (lastDot != -1 && fAnnotationToRemove.substring(lastDot + 1).equals(existingName))) {
						if (!fAllowRemove)
							return false; // veto this change
						listRewrite.remove(annotation, editGroup);
						return true;
					}
					// paranoia: check if by accident the annotation is already present (shouldn't happen):
					lastDot= fAnnotationToAdd.lastIndexOf('.');
					if (existingName.equals(fAnnotationToAdd) || (lastDot != -1 && fAnnotationToAdd.substring(lastDot + 1).equals(existingName))) {
						return false; // already present
					}
				}
			}
			return true;
		}

		public String getMessage() {
			return fMessage;
		}
	}

	/**
	 * Rewrite operation that inserts an annotation into a method signature.
	 * 
	 * Crafted after the lead of Java50Fix.AnnotationRewriteOperation
	 */
	static class ReturnAnnotationRewriteOperation extends SignatureAnnotationRewriteOperation {

		private final BodyDeclaration fBodyDeclaration;

		ReturnAnnotationRewriteOperation(CompilationUnit unit, MethodDeclaration method, String annotationToAdd, String annotationToRemove, boolean allowRemove, String message) {
			fUnit= unit;
			fKey= method.resolveBinding().getKey() + "<return>"; //$NON-NLS-1$
			fBodyDeclaration= method;
			fAnnotationToAdd= annotationToAdd;
			fAnnotationToRemove= annotationToRemove;
			fAllowRemove= allowRemove;
			fMessage= message;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			AST ast= cuRewrite.getRoot().getAST();
			ListRewrite listRewrite= cuRewrite.getASTRewrite().getListRewrite(fBodyDeclaration, fBodyDeclaration.getModifiersProperty());
			TextEditGroup group= createTextEditGroup(fMessage, cuRewrite);
			if (!checkExisting(fBodyDeclaration.modifiers(), listRewrite, group))
				return;
			Annotation newAnnotation= ast.newMarkerAnnotation();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			String resolvableName= importRewrite.addImport(fAnnotationToAdd);
			newAnnotation.setTypeName(ast.newName(resolvableName));
			listRewrite.insertLast(newAnnotation, group); // null annotation is last modifier, directly preceding the return type
		}
	}

	static class ParameterAnnotationRewriteOperation extends SignatureAnnotationRewriteOperation {

		private SingleVariableDeclaration fArgument;

		ParameterAnnotationRewriteOperation(CompilationUnit unit, MethodDeclaration method, String annotationToAdd, String annotationToRemove, String paramName, boolean allowRemove, String message) {
			fUnit= unit;
			fKey= method.resolveBinding().getKey();
			fAnnotationToAdd= annotationToAdd;
			fAnnotationToRemove= annotationToRemove;
			fAllowRemove= allowRemove;
			fMessage= message;
			for (Object param : method.parameters()) {
				SingleVariableDeclaration argument= (SingleVariableDeclaration) param;
				if (argument.getName().getIdentifier().equals(paramName)) {
					fArgument= argument;
					fKey+= argument.getName().getIdentifier();
					return;
				}
			}
			// shouldn't happen, we've checked that paramName indeed denotes a parameter.
			throw new RuntimeException("Argument " + paramName + " not found in method " + method.getName().getIdentifier()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		ParameterAnnotationRewriteOperation(CompilationUnit unit, MethodDeclaration method, String annotationToAdd, String annotationToRemove, int paramIdx, boolean allowRemove, String message) {
			fUnit= unit;
			fKey= method.resolveBinding().getKey();
			fAnnotationToAdd= annotationToAdd;
			fAnnotationToRemove= annotationToRemove;
			fAllowRemove= allowRemove;
			fArgument= (SingleVariableDeclaration) method.parameters().get(paramIdx);
			fKey+= fArgument.getName().getIdentifier();
			fMessage= message;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			AST ast= cuRewrite.getRoot().getAST();
			ListRewrite listRewrite= cuRewrite.getASTRewrite().getListRewrite(fArgument, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
			TextEditGroup group= createTextEditGroup(fMessage, cuRewrite);
			if (!checkExisting(fArgument.modifiers(), listRewrite, group))
				return;
			Annotation newAnnotation= ast.newMarkerAnnotation();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			String resolvableName= importRewrite.addImport(fAnnotationToAdd);
			newAnnotation.setTypeName(ast.newName(resolvableName));
			listRewrite.insertLast(newAnnotation, group); // null annotation is last modifier, directly preceding the type
		}
	}

	// Entry for QuickFixes:
	public static SignatureAnnotationRewriteOperation createAddAnnotationOperation(CompilationUnit compilationUnit, IProblemLocation problem, String annotationToAdd, String annotationToRemove,
			Set<String> handledPositions, boolean thisUnitOnly, boolean allowRemove, boolean modifyOverridden) {
		SignatureAnnotationRewriteOperation result= modifyOverridden ? createAddAnnotationToOverriddenOperation(compilationUnit, problem, annotationToAdd, annotationToRemove, handledPositions,
				thisUnitOnly, allowRemove) : createAddAnnotationOperation(compilationUnit, problem, annotationToAdd, annotationToRemove, handledPositions, thisUnitOnly, allowRemove);
		if (handledPositions != null && result != null) {
			if (handledPositions.contains(result.getKey()))
				return null;
			handledPositions.add(result.getKey());
		}
		return result;
	}

	private static SignatureAnnotationRewriteOperation createAddAnnotationOperation(CompilationUnit compilationUnit, IProblemLocation problem, String annotationToAdd, String annotationToRemove,
			Set<String> handledPositions, boolean thisUnitOnly, boolean allowRemove) {
		ICompilationUnit cu= (ICompilationUnit) compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode == null)
			return null;
		ASTNode declaringNode= getDeclaringNode(selectedNode);

		switch (problem.getProblemId()) {
			case IProblem.IllegalDefinitionToNonNullParameter:
//			case IllegalRedefinitionToNonNullParameter:
				// these affect another method
				break;
			case IProblem.IllegalReturnNullityRedefinition:
				if (declaringNode == null)
					declaringNode= selectedNode;
				break; // do propose changes even if we already have an annotation
			default:
				// if this method has annotations, don't change'em
				if (NullQuickFixes.hasExplicitNullAnnotation(cu, problem.getOffset()))
					return null;
		}

		String annotationNameLabel= annotationToAdd;
		int lastDot= annotationToAdd.lastIndexOf('.');
		if (lastDot != -1)
			annotationNameLabel= annotationToAdd.substring(lastDot + 1);
		annotationNameLabel= BasicElementLabels.getJavaElementName(annotationNameLabel);

		if (selectedNode.getParent() instanceof MethodInvocation) {
			// DefiniteNullToNonNullParameter || PotentialNullToNonNullParameter
			MethodInvocation methodInvocation= (MethodInvocation) selectedNode.getParent();
			int paramIdx= methodInvocation.arguments().indexOf(selectedNode);
			IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
			compilationUnit= findCUForMethod(compilationUnit, cu, methodBinding);
			if (compilationUnit == null)
				return null;
			if (thisUnitOnly && !compilationUnit.getJavaElement().equals(cu))
				return null;
			ASTNode methodDecl= compilationUnit.findDeclaringNode(methodBinding.getKey());
			if (methodDecl == null)
				return null;
			String message= Messages.format(NullFixMessages.QuickFixes_change_method_parameter_nullness, annotationNameLabel);
			return new ParameterAnnotationRewriteOperation(compilationUnit, (MethodDeclaration) methodDecl, annotationToAdd, annotationToRemove, paramIdx, allowRemove, message);
		} else if (declaringNode instanceof MethodDeclaration) {
			// complaint is in signature of this method
			MethodDeclaration declaration= (MethodDeclaration) declaringNode;
			switch (problem.getProblemId()) {
				case IProblem.ParameterLackingNonNullAnnotation:
				case IProblem.ParameterLackingNullableAnnotation:
				case IProblem.IllegalDefinitionToNonNullParameter:
				case IProblem.IllegalRedefinitionToNonNullParameter:
				case IProblem.NonNullLocalVariableComparisonYieldsFalse:
				case IProblem.RedundantNullCheckOnNonNullLocalVariable:
					// statements suggest changing parameters:
					if (declaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
						String paramName= findAffectedParameterName(selectedNode);
						if (paramName != null) {
							String message= Messages.format(NullFixMessages.QuickFixes_change_method_parameter_nullness, annotationNameLabel);
							return new ParameterAnnotationRewriteOperation(compilationUnit, declaration, annotationToAdd, annotationToRemove, paramName, allowRemove, message);
						}
					}
					break;
				case IProblem.RequiredNonNullButProvidedNull:
				case IProblem.RequiredNonNullButProvidedPotentialNull:
				case IProblem.RequiredNonNullButProvidedUnknown:
					if (NullQuickFixes.isComplainingAboutArgument(selectedNode)) {
						//TODO: duplication
						// statements suggest changing parameters:
						if (declaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
							String paramName= findAffectedParameterName(selectedNode);
							if (paramName != null) {
								String message= Messages.format(NullFixMessages.QuickFixes_change_method_parameter_nullness, annotationNameLabel);
								return new ParameterAnnotationRewriteOperation(compilationUnit, declaration, annotationToAdd, annotationToRemove, paramName, allowRemove, message);
							}
						}
						break;
					}
					//$FALL-THROUGH$
				case IProblem.IllegalReturnNullityRedefinition:
					String message= Messages.format(NullFixMessages.QuickFixes_change_method_return_nullness, new String[] { declaration.getName().getIdentifier(), annotationNameLabel });
					return new ReturnAnnotationRewriteOperation(compilationUnit, declaration, annotationToAdd, annotationToRemove, allowRemove, message);
			}
		}
		return null;
	}

	private static SignatureAnnotationRewriteOperation createAddAnnotationToOverriddenOperation(CompilationUnit compilationUnit, IProblemLocation problem, String annotationToAdd,
			String annotationToRemove, Set<String> handledPositions, boolean thisUnitOnly, boolean allowRemove) {
		ICompilationUnit cu= (ICompilationUnit) compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode == null)
			return null;

		ASTNode declaringNode= getDeclaringNode(selectedNode);
		switch (problem.getProblemId()) {
			case IProblem.IllegalDefinitionToNonNullParameter:
			case IProblem.IllegalRedefinitionToNonNullParameter:
				break;
			case IProblem.IllegalReturnNullityRedefinition:
				if (declaringNode == null)
					declaringNode= selectedNode;
				break;
			default:
				return null;
		}

		String annotationNameLabel= annotationToAdd;
		int lastDot= annotationToAdd.lastIndexOf('.');
		if (lastDot != -1)
			annotationNameLabel= annotationToAdd.substring(lastDot + 1);
		annotationNameLabel= BasicElementLabels.getJavaElementName(annotationNameLabel);

		if (declaringNode instanceof MethodDeclaration) {
			// complaint is in signature of this method
			MethodDeclaration declaration= (MethodDeclaration) declaringNode;
			switch (problem.getProblemId()) {
				case IProblem.IllegalDefinitionToNonNullParameter:
				case IProblem.IllegalRedefinitionToNonNullParameter:
					return createChangeOverriddenParameterOperation(compilationUnit, cu, declaration, selectedNode, allowRemove, annotationToAdd, annotationToRemove, annotationNameLabel);
				case IProblem.IllegalReturnNullityRedefinition:
					if (hasNullAnnotation(declaration)) { // don't adjust super if local has no explicit annotation (?)
						return createChangeOverriddenReturnOperation(compilationUnit, cu, declaration, allowRemove, annotationToAdd, annotationToRemove, annotationNameLabel);
					}
			}
		}
		return null;
	}

	private static SignatureAnnotationRewriteOperation createChangeOverriddenParameterOperation(CompilationUnit compilationUnit, ICompilationUnit cu, MethodDeclaration declaration,
			ASTNode selectedNode, boolean allowRemove, String annotationToAdd, String annotationToRemove, String annotationNameLabel) {
		IMethodBinding methodDeclBinding= declaration.resolveBinding();
		if (methodDeclBinding == null)
			return null;

		IMethodBinding overridden= Bindings.findOverriddenMethod(methodDeclBinding, false);
		if (overridden == null)
			return null;
		compilationUnit= findCUForMethod(compilationUnit, cu, overridden);
		if (compilationUnit == null)
			return null;
		ASTNode methodDecl= compilationUnit.findDeclaringNode(overridden.getKey());
		if (methodDecl == null)
			return null;
		declaration= (MethodDeclaration) methodDecl;
		String message= Messages.format(NullFixMessages.QuickFixes_change_overridden_parameter_nullness, new String[] { overridden.getName(), annotationNameLabel });
		String paramName= findAffectedParameterName(selectedNode);
		return new ParameterAnnotationRewriteOperation(compilationUnit, declaration, annotationToAdd, annotationToRemove, paramName, allowRemove, message);
	}

	private static String findAffectedParameterName(ASTNode selectedNode) {
		VariableDeclaration argDecl= (selectedNode instanceof VariableDeclaration) ? (VariableDeclaration) selectedNode : (VariableDeclaration) ASTNodes.getParent(selectedNode,
				VariableDeclaration.class);
		if (argDecl != null)
			return argDecl.getName().getIdentifier();
		if (selectedNode.getNodeType() == ASTNode.SIMPLE_NAME) {
			IBinding binding= ((SimpleName) selectedNode).resolveBinding();
			if (binding.getKind() == IBinding.VARIABLE && ((IVariableBinding) binding).isParameter())
				return ((SimpleName) selectedNode).getIdentifier();
		}
		return null;
	}

	private static boolean hasNullAnnotation(MethodDeclaration decl) {
		List<IExtendedModifier> modifiers= decl.modifiers();
		String nonnull= NullQuickFixes.getNonNullAnnotationName(decl.resolveBinding().getJavaElement(), false);
		String nullable= NullQuickFixes.getNullableAnnotationName(decl.resolveBinding().getJavaElement(), false);
		for (Object mod : modifiers) {
			if (mod instanceof Annotation) {
				Name annotationName= ((Annotation) mod).getTypeName();
				String fullyQualifiedName= annotationName.getFullyQualifiedName();
				if (annotationName.isSimpleName() ? nonnull.endsWith(fullyQualifiedName) : fullyQualifiedName.equals(nonnull))
					return true;
				if (annotationName.isSimpleName() ? nullable.endsWith(fullyQualifiedName) : fullyQualifiedName.equals(nullable))
					return true;
			}
		}
		return false;
	}

	private static SignatureAnnotationRewriteOperation createChangeOverriddenReturnOperation(CompilationUnit compilationUnit, ICompilationUnit cu, MethodDeclaration declaration, boolean allowRemove,
			String annotationToAdd, String annotationToRemove, String annotationNameLabel) {
		IMethodBinding methodDeclBinding= declaration.resolveBinding();
		if (methodDeclBinding == null)
			return null;

		IMethodBinding overridden= Bindings.findOverriddenMethod(methodDeclBinding, false);
		if (overridden == null)
			return null;
		compilationUnit= findCUForMethod(compilationUnit, cu, overridden);
		if (compilationUnit == null)
			return null;
		ASTNode methodDecl= compilationUnit.findDeclaringNode(overridden.getKey());
		if (methodDecl == null)
			return null;
		declaration= (MethodDeclaration) methodDecl;
// TODO(SH): decide whether we want to propose overwriting existing annotations in super
//		if (hasNullAnnotation(declaration)) // if overridden has explicit declaration don't propose to change it
//			return null;
		String message= Messages.format(NullFixMessages.QuickFixes_change_overridden_return_nullness, new String[] { overridden.getName(), annotationNameLabel });
		return new ReturnAnnotationRewriteOperation(compilationUnit, declaration, annotationToAdd, annotationToRemove, allowRemove, message);
	}

	private static CompilationUnit findCUForMethod(CompilationUnit compilationUnit, ICompilationUnit cu, IMethodBinding methodBinding) {
		ASTNode methodDecl= compilationUnit.findDeclaringNode(methodBinding.getMethodDeclaration());
		if (methodDecl == null) {
			// is methodDecl defined in another CU?
			ITypeBinding declaringTypeDecl= methodBinding.getDeclaringClass().getTypeDeclaration();
			if (declaringTypeDecl.isFromSource()) {
				ICompilationUnit targetCU= null;
				try {
					targetCU= ASTResolving.findCompilationUnitForBinding(cu, compilationUnit, declaringTypeDecl);
				} catch (JavaModelException e) { /* can't do better */
				}
				if (targetCU != null) {
					return ASTResolving.createQuickFixAST(targetCU, null);
				}
			}
			return null;
		}
		return compilationUnit;
	}

	/* The relevant declaring node of a return statement is the enclosing method. */
	private static ASTNode getDeclaringNode(ASTNode selectedNode) {
		return ASTNodes.getParent(selectedNode, ASTNode.METHOD_DECLARATION);
	}
}
