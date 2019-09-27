/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - [CodeMining] Provide Java References/Implementation CodeMinings - Bug 529127
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.resources.IProject;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Code mining preference page.
 * <p>
 * Note: Must be public since it is referenced from plugin.xml
 * </p>
 *
 * @since 3.16
 */
public class JavaEditorCodeMiningPreferencePage extends PropertyAndPreferencePage {

	private JavaEditorCodeMiningConfigurationBlock fConfigurationBlock;

	public JavaEditorCodeMiningPreferencePage() {
		setPreferenceStore(PreferenceConstants.getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer) getContainer();
		fConfigurationBlock= new JavaEditorCodeMiningConfigurationBlock(getNewStatusChangedListener(), container);

		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJavaHelpContextIds.CODE_MINING_PREFERENCE_PAGE);
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
		return "org.eclipse.jdt.ui.preferences.JavaEditorCodeMiningPreferencePage"; //$NON-NLS-1$
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
