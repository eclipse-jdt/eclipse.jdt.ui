/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.util.Resources;

import org.eclipse.jdt.ui.refactoring.IRefactoringProcessorIds;

public final class JavaMoveProcessor extends MoveProcessor implements IQualifiedNameUpdating, IReorgDestinationValidator {

	private MonitoringCreateTargetQueries fCreateTargetQueries;

	private IMovePolicy fMovePolicy;

	private IReorgQueries fReorgQueries;

	private boolean fWasCanceled;

	public JavaMoveProcessor(IMovePolicy policy) {
		fMovePolicy= policy;
	}

	public JavaMoveProcessor(JavaRefactoringArguments arguments, RefactoringStatus status) {
		RefactoringStatus initializeStatus= initialize(arguments);
		status.merge(initializeStatus);
	}

	@Override
	public boolean canChildrenBeDestinations(IReorgDestination destination) {
		return fMovePolicy.canChildrenBeDestinations(destination);
	}

	@Override
	public boolean canElementBeDestination(IReorgDestination destination) {
		return fMovePolicy.canElementBeDestination(destination);
	}

	@Override
	public boolean canEnableQualifiedNameUpdating() {
		return fMovePolicy.canEnableQualifiedNameUpdating();
	}

	public boolean canUpdateQualifiedNames() {
		return fMovePolicy.canUpdateQualifiedNames();
	}

	/**
	 * Checks if <b>Java</b> references to the selected element(s) can be updated if moved to
	 * the selected destination. Even if <code>false</code>, participants could still update
	 * non-Java references.
	 *
	 * @return <code>true</code> iff <b>Java</b> references to the moved element can be updated
	 * @since 3.5
	 */
	public boolean canUpdateJavaReferences() {
		return fMovePolicy.canUpdateJavaReferences();
	}

	/**
	 * DO NOT REMOVE, used in a product, see https://bugs.eclipse.org/299631 .
	 * @return <code>true</code> iff <b>Java</b> references to the moved element can be updated
	 * @deprecated since 3.5, replaced by {@link #canUpdateJavaReferences()}
	 */
	@Deprecated
	public boolean canUpdateReferences() {
		return canUpdateJavaReferences();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try {
			Assert.isNotNull(fReorgQueries);
			fWasCanceled= false;
			return fMovePolicy.checkFinalConditions(pm, context, fReorgQueries);
		} catch (OperationCanceledException e) {
			fWasCanceled= true;
			throw e;
		}
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();
			IResource[] resources= ReorgUtils.getNotNulls(fMovePolicy.getResources());
			IStatus status= Resources.checkInSync(resources);
			if (!status.isOK()) {
				boolean autoRefresh= Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false, null);
				if (autoRefresh) {
					for (IResource resource : resources) {
						try {
							resource.refreshLocal(IResource.DEPTH_INFINITE, pm);
						} catch (CoreException e) {
							break;
						}
						status= Resources.checkInSync(resources);
					}
				}
			}
			result.merge(RefactoringStatus.create(status));
			IResource[] javaResources= ReorgUtils.getResources(fMovePolicy.getJavaElements());
			resources= ReorgUtils.getNotNulls(javaResources);
			status= Resources.checkInSync(resources);
			if (!status.isOK()) {
				boolean autoRefresh= Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false, null);
				if (autoRefresh) {
					for (IResource resource : resources) {
						try {
							resource.refreshLocal(IResource.DEPTH_INFINITE, pm);
						} catch (CoreException e) {
							break;
						}
						status= Resources.checkInSync(resources);
					}
				}
			}
			result.merge(RefactoringStatus.create(status));
			return result;
		} finally {
			pm.done();
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		Assert.isTrue(fMovePolicy.getJavaElementDestination() == null || fMovePolicy.getResourceDestination() == null);
		Assert.isTrue(fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null);
		try {
			final DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.JavaMoveProcessor_change_name) {

				@Override
				public ChangeDescriptor getDescriptor() {
					return fMovePolicy.getDescriptor();
				}

				@Override
				public Change perform(IProgressMonitor pm2) throws CoreException {
					Change change= super.perform(pm2);
					for (Change c : getChildren()) {
						if (!(c instanceof TextEditBasedChange)) {
							return null;
						}
					}
					return change;
				}
			};
			CreateTargetExecutionLog log= null;
			if (fCreateTargetQueries != null) {
				final MonitoringCreateTargetQueries queries= fCreateTargetQueries;
				final ICreateTargetQueries delegate= queries.getDelegate();
				if (delegate instanceof LoggedCreateTargetQueries)
					log= queries.getCreateTargetExecutionLog();
			}
			if (log != null) {
				for (Object element : log.getSelectedElements()) {
					result.add(new LoggedCreateTargetChange(element, fCreateTargetQueries));
				}
			}
			Change change= fMovePolicy.createChange(pm);
			if (change instanceof CompositeChange) {
				CompositeChange subComposite= (CompositeChange) change;
				result.merge(subComposite);
			} else {
				result.add(change);
			}
			return result;
		} finally {
			pm.done();
		}
	}

	private String[] getAffectedProjectNatures() throws CoreException {
		String[] jNatures= JavaProcessors.computeAffectedNaturs(fMovePolicy.getJavaElements());
		String[] rNatures= ResourceProcessors.computeAffectedNatures(fMovePolicy.getResources());
		Set<String> result= new HashSet<>(Arrays.asList(jNatures));
		result.addAll(Arrays.asList(rNatures));
		return result.toArray(new String[result.size()]);
	}

	public Object getCommonParentForInputElements() {
		return new ParentChecker(fMovePolicy.getResources(), fMovePolicy.getJavaElements()).getCommonParent();
	}

	public ICreateTargetQuery getCreateTargetQuery() {
		return fMovePolicy.getCreateTargetQuery(fCreateTargetQueries);
	}

	protected Object getDestination() {
		IJavaElement je= fMovePolicy.getJavaElementDestination();
		if (je != null)
			return je;
		return fMovePolicy.getResourceDestination();
	}

	@Override
	public Object[] getElements() {
		List<IAdaptable> result= new ArrayList<>();
		result.addAll(Arrays.asList(fMovePolicy.getJavaElements()));
		result.addAll(Arrays.asList(fMovePolicy.getResources()));
		return result.toArray();
	}

	@Override
	public String getFilePatterns() {
		return fMovePolicy.getFilePatterns();
	}

	@Override
	public String getIdentifier() {
		return IRefactoringProcessorIds.MOVE_PROCESSOR;
	}

	public IJavaElement[] getJavaElements() {
		return fMovePolicy.getJavaElements();
	}

	@Override
	public String getProcessorName() {
		return RefactoringCoreMessages.MoveRefactoring_0;
	}

	public IResource[] getResources() {
		return fMovePolicy.getResources();
	}

	@Override
	public boolean getUpdateQualifiedNames() {
		return fMovePolicy.getUpdateQualifiedNames();
	}

	public boolean getUpdateReferences() {
		return fMovePolicy.getUpdateReferences();
	}

	public boolean hasAllInputSet() {
		return fMovePolicy.hasAllInputSet();
	}

	public boolean hasDestinationSet() {
		return fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null;
	}

	private RefactoringStatus initialize(JavaRefactoringArguments arguments) {
		setReorgQueries(new NullReorgQueries());
		final RefactoringStatus status= new RefactoringStatus();
		fMovePolicy= ReorgPolicyFactory.createMovePolicy(status, arguments);
		if (fMovePolicy != null && !status.hasFatalError()) {
			final CreateTargetExecutionLog log= ReorgPolicyFactory.loadCreateTargetExecutionLog(arguments);
			if (log != null && !status.hasFatalError()) {
				fMovePolicy.setDestinationCheck(false);
				fCreateTargetQueries= new MonitoringCreateTargetQueries(new LoggedCreateTargetQueries(log), log);
			}
			status.merge(fMovePolicy.initialize(arguments));
		}
		return status;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return fMovePolicy.canEnable();
	}

	public boolean isTextualMove() {
		return fMovePolicy.isTextualMove();
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		return fMovePolicy.loadParticipants(status, this, getAffectedProjectNatures(), shared);
	}

	@Override
	public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
		return fMovePolicy.postCreateChange(participantChanges, pm);
	}

	public void setCreateTargetQueries(ICreateTargetQueries queries) {
		Assert.isNotNull(queries);
		fCreateTargetQueries= new MonitoringCreateTargetQueries(queries, fMovePolicy.getCreateTargetExecutionLog());
	}

	public RefactoringStatus setDestination(IReorgDestination destination) throws JavaModelException {
		fMovePolicy.setDestination(destination);
		return fMovePolicy.verifyDestination(destination);
	}

	@Override
	public void setFilePatterns(String patterns) {
		fMovePolicy.setFilePatterns(patterns);
	}

	public void setReorgQueries(IReorgQueries queries) {
		Assert.isNotNull(queries);
		fReorgQueries= queries;
	}

	@Override
	public void setUpdateQualifiedNames(boolean update) {
		fMovePolicy.setUpdateQualifiedNames(update);
	}

	public void setUpdateReferences(boolean update) {
		fMovePolicy.setUpdateReferences(update);
	}

	public boolean wasCanceled() {
		return fWasCanceled;
	}

	public int getSaveMode() {
		return fMovePolicy.getSaveMode();
	}
}
