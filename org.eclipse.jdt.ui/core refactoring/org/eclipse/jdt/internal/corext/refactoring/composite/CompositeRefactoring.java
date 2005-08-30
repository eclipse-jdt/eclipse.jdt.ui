/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.composite;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.CompositeTextFileChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Default implementation of a composite refactoring.
 * 
 * @since 3.2
 */
public class CompositeRefactoring extends Refactoring {

	/** The global working copy owner */
	private static class GlobalWorkingCopyOwner extends WorkingCopyOwner {
		// No further members
	}

	/** The local working copy owner */
	private static class LocalWorkingCopyOwner extends WorkingCopyOwner {
		// No further members
	}

	/**
	 * The created changes (element type: &lt;<code>ICompilationUnit</code>,
	 * <code>CompositeTextFileChange</code>&gt;)
	 */
	private final Map fChanges= new HashMap();

	/** The set of custom changes (element type: <code>Change</code>) */
	private final Set fCustomChanges= new HashSet();

	/** The set of disabled composable refactorings (element type: &lt;<code>Refactoring</code>&gt;) */
	private final Set fDisabledRefactorings= new HashSet(2);

	/** The global working copy owner */
	private WorkingCopyOwner fGlobalOwner= null;

	/**
	 * The local working copy owners (element type: &lt;<code>Refactoring</code>,
	 * <code>WorkingCopyOwner</code>&gt;)
	 */
	private final Map fLocalOwners= new HashMap();

	/** The name of the refactoring */
	private final String fName;

	/**
	 * Should initial conditions of the first enabled refactoring be checked
	 * again during <code>checkFinalConditions</code>?
	 */
	private boolean fRecheckInitialConditions= false;

	/**
	 * The refactoring setups (element type: &lt;<code>Refactoring</code>,
	 * <code>RefactoringArguments</code>&gt;)
	 */
	private final Map fRefactoringArguments= new HashMap();

	/**
	 * The composable refactorings (must all implement
	 * {@link IComposableRefactoring})
	 */
	private final Refactoring[] fRefactorings;

	/**
	 * The working copies (element type: &lt;<code>ICompilationUnit</code>,
	 * <code>ICompilationUnit</code>&gt;)
	 */
	private final Map fWorkingCopies= new HashMap();

	/**
	 * Creates a new composite refactoring.
	 * 
	 * @param name
	 *            the name of the refactoring
	 * @param refactorings
	 *            the composable refactorings must implement
	 *            {@link IComposableRefactoring}
	 */
	public CompositeRefactoring(final String name, final Refactoring[] refactorings) {
		this(name, refactorings, new RefactoringArguments[] {});
	}

	/**
	 * Creates a new composite refactoring.
	 * 
	 * @param name
	 *            the name of the refactoring
	 * @param refactorings
	 *            the composable refactorings which must implement
	 *            {@link IComposableRefactoring}
	 * @param arguments
	 *            the refactoring arguments for the composable refactorings
	 */
	public CompositeRefactoring(final String name, final Refactoring[] refactorings, final RefactoringArguments[] arguments) {

		Assert.isNotNull(name);
		Assert.isNotNull(refactorings);
		Assert.isNotNull(arguments);
		Assert.isTrue(refactorings.length > 0);
		Assert.isTrue(arguments.length == 0 || arguments.length == refactorings.length);

		fName= name;
		fRefactorings= refactorings;

		for (int index= 0; index < refactorings.length; index++) {

			final Refactoring refactoring= fRefactorings[index];
			Assert.isTrue(refactoring instanceof IComposableRefactoring);

			final IComposableRefactoring composable= (IComposableRefactoring) refactoring;

			composable.setCompositeRefactoring(this);
		}

		for (int index= 0; index < arguments.length; index++)
			setRefactoringArguments(fRefactorings[index], arguments[index]);
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		fChanges.clear();

		boolean first= true;

		monitor.beginTask("", Math.max(1, 3 * (fRefactorings.length - fDisabledRefactorings.size()) - 1)); //$NON-NLS-1$
		monitor.setTaskName(RefactoringCoreMessages.CompositeRefactoring_checking_preconditions);

		final RefactoringStatus status= new RefactoringStatus();
		try {

			for (int index= 0; index < fRefactorings.length; index++) {

				final Refactoring refactoring= fRefactorings[index];
				Assert.isTrue(refactoring instanceof IComposableRefactoring);

				if (!fDisabledRefactorings.contains(refactoring)) {
					final IComposableRefactoring composable= (IComposableRefactoring) refactoring;

					final RefactoringArguments arguments= (RefactoringArguments) fRefactoringArguments.get(refactoring);
					if (arguments != null) {

						if (!composable.initialize(arguments))
							return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.CompositeRefactoring_error_setup, refactoring.getName()));
					}

					if (!first && fRecheckInitialConditions)
						status.merge(refactoring.checkInitialConditions(new SubProgressMonitor(monitor, 1)));

					first= false;

					if (status.hasFatalError())
						return status;

					status.merge(refactoring.checkFinalConditions(new SubProgressMonitor(monitor, 1)));

					if (status.hasFatalError())
						return status;

					final Change change= refactoring.createChange(new SubProgressMonitor(monitor, 1));
					if (change != null) {

						registerChange(change);
						updateWorkingCopies(change);
					}
				}
			}

		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {

		monitor.beginTask("", 1); //$NON-NLS-1$
		monitor.setTaskName(RefactoringCoreMessages.CompositeRefactoring_checking_preconditions);

		try {

			for (int index= 0; index < fRefactorings.length; index++) {

				final Refactoring refactoring= fRefactorings[index];
				Assert.isTrue(refactoring instanceof IComposableRefactoring);

				if (!fDisabledRefactorings.contains(refactoring)) {
					final IComposableRefactoring composable= (IComposableRefactoring) refactoring;

					final RefactoringArguments arguments= (RefactoringArguments) fRefactoringArguments.get(refactoring);
					if (arguments != null) {

						if (composable.initialize(arguments))
							return refactoring.checkInitialConditions(new SubProgressMonitor(monitor, 1));
					}
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.CompositeRefactoring_error_setup, refactoring.getName()));
				}
			}

		} finally {
			monitor.done();
		}
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);

		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.CompositeRefactoring_creating_change);

			final Set changes= new HashSet(fChanges.values());
			changes.addAll(fCustomChanges);

			return new DynamicValidationStateChange(getName(), (Change[]) changes.toArray(new Change[changes.size()]));

		} finally {

			fCustomChanges.clear();
			fChanges.clear();
			fGlobalOwner= null;
			fLocalOwners.clear();

			final Collection copies= fWorkingCopies.values();

			final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
			subMonitor.beginTask(RefactoringCoreMessages.CompositeRefactoring_creating_change, copies.size());

			for (final Iterator iterator= copies.iterator(); iterator.hasNext();) {
				((ICompilationUnit) iterator.next()).discardWorkingCopy();

				subMonitor.worked(1);
			}

			subMonitor.done();
			monitor.done();
		}
	}

	/**
	 * Returns the composed refactorings.
	 * 
	 * @return the composed refactorings
	 */
	public final Refactoring[] getComposedRefactorings() {
		return fRefactorings;
	}

	/**
	 * Returns the global working copy owner.
	 * 
	 * @return the global working copy owner
	 */
	public final WorkingCopyOwner getGlobalWorkingCopyOwner() {

		if (fGlobalOwner == null)
			fGlobalOwner= new GlobalWorkingCopyOwner();

		return fGlobalOwner;
	}

	/**
	 * Returns the local working copy owner.
	 * 
	 * @param refactoring
	 *            the refactoring to get the working copy owner for
	 * @return the local working copy owner
	 */
	public final WorkingCopyOwner getLocalWorkingCopyOwner(final Refactoring refactoring) {

		final WorkingCopyOwner owner= (WorkingCopyOwner) fLocalOwners.get(refactoring);
		if (owner != null)
			return owner;

		final LocalWorkingCopyOwner local= new LocalWorkingCopyOwner();
		fLocalOwners.put(refactoring, local);

		return local;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public final String getName() {
		return fName;
	}

	/**
	 * Returns the working copy to use instead of the original compilation unit.
	 * 
	 * @param original
	 *            the original compilation unit
	 * @return the working copy
	 * @throws JavaModelException
	 *             if the working copy could not be acquired
	 */
	public final ICompilationUnit getWorkingCopy(final ICompilationUnit original) throws JavaModelException {
		Assert.isNotNull(original);

		final ICompilationUnit cached= (ICompilationUnit) fWorkingCopies.get(original);
		if (cached != null)
			return cached;

		final ICompilationUnit unit= original.getWorkingCopy(getGlobalWorkingCopyOwner(), null, null);
		fWorkingCopies.put(original, unit);

		return unit;
	}

	/**
	 * Registers a change created from a composable refactoring.
	 * 
	 * @param change
	 *            the change to register
	 */
	protected void registerChange(final Change change) {

		if (change instanceof CompositeChange) {

			final CompositeChange composite= (CompositeChange) change;
			final Change[] changes= composite.getChildren();

			for (int index= 0; index < changes.length; index++)
				registerChange(changes[index]);

		} else if (change instanceof CompilationUnitChange) {
			final CompilationUnitChange unitChange= (CompilationUnitChange) change;

			registerCompilationUnitChange(unitChange);
		}
	}

	/**
	 * Registers a compilation unit change.
	 * 
	 * @param change
	 *            the change to register
	 */
	protected void registerCompilationUnitChange(final CompilationUnitChange change) {

		final ICompilationUnit unit= change.getCompilationUnit().getPrimary();
		if (!fChanges.containsKey(unit)) {

			final CompositeTextFileChange newChange= new CompositeCompilationUnitChange(unit.getElementName(), unit);
			newChange.addChange(change);

			fChanges.put(unit, newChange);

		} else {

			final CompositeTextFileChange existingChange= (CompositeTextFileChange) fChanges.get(unit);
			if (existingChange != null)
				existingChange.addChange(change);
		}
	}

	/**
	 * Removes the refactoring arguments for the specified refactoring.
	 * 
	 * @param refactoring
	 *            the refactoring to remove its arguments
	 */
	public final void removeRefactoringArguments(final Refactoring refactoring) {
		fRefactoringArguments.remove(refactoring);
	}

	/**
	 * Determines whether initial conditions of the first enabled refactoring
	 * should be checked again during <code>checkFinalConditions</code>.
	 * 
	 * @param check
	 *            <code>true</code> to check again, <code>false</code>
	 *            otherwise
	 */
	public final void setRecheckInitialConditions(final boolean check) {
		fRecheckInitialConditions= check;
	}

	/**
	 * Sets a refactoring arguments for the specified composable refactoring.
	 * 
	 * @param refactoring
	 *            the composable refactoring
	 * @param arguments
	 *            the refactoring arguments to use for the composable
	 *            refactoring
	 */
	public final void setRefactoringArguments(final Refactoring refactoring, final RefactoringArguments arguments) {
		fRefactoringArguments.put(refactoring, arguments);
	}

	/**
	 * Determines whether the specified composable refactoring is currently
	 * enabled.
	 * 
	 * @param refactoring
	 *            the refactoring to control its enablement
	 * @param enable
	 *            <code>true</code> to enable the composable refactoring,
	 *            <code>false</code> otherwise
	 */
	public final void setRefactoringEnabled(final Refactoring refactoring, final boolean enable) {
		if (enable)
			fDisabledRefactorings.remove(refactoring);
		else
			fDisabledRefactorings.add(refactoring);
	}

	/**
	 * Updates the current working copies based on the incoming change.
	 * 
	 * @param change
	 *            the change
	 * @throws JavaModelException
	 *             if the working copy does not exist
	 */
	protected void updateWorkingCopies(final Change change) throws JavaModelException {

		if (change instanceof CompositeChange) {

			final CompositeChange composite= (CompositeChange) change;
			final Change[] changes= composite.getChildren();

			for (int index= 0; index < changes.length; index++)
				updateWorkingCopies(changes[index]);

		} else if (change instanceof CompilationUnitChange) {
			final CompilationUnitChange unitChange= (CompilationUnitChange) change;

			final TextEdit edit= unitChange.getEdit();
			if (edit != null)
				updateWorkingCopy(unitChange.getCompilationUnit(), edit.copy());
		}
	}

	/**
	 * Updates the specified working copy with the text edit.
	 * <p>
	 * The passed text edit should be a copy, since it is altered during the
	 * working copy update process.
	 * </p>
	 * 
	 * @param unit
	 *            the working copy to update
	 * @param edit
	 *            the text edit to apply
	 * @throws JavaModelException
	 *             if the working copy does not exist
	 */
	protected void updateWorkingCopy(final ICompilationUnit unit, final TextEdit edit) throws JavaModelException {

		final IDocument document= new Document(unit.getBuffer().getContents());

		try {
			edit.apply(document, 0);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}

		unit.getBuffer().setContents(document.get());
		JavaModelUtil.reconcile(unit);
	}
}