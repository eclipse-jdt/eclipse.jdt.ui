/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

/**
 * Refactoring processor to replace type occurrences by a super type
 */
public final class UseSuperTypeProcessor extends SuperTypeRefactoringProcessor {

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.useSuperTypeProcessor"; //$NON-NLS-1$

	/** The text change manager */
	private TextChangeManager fChangeManager= null;

	/** The subtype to replace */
	private final IType fSubType;

	/** The supertype as replacement */
	private final IType fSuperType;

	/**
	 * Creates a new super type processor.
	 * 
	 * @param subType the subtype to replace its occurrences
	 * @param superType the supertype as replacement
	 */
	public UseSuperTypeProcessor(final IType subType, final IType superType) {
		Assert.isNotNull(subType);
		Assert.isNotNull(superType);
		fSubType= subType;
		fSuperType= superType;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(context);
		final RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextChangeManager();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("UseSuperTypeProcessor.checking")); //$NON-NLS-1$
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), status);
			if (!status.hasFatalError())
				status.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext()));
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("UseSuperTypeProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fSubType));
			if (!status.hasError()) {
				if (Checks.isException(fSubType, new SubProgressMonitor(monitor, 1)))
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("UseSuperTypeProcessor.exception"))); //$NON-NLS-1$
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.creating")); //$NON-NLS-1$
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.name"), fChangeManager.getAllChanges());//$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the text change manager for this processor.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param status the refactoring status
	 * @return the created text change manager
	 * @throws JavaModelException if the method declaration could not be found
	 * @throws CoreException if the changes could not be generated
	 */
	protected TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException, CoreException {
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("UseSuperTypeProcessor.creating")); //$NON-NLS-1$
			final TextChangeManager manager= new TextChangeManager();
			ExtractInterfaceUtil.updateReferences(manager, fSubType, fSuperType, fOwner, true, new SubProgressMonitor(monitor, 1), status);
			return manager;
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public final Object[] getElements() {
		return new Object[] { fSubType};
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public final String getIdentifier() {
		return IDENTIFIER;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public final String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString("UseSuperTypeProcessor.name", new String[] { fSubType.getElementName(), fSuperType.getElementName()}); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public final boolean isApplicable() throws CoreException {
		return Checks.isAvailable(fSubType) && Checks.isAvailable(fSuperType) && !fSubType.isAnonymous() && !fSubType.isAnnotation() && !fSuperType.isAnonymous() && !fSuperType.isAnnotation() && !fSuperType.isEnum();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#loadParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus,org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	public final RefactoringParticipant[] loadParticipants(final RefactoringStatus status, final SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor#rewriteTypeOccurrences(org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager, org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit, java.util.Set)
	 */
	protected void rewriteTypeOccurrences(final TextChangeManager manager, final CompilationUnitRewrite subRewrite, final ICompilationUnit unit, final CompilationUnit node, final Set replacements) {
		// TODO implement
	}
}
