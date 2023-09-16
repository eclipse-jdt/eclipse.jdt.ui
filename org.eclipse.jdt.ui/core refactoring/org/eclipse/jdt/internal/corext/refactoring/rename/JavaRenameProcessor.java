/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;

import org.eclipse.jdt.ui.refactoring.IRefactoringSaveModes;


public abstract class JavaRenameProcessor extends RenameProcessor implements INameUpdating {

	private String fNewElementName;
	private RenameModifications fRenameModifications;

	@Override
	public final RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		return fRenameModifications.loadParticipants(status, this, getAffectedProjectNatures(), shared);
	}

	@Override
	public final RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		ResourceChangeChecker checker= context.getChecker(ResourceChangeChecker.class);
		IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();
		RefactoringStatus result= doCheckFinalConditions(pm, context);
		if (result.hasFatalError())
			return result;
		for (IFile f : getChangedFiles()) {
			deltaFactory.change(f);
		}
		fRenameModifications= computeRenameModifications();
		fRenameModifications.buildDelta(deltaFactory);
		fRenameModifications.buildValidateEdits(context.getChecker(ValidateEditChecker.class));
		return result;
	}

	protected abstract RenameModifications computeRenameModifications() throws CoreException;

	protected abstract RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException;

	protected abstract IFile[] getChangedFiles() throws CoreException;

	protected abstract String[] getAffectedProjectNatures() throws CoreException;

	@Override
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewElementName= newName;
	}

	@Override
	public String getNewElementName() {
		return fNewElementName;
	}

	/**
	 * @return a save mode from {@link IRefactoringSaveModes}
	 *
	 * @see IRefactoringSaveModes
	 */
	public abstract int getSaveMode();

}
