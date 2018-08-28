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
package org.eclipse.jdt.internal.ui.preferences;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferencePageContainer;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Abstract preference and property page which is used to wrap a
 * {@link org.eclipse.jdt.internal.ui.preferences.IPreferenceAndPropertyConfigurationBlock}.
 *
 * @since 3.3
 */
public abstract class AbstractConfigurationBlockPreferenceAndPropertyPage extends PropertyAndPreferencePage {

	private IPreferenceAndPropertyConfigurationBlock fConfigurationBlock;
	private PreferencesAccess fAccess;

	public AbstractConfigurationBlockPreferenceAndPropertyPage() {
	}

	/**
	 * Create a configuration block which does modify settings in <code>context</code>.
	 *
	 * @param context the context to modify
	 * @return the preference block, not null
	 */
	protected abstract IPreferenceAndPropertyConfigurationBlock createConfigurationBlock(IScopeContext context);

	protected abstract String getHelpId();

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpId());
	}

	@Override
	protected Control createPreferenceContent(Composite parent) {

		IPreferencePageContainer container= getContainer();
		IWorkingCopyManager manager;
		if (container instanceof IWorkbenchPreferenceContainer) {
			manager= ((IWorkbenchPreferenceContainer)container).getWorkingCopyManager();
		} else {
			manager= new WorkingCopyManager(); // non shared
		}
		fAccess= PreferencesAccess.getWorkingCopyPreferences(manager);
		IProject project= getProject();
		IScopeContext context;
		if (project != null) {
			context= fAccess.getProjectScope(project);
		} else {
			context= fAccess.getInstanceScope();
		}

		fConfigurationBlock= createConfigurationBlock(context);

		Control content= fConfigurationBlock.createControl(parent);

		fConfigurationBlock.initialize();

		Dialog.applyDialogFont(content);
		return content;
	}

	@Override
	public boolean performOk() {
		fConfigurationBlock.performOk();

		try {
	        fAccess.applyChanges();
        } catch (BackingStoreException e) {
	        JavaPlugin.log(e);
        }

		return true;
	}

	@Override
	public void performDefaults() {
		fConfigurationBlock.performDefaults();
		super.performDefaults();
	}

	@Override
	public void dispose() {
		fConfigurationBlock.dispose();
		super.dispose();
	}

	@Override
	protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
		super.enableProjectSpecificSettings(useProjectSpecificSettings);
		if (useProjectSpecificSettings) {
			fConfigurationBlock.enableProjectSettings();
		} else {
			fConfigurationBlock.disableProjectSettings();
		}
	}
}
