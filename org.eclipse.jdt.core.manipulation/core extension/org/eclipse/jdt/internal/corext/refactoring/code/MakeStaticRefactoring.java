/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *s
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.ChangeCalculator;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.ContextCalculator;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.ContextCalculator.SelectionInputType;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.FinalConditionsChecker;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.InitialConditionsChecker;

/**
 *
 * The {@code MakeStaticRefactoring} class represents a refactoring operation to convert a method
 * into a static method. It provides the capability to transform an instance method into a static
 * method by modifying the method declaration, updating method invocations, and handling related
 * changes.
 *
 * @since 3.30
 *
 */
public class MakeStaticRefactoring extends Refactoring {

	/**
	 * Represents the status of a refactoring operation.
	 */
	private RefactoringStatus fStatus;

	/**
	 * The context calculator is used to calculate the necessary context for the initial conditions
	 * check of the refactoring operation.
	 */
	private ContextCalculator fContextCalculator;

	/**
	 * This calculator calculates the changes that are made during the refactoring process.
	 */
	private ChangeCalculator fChangeCalculator;

	/**
	 * Constructs a new {@code MakeStaticRefactoring} object with the given parameters.
	 *
	 * @param inputAsICompilationUnit The input ICompilationUnit for the refactoring.
	 * @param selectionStart The starting position of the target selection.
	 * @param selectionLength The length of the target selection.
	 */
	public MakeStaticRefactoring(ICompilationUnit inputAsICompilationUnit, int selectionStart, int selectionLength) {
		Selection targetSelection= Selection.createFromStartLength(selectionStart, selectionLength);
		fContextCalculator= new ContextCalculator(inputAsICompilationUnit, targetSelection);
	}

	/**
	 * Constructs a new {@code MakeStaticRefactoring} object. This constructor is called when
	 * performing the refactoring on a method in the outline menu.
	 *
	 * @param method The target method the refactoring should be performed on.
	 */
	public MakeStaticRefactoring(IMethod method) {
		fContextCalculator= new ContextCalculator(method);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return The name of the refactoring operation.
	 */
	@Override
	public String getName() {
		return RefactoringCoreMessages.MakeStaticRefactoring_name;
	}

	/**
	 * Checks the initial conditions required for the refactoring process.
	 *
	 * @param progressMonitor The progress monitor to report the progress of the operation.
	 * @return The refactoring status indicating the result of the initial conditions check.
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor progressMonitor) throws JavaModelException {
		fStatus= new RefactoringStatus();
		SubMonitor progress= SubMonitor.convert(progressMonitor, RefactoringCoreMessages.MakeStaticRefactoring_checking_preconditions, 10);

		SelectionInputType selectionInputType= fContextCalculator.getSelectionInputType();
		InitialConditionsChecker checker= new InitialConditionsChecker(fStatus);

		if (selectionInputType == SelectionInputType.TEXT_SELECTION) {
			if (!checker.checkValidTextSelectionStart(fContextCalculator.getSelectionEditorText())) {
				return fStatus;
			}
			if (!checker.checkValidICompilationUnit(fContextCalculator.getSelectionICompilationUnit())) {
				return fStatus;
			}
			if (!checker.checkASTNodeIsValidMethod(fContextCalculator.getOrComputeSelectionASTNode())) {
				return fStatus;
			}
		}
		progress.split(3);

		if (!checker.checkSourceAvailable(fContextCalculator.getOrComputeTargetIMethod())) {
			return fStatus;
		}
		progress.split(1);

		if (!checker.checkValidIMethod(fContextCalculator.getOrComputeTargetIMethod())) {
			return fStatus;
		}
		progress.split(1);

		if (!checker.checkValidICompilationUnit(fContextCalculator.getOrComputeTargetICompilationUnit())) {
			return fStatus;
		}
		progress.split(1);

		if (!checker.checkMethodIsNotConstructor(fContextCalculator.getOrComputeTargetIMethod())) {
			return fStatus;
		}
		progress.split(1);

		if (!checker.checkMethodNotInLocalOrAnonymousClass(fContextCalculator.getOrComputeTargetIMethod())) {
			return fStatus;
		}
		progress.split(1);

		if (!checker.checkMethodNotStatic(fContextCalculator.getOrComputeTargetIMethod())) {
			return fStatus;
		}
		progress.split(1);

		if (!checker.checkMethodNotOverridden(fContextCalculator.getOrComputeTargetIMethod())) {
			return fStatus;
		}
		progress.split(1);

		return fStatus;
	}

	/**
	 * Checks the final conditions required for the refactoring process.
	 *
	 * @param progressMonitor The progress monitor to report the progress of the operation.
	 * @return The refactoring status indicating the result of the final conditions check.
	 * @throws CoreException if an error occurs during the final conditions check.
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor progressMonitor) throws CoreException {
		SubMonitor progress= SubMonitor.convert(progressMonitor, RefactoringCoreMessages.MakeStaticRefactoring_checking_conditions, 8);
		fStatus = new RefactoringStatus();
		FinalConditionsChecker checker= new FinalConditionsChecker(fStatus);
		fChangeCalculator= new ChangeCalculator(fContextCalculator.getOrComputeTargetMethodDeclaration(), fContextCalculator.getOrComputeTargetIMethod(), checker);
		progress.split(1);

		fChangeCalculator.modifyMethodDeclaration();
		progress.split(6);

		//Find and modify MethodInvocations
		fChangeCalculator.handleMethodInvocations(progress.slice(1), fContextCalculator.getOrComputeTargetIMethodBinding());
		progress.split(1);
		return fStatus;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Creates a {@code Change} object representing the refactoring changes to be performed. This
	 * method constructs a composite change that encapsulates all the individual changes managed by
	 * the {@code fChangeManager}.
	 *
	 * @param progressMonitor The progress monitor used to track the progress of the operation.
	 * @return A {@code Change} object representing all refactoring changes.
	 * @throws CoreException If an error occurs during the creation of the change.
	 * @throws OperationCanceledException If the operation is canceled by the user.
	 */
	@Override
	public Change createChange(IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException {
		SubMonitor progress= SubMonitor.convert(progressMonitor, RefactoringCoreMessages.MakeStaticRefactoring_creating_changes, 1);

		CompositeChange multiChange= new CompositeChange(RefactoringCoreMessages.MakeStaticRefactoring_name, fChangeCalculator.getOrComputeChanges());
		progress.split(1);

		return multiChange;
	}
}
