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
package org.eclipse.jdt.internal.junit.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

public abstract class JUnitRenameParticipant extends RenameParticipant implements IChangeAdder {

	static public class ChangeList {

		private final RenameArguments fArguments;

		private List fChanges;

		private final ILaunchManager fLaunchManager;

		private boolean fShouldFlagWarnings= true;

		public ChangeList(RenameArguments arguments, ILaunchManager manager, List changes) {
			fArguments= arguments;
			fLaunchManager= manager;
			fChanges= changes;
		}

		public void addChange(Change change) {
			if (change != null) {
				fChanges.add(change);
				fShouldFlagWarnings= false;
			}
		}

		public void createChangeForConfigs(ILaunchConfiguration[] configs, IChangeAdder changeCreator) throws CoreException {
			for (int i= 0; i < configs.length; i++) {
				fShouldFlagWarnings= true;
				changeCreator.createChangeForConfig(this, new LaunchConfigurationContainer(configs[i]));
			}
		}

		public boolean shouldFlagWarnings() {
			return fShouldFlagWarnings;
		}

		public void addRenameChangeIfNeeded(LaunchConfigurationContainer config, String oldName) {
			if (config.getName().equals(oldName)) {
				LaunchConfigRenameChange renameChange= new LaunchConfigRenameChange(config, fArguments.getNewName(), fLaunchManager, shouldFlagWarnings());
				addChange(renameChange);
			}
		}

		public void addAttributeChangeIfNeeded(LaunchConfigurationContainer config, String attributeName, String oldValue, String newValue) throws CoreException {
			String currentValue= config.getAttribute(attributeName, (String) null);
			if (currentValue != null && oldValue.equals(currentValue)) {
				addAttributeChange(config, attributeName, newValue);
			}
		}

		public void addAttributeChange(LaunchConfigurationContainer config, String attributeName, String newValue) {
			addChange(new LaunchConfigSetAttributeChange(config, attributeName, newValue, shouldFlagWarnings()));
		}
	}

	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		if (!getArguments().getUpdateReferences())
			return null;

		ILaunchManager manager= getLaunchManager();
		List launchConfigTypes= getLaunchConfigTypes();
		List changes= new ArrayList();
		for (Iterator types= launchConfigTypes.iterator(); types.hasNext();) {
			String typeId= (String) types.next();
			ILaunchConfigurationType type= manager.getLaunchConfigurationType(typeId);
			ILaunchConfiguration configs[]= manager.getLaunchConfigurations(type);
			new ChangeList(getArguments(), getLaunchManager(), changes).createChangeForConfigs(configs, this);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		if (changes.size() > 0)
			return new CompositeChange(getChangeName(), (Change[]) changes.toArray(new Change[changes.size()]));
		return null;
	}

	// @see
	// org.eclipse.jdt.internal.junit.refactoring.IChangeAdder#createChangeForConfig(org.eclipse.jdt.internal.junit.refactoring.JUnitRenameParticipant.ChangeList,
	// org.eclipse.debug.core.ILaunchConfiguration)
	public abstract void createChangeForConfig(ChangeList list, LaunchConfigurationContainer config) throws CoreException;

	protected String getChangeName() {
		return JUnitMessages.TypeRenameParticipant_change_name;
	}

	protected List getLaunchConfigTypes() {
		return JUnitPlugin.getDefault().getJUnitLaunchConfigTypeIDs();
	}

	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	protected String getNewName() {
		return getArguments().getNewName();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return JUnitMessages.TypeRenameParticipant_name;
	}
}
