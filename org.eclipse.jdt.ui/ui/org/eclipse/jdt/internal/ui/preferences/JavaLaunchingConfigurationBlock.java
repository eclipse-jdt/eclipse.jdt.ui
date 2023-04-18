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

import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class JavaLaunchingConfigurationBlock extends OptionsConfigurationBlock {

	// Preference store keys

	// --------------------- General

	private static final Key PREF_APPLICATION_NAME_FULLY_QUALIFIED_ENABLED= getJDTUIKey(
			PreferenceConstants.LAUNCH_NAME_FULLY_QUALIFIED_FOR_APPLICATION);
	private static final Key PREF_APPLET_NAME_FULLY_QUALIFIED_ENABLED= getJDTUIKey(
			PreferenceConstants.LAUNCH_NAME_FULLY_QUALIFIED_FOR_APPLET);
	private static final Key PREF_JUNIT_TESTS_NAME_FULLY_QUALIFIED_ENABLED= getJDTUIKey(
			PreferenceConstants.LAUNCH_NAME_FULLY_QUALIFIED_FOR_JUNIT_TEST);

	private static final String[] TRUE_FALSE= new String[] { "true", "false" }; //$NON-NLS-1$ //$NON-NLS-2$

	public static Key[] getAllKeys() {
		return new Key[] { PREF_APPLICATION_NAME_FULLY_QUALIFIED_ENABLED,
				PREF_APPLET_NAME_FULLY_QUALIFIED_ENABLED,
				PREF_JUNIT_TESTS_NAME_FULLY_QUALIFIED_ENABLED };
	}

	private Button fEnableApplicationNameFullyQualified;
	private Button fEnableAppletNameFullyQualified;
	private Button fEnableJUnitNameFullyQualified;

	public JavaLaunchingConfigurationBlock(IStatusChangeListener context,
			IWorkbenchPreferenceContainer container) {
		super(context, null, getAllKeys(), container);
	}

	@Override
	protected Control createContents(Composite parent) {
		Shell shell= parent.getShell();
		setShell(shell);

		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.marginHeight= 5;
		layout.marginWidth= 5;
		mainComp.setLayout(layout);

		Group group= new Group(mainComp, SWT.NONE);
		group.setText(PreferencesMessages.JavaLaunchingConfigurationBlock_name_description);
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= true;
		group.setLayoutData(gd);
		GridLayout groupLayout= new GridLayout();
		groupLayout.marginHeight= 5;
		groupLayout.marginWidth= 5;
		group.setLayout(groupLayout);

		fEnableApplicationNameFullyQualified= addCheckBox(group,
				PreferencesMessages.JavaLaunchingConfigurationBlock_application_name_fully_qualified_label,
				PREF_APPLICATION_NAME_FULLY_QUALIFIED_ENABLED, TRUE_FALSE, LayoutUtil.getIndent());

		fEnableAppletNameFullyQualified= addCheckBox(group,
				PreferencesMessages.JavaLaunchingConfigurationBlock_applet_name_fully_qualified_label,
				PREF_APPLET_NAME_FULLY_QUALIFIED_ENABLED, TRUE_FALSE, LayoutUtil.getIndent());

		fEnableJUnitNameFullyQualified= addCheckBox(group,
				PreferencesMessages.JavaLaunchingConfigurationBlock_junit_name_fully_qualified_label,
				PREF_JUNIT_TESTS_NAME_FULLY_QUALIFIED_ENABLED, TRUE_FALSE, LayoutUtil.getIndent());

		fEnableApplicationNameFullyQualified.setEnabled(true);
		fEnableAppletNameFullyQualified.setEnabled(true);
		fEnableJUnitNameFullyQualified.setEnabled(true);
		return mainComp;
	}

	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		// do nothing
	}

	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}

}
