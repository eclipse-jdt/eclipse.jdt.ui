/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.resources.IProject;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class LaunchingPreferencePage extends PropertyAndPreferencePage {

	private JavaLaunchingConfigurationBlock fConfigurationBlock;

	public LaunchingPreferencePage() {
		setPreferenceStore(PreferenceConstants.getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer) getContainer();
		fConfigurationBlock= new JavaLaunchingConfigurationBlock(getNewStatusChangedListener(), container);

		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJavaHelpContextIds.LAUNCHING_PREFERENCE_PAGE);
	}

	@Override
	protected Control createPreferenceContent(Composite composite) {
		return fConfigurationBlock.createContents(composite);
	}

	@Override
	protected boolean hasProjectSpecificOptions(IProject project) {
		return false;
	}

	@Override
	protected String getPreferencePageID() {
		return "org.eclipse.jdt.ui.preferences.LaunchingPreferencePage"; //$NON-NLS-1$
	}

	@Override
	protected String getPropertyPageID() {
		return null;
	}

	@Override
	public void dispose() {
		if (fConfigurationBlock != null) {
			fConfigurationBlock.dispose();
		}
		super.dispose();
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		if (fConfigurationBlock != null) {
			fConfigurationBlock.performDefaults();
		}
	}

	@Override
	public boolean performOk() {
		if (fConfigurationBlock != null && !fConfigurationBlock.performOk()) {
			return false;
		}
		return super.performOk();
	}

	@Override
	public void performApply() {
		if (fConfigurationBlock != null) {
			fConfigurationBlock.performApply();
		}
	}
}

