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

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;


/**
 * The FinalConditionsChecker class provides static methods to check various final conditions for a
 * refactoring.
 */
public class FinalConditionsChecker {

	/**
	 * Represents the status for the calculation of changes in a refactoring operation.
	 */
	private RefactoringStatus fStatus;

	/**
	 * Creates a new FinalConditionsChecker with the provided RefactoringStatus. This class is
	 * responsible for performing final checks and conditions during the refactoring process, using
	 * the specified RefactoringStatus instance to report any issues or errors that may occur.
	 *
	 * @param status The RefactoringStatus object used to track and report the status of the
	 *            refactoring process.
	 */
	public FinalConditionsChecker(RefactoringStatus status) {
		fStatus= status;
	}

	/**
	 * Gets the current RefactoringStatus. The RefactoringStatus is used to track and report the
	 * status of the refactoring process, including any issues or errors that occurred during
	 * various stages of the refactoring.
	 *
	 * @return The RefactoringStatus object used to track and report the status of the refactoring
	 *         process.
	 */
	public RefactoringStatus getStatus() {
		return fStatus;
	}

	/**
	 * Checks if a duplicate method with the same signature as the refactored method exists. The
	 * MakeStaticRefactoring introduces a new parameter if fields or instance methods are used in
	 * the body of the selected method. This check ensures that there is no conflict with the
	 * signature of another method.
	 *
	 * @param methodDeclaration the MethodDeclaration to be checked
	 *
	 * @param imethod the IMethod representing the method declaration
	 *
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public void checkMethodIsNotDuplicate(MethodDeclaration methodDeclaration, IMethod imethod) throws JavaModelException {
		int parameterAmount= methodDeclaration.parameters().size() + 1;
		String methodName= methodDeclaration.getName().getIdentifier();
		IMethodBinding methodBinding= methodDeclaration.resolveBinding();
		ITypeBinding typeBinding= methodBinding.getDeclaringClass();
		IType type= (IType) typeBinding.getJavaElement();

		IMethod method= Checks.findMethod(methodName, parameterAmount, false, type);

		if (method == null) {
			return;
		}

		//check if parameter types match (also compare new parameter that is added by refactoring)
		String className= ((TypeDeclaration) methodDeclaration.getParent()).getName().toString();
		String extendedClassName= "Q" + className + ";"; //$NON-NLS-1$ //$NON-NLS-2$
		boolean contains;
		String[] paramTypesOfFoundMethod= method.getParameterTypes();
		String[] paramTypesOfSelectedMethodExtended= new String[parameterAmount];
		paramTypesOfSelectedMethodExtended[0]= extendedClassName;
		String[] paramTypesOfSelectedMethod= imethod.getParameterTypes();

		for (int parameterNumber= 0; parameterNumber < paramTypesOfSelectedMethod.length; parameterNumber++) {
			paramTypesOfSelectedMethodExtended[parameterNumber + 1]= paramTypesOfSelectedMethod[parameterNumber];
		}

		for (int parameterNumber= 0; parameterNumber < paramTypesOfFoundMethod.length; parameterNumber++) {
			contains= paramTypesOfSelectedMethodExtended[parameterNumber].equals(paramTypesOfFoundMethod[parameterNumber]);
			if (!contains) {
				return;
			}
		}
		fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_duplicate_method_signature));
	}

	/**
	 * Checks if a method that overrides a method of parent type will not hide the parent method
	 * after MakeStaticRefactoring.
	 *
	 * @param methodhasInstanceUsage indicates if the method has any instance usage
	 * @param iMethod the IMethod to be checked
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public void checkMethodWouldHideParentMethod(boolean methodhasInstanceUsage, IMethod iMethod) throws JavaModelException {
		if (!methodhasInstanceUsage && isOverriding(iMethod.getDeclaringType(), iMethod)) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_hiding_method_of_parent_type));
		}
	}


	/**
	 * Checks if the bound does not contains a wildcard type.
	 *
	 * @param bound the bound to be checked
	 */
	public void checkBoundNotContainingWildCardType(String bound) {
		if (bound.contains("?")) { //$NON-NLS-1$
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_wildCardTypes_as_bound));
		}
	}

	/**
	 * Checks if the method reference is not referring to the specified method.
	 *
	 * @param methodReference the MethodReference to be checked
	 * @param targetMethodBinding the IMethodBinding representing the target method
	 */
	public void checkMethodReferenceNotReferingToMethod(MethodReference methodReference, IMethodBinding targetMethodBinding) {
		IMethodBinding methodReferenceBinding= methodReference.resolveMethodBinding();
		ITypeBinding typeBindingOfMethodReference= methodReferenceBinding.getDeclaringClass();
		ITypeBinding typeBindingOfTargetMethod= targetMethodBinding.getDeclaringClass();
		if (targetMethodBinding.isEqualTo(methodReferenceBinding) && typeBindingOfMethodReference.isEqualTo(typeBindingOfTargetMethod)) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
		}
	}

	/**
	 * Checks if the current node represents a super method invocation and reports it as a fatal
	 * error. This method is used during the static method refactoring process to ensure that there
	 * are no explicit super method invocations in the code, as a static method cannot reference the
	 * 'super' keyword. If a super method invocation is found, it will be reported as a fatal error
	 * using the provided status.
	 */
	public void checkNodeIsNoSuperMethodInvocation() {
		fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_explicit_super_method_invocation));
	}

	/**
	 * Checks if the selected method uses a 'super' field access and reports it as a warning.
	 *
	 * @param parent The ASTNode representing the parent of the current node being checked.
	 */
	public void checkMethodNotUsingSuperFieldAccess(ASTNode parent) {
		if (parent instanceof SuperFieldAccess) {
			fStatus.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.MakeStaticRefactoring_selected_method_uses_super_field_access));
		}
	}

	/**
	 * Checks if the selected method is not recursive and reports it as a fatal error if it is.
	 *
	 * @param node The SimpleName node representing the method name being checked.
	 * @param methodDeclaration The MethodDeclaration node representing the selected method being
	 *            checked.
	 */
	public void checkIsNotRecursive(SimpleName node, MethodDeclaration methodDeclaration) {
		IMethodBinding nodeMethodBinding= (IMethodBinding) node.resolveBinding();
		IMethodBinding outerMethodBinding= methodDeclaration.resolveBinding();

		if (nodeMethodBinding.isEqualTo(outerMethodBinding)) {
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
		}
	}

	private boolean isOverriding(IType type, IMethod iMethod) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] supertypes= hierarchy.getAllSupertypes(type);
		for (IType supertype : supertypes) {
			IMethod[] methods= supertype.getMethods();
			for (IMethod method : methods) {
				if (method.isSimilar(iMethod)) {
					return true;
				}
			}
		}
		return false;
	}
}
