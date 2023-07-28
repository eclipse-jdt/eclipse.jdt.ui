/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * The InitialConditionsChecker class provides static methods to check various initial conditions
 * for a refactoring.
 */
public class InitialConditionsChecker {

	private RefactoringStatus fStatus;

	public InitialConditionsChecker(RefactoringStatus status) {
		fStatus= status;
	}

	/**
	 * Checks if the start position of a text selection is valid. A Selection is valid if the offset
	 * and length are greater zero.
	 *
	 * @param selection the text selection to be checked
	 *
	 * @return {@code true} if the selection is valid, {@code false} otherwise.
	 */
	public boolean checkValidTextSelectionStart(Selection selection) {
		if (selection.getOffset() < 0 || selection.getLength() < 0) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return !fStatus.hasError();
	}

	/**
	 * Checks the validity of an ICompilationUnit. The ICompilationUnit is valid if it not null.
	 *
	 * @param iCompilationUnit the ICompilationUnit to be checked
	 * @return {@code true} if the ICompilationUnit is valid, {@code false} otherwise.
	 */
	public boolean checkValidICompilationUnit(ICompilationUnit iCompilationUnit) {
		if (iCompilationUnit == null) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return !fStatus.hasError();
	}


	/**
	 * Checks the validity of an ASTNode as a method. The check fails if the AStNode is not an
	 * instance of MethodDeclaration or MethodInvocation. It also fails if the ASTNode is null or
	 * instance of SuperMethodInvocation.
	 *
	 * @param selectedNode the ASTNode to be checked
	 *
	 * @return {@code true} if the ASTNode is a valid method, {@code false} otherwise.
	 */
	public boolean checkASTNodeIsValidMethod(ASTNode selectedNode) {
		if (selectedNode == null) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		} else if (selectedNode instanceof SuperMethodInvocation) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations));
		}
		if (!(selectedNode instanceof MethodDeclaration || selectedNode instanceof MethodInvocation)) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return !fStatus.hasError();
	}


	/**
	 * Checks the validity of an IMethod. The IMethod is valid if it is not null and if its
	 * declaring type is not an annotation.
	 *
	 * @param iMethod the IMethod to be checked
	 *
	 * @return {@code true} if the IMethod is valid, {@code false} otherwise.
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public boolean checkValidIMethod(IMethod iMethod) throws JavaModelException {
		if (iMethod == null) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		} else if (iMethod.getDeclaringType().isAnnotation()) {
			fStatus.addFatalError(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_annotation);
		}
		return !fStatus.hasError();
	}

	/**
	 * Checks if the method is not declared in a local or anonymous class.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return {@code true} if the Method is not in a local or anonymous class, {@code false}
	 *         otherwise.
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public boolean checkMethodNotInLocalOrAnonymousClass(IMethod iMethod) throws JavaModelException {
		if (iMethod.getDeclaringType().isLocal() || iMethod.getDeclaringType().isAnonymous()) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_local_or_anonymous_types));
		}

		return !fStatus.hasError();
	}

	/**
	 * Checks if the method is not a constructor.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return {@code true} if the method is not a constructor, {@code false} otherwise.
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public boolean checkMethodIsNotConstructor(IMethod iMethod) throws JavaModelException {
		if (iMethod.isConstructor()) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_constructors));
		}

		return !fStatus.hasError();
	}

	/**
	 * Checks if the method is not already static.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return {@code true} if the method is not static, {@code false} otherwise.
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public boolean checkMethodNotStatic(IMethod iMethod) throws JavaModelException {
		int flags= iMethod.getFlags();
		if (Modifier.isStatic(flags)) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_already_static));
		}
		return !fStatus.hasError();
	}

	/**
	 * Checks if the method is not overridden in any subtype.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return {@code true} if the method is not overridden, {@code false} otherwise.
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public boolean checkMethodNotOverridden(IMethod iMethod) throws JavaModelException {
		if (isOverridden(iMethod.getDeclaringType(), iMethod)) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_is_overridden_in_subtype));
		}

		return !fStatus.hasError();
	}

	/**
	 * Checks if the source code is available for the selected method.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return {@code true} if the source is available, {@code false} otherwise.
	 */
	public boolean checkSourceAvailable(IMethod iMethod) {
		if (iMethod.getCompilationUnit() == null) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_source_not_available_for_selected_method));
		}
		return !fStatus.hasError();
	}

	private boolean isOverridden(IType type, IMethod iMethod) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] subtypes= hierarchy.getAllSubtypes(type);
		for (IType subtype : subtypes) {
			IMethod[] methods= subtype.getMethods();
			for (IMethod method : methods) {
				if (method.isSimilar(iMethod)) {
					int flags= method.getFlags();
					if (!Flags.isPrivate(flags) || (!Flags.isStatic(flags))) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
