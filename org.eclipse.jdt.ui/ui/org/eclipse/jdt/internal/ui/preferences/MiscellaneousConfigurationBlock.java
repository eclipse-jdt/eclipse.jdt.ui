/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;

public class MiscellaneousConfigurationBlock extends OptionsConfigurationBlock {

	// Preference store keys

	// --------------------- General

	private static final Key PREF_OPEN_TYPE_DEFAULT_WILDCARD_BETWEEN_CAMEL_CASE_PARTS_ENABLED= getJDTUIKey(
			PreferenceConstants.OPEN_TYPE_DEFAULT_WILDCARD_BETWEEN_CAMEL_CASE_PARTS);

	private static final String[] TRUE_FALSE= new String[] { "true", "false" }; //$NON-NLS-1$ //$NON-NLS-2$

	public static Key[] getAllKeys() {
		return new Key[] { PREF_OPEN_TYPE_DEFAULT_WILDCARD_BETWEEN_CAMEL_CASE_PARTS_ENABLED,
				};
	}

	private Button fEnableWildcardsBetweenCamelCaseForOpenType;

	public MiscellaneousConfigurationBlock(IStatusChangeListener context,
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
		group.setText(PreferencesMessages.MiscellaneousConfigurationBlock_open_type_name);
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= true;
		group.setLayoutData(gd);
		GridLayout groupLayout= new GridLayout();
		groupLayout.marginHeight= 5;
		groupLayout.marginWidth= 5;
		group.setLayout(groupLayout);

		fEnableWildcardsBetweenCamelCaseForOpenType= addCheckBox(group,
				PreferencesMessages.MiscellaneousConfigurationBlock_default_wildcard_between_camel_case_parts_label,
				PREF_OPEN_TYPE_DEFAULT_WILDCARD_BETWEEN_CAMEL_CASE_PARTS_ENABLED, TRUE_FALSE, LayoutUtil.getIndent());

		fEnableWildcardsBetweenCamelCaseForOpenType.setEnabled(true);
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
