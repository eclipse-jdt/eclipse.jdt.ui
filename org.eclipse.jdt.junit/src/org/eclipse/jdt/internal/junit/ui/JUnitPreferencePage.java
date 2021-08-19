/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Sebastian Davids: sdavids@gmx.de
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.launcher.AssertionVMArg;

import org.eclipse.jdt.internal.ui.filtertable.FilterManager;
import org.eclipse.jdt.internal.ui.filtertable.JavaFilterTable;
import org.eclipse.jdt.internal.ui.filtertable.JavaFilterTable.ButtonLabel;
import org.eclipse.jdt.internal.ui.filtertable.JavaFilterTable.DialogLabels;
import org.eclipse.jdt.internal.ui.filtertable.JavaFilterTable.FilterTableConfig;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * Preference page for JUnit settings. Supports to define the failure
 * stack filter patterns.
 */
public class JUnitPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	final static FilterManager FILTER_MANAGER = new FilterManager(JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, JUnitPreferencesConstants.PREF_INACTIVE_FILTERS_LIST) {
		@Override
		protected String[] getDefaultActiveFilters(IPreferenceStore store) {
			return JUnitPreferencesConstants.createDefaultStackFiltersList();
		}

		@Override
		protected String[] getDefaultInactiveFilters(IPreferenceStore store) {
			return new String[0];
		}
	};

	private final JavaFilterTable fJavaFilterTable;
	private Button fEnableAssertionsCheckBox;
	private Button fShowInAllViewsCheckBox;

	public JUnitPreferencePage() {
		super();
		setDescription(JUnitMessages.JUnitPreferencePage_description);
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, JUnitCorePlugin.CORE_PLUGIN_ID));
		fJavaFilterTable = new JavaFilterTable(this, FILTER_MANAGER,
				new FilterTableConfig()
					.setLabelText(JUnitMessages.JUnitPreferencePage_filter_label)
					.setAddFilter(new ButtonLabel(JUnitMessages.JUnitPreferencePage_addfilterbutton_label))
					.setAddType(new ButtonLabel(JUnitMessages.JUnitPreferencePage_addtypebutton_label))
					.setAddTypeDialog(new DialogLabels(JUnitMessages.JUnitPreferencePage_addtypedialog_title, JUnitMessages.JUnitPreferencePage_addtypedialog_message))
					.setAddPackage(new ButtonLabel(JUnitMessages.JUnitPreferencePage_addpackagebutton_label))
					.setAddPackageDialog(new DialogLabels(JUnitMessages.JUnitPreferencePage_addpackagedialog_title, JUnitMessages.JUnitPreferencePage_addpackagedialog_message))
					.setRemove(new ButtonLabel(JUnitMessages.JUnitPreferencePage_removefilterbutton_label))
					.setSelectAll(new ButtonLabel(JUnitMessages.JUnitPreferencePage_enableallbutton_label))
					.setDeselectAll(new ButtonLabel(JUnitMessages.JUnitPreferencePage_disableallbutton_label))
					.setHelpContextId(IJUnitHelpContextIds.JUNIT_PREFERENCE_PAGE));
	}

	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJUnitHelpContextIds.JUNIT_PREFERENCE_PAGE);

		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		GridData data= new GridData();
		data.verticalAlignment= GridData.FILL;
		data.horizontalAlignment= GridData.FILL;
		composite.setLayoutData(data);

		createEnableAssertionsCheckbox(composite);
		createShowInAllViewsCheckbox(composite);
		createJavaFilterTable(composite);
		Dialog.applyDialogFont(composite);
		return composite;
	}

	private void createJavaFilterTable(Composite parent) {
		Composite container= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		container.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		fJavaFilterTable.createTable(container);
	}

	private void createEnableAssertionsCheckbox(Composite container) {
		fEnableAssertionsCheckBox= new Button(container, SWT.CHECK | SWT.WRAP);
		fEnableAssertionsCheckBox.setText(JUnitMessages.JUnitPreferencePage_enableassertionscheckbox_label);
		GridData gd= getButtonGridData(fEnableAssertionsCheckBox);
		fEnableAssertionsCheckBox.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(fEnableAssertionsCheckBox);
		setAssertionCheckBoxSelection(AssertionVMArg.getEnableAssertionsPreference());
	}

	private void createShowInAllViewsCheckbox(Composite container) {
		fShowInAllViewsCheckBox= new Button(container, SWT.CHECK | SWT.WRAP);
		fShowInAllViewsCheckBox.setText(JUnitMessages.JUnitPreferencePage_showInAllViews_label);
		GridData gd= getButtonGridData(fShowInAllViewsCheckBox);
		fShowInAllViewsCheckBox.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(fShowInAllViewsCheckBox);
		setShowInAllViewsCheckBoxSelection(JUnitUIPreferencesConstants.getShowInAllViews());
	}

	/**
	 * Programatic access to enable assertions checkbox
	 * @return boolean indicating check box selected or not
	 */
	public boolean getAssertionCheckBoxSelection() {
		return fEnableAssertionsCheckBox.getSelection();
	}

	public void setAssertionCheckBoxSelection(boolean selected) {
		fEnableAssertionsCheckBox.setSelection(selected);
	}

	public boolean getShowInAllViewsCheckBoxSelection() {
		return fShowInAllViewsCheckBox.getSelection();
	}

	public void setShowInAllViewsCheckBoxSelection(boolean selected) {
		fShowInAllViewsCheckBox.setSelection(selected);
	}

	private GridData getButtonGridData(Button button) {
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		int widthHint= convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gd.widthHint= Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		return gd;
	}

	@Override
	public void init(IWorkbench workbench) {}

	@Override
	public boolean performOk() {
		AssertionVMArg.setEnableAssertionsPreference(getAssertionCheckBoxSelection());
		JUnitUIPreferencesConstants.setShowInAllViews(getShowInAllViewsCheckBoxSelection());
		fJavaFilterTable.performOk(getPreferenceStore());
		return true;
	}

	@Override
	protected void performDefaults() {
		setDefaultValues();
		super.performDefaults();
	}

	private void setDefaultValues() {
		fEnableAssertionsCheckBox.setSelection(DefaultScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID)
				.getBoolean(JUnitPreferencesConstants.ENABLE_ASSERTIONS, JUnitPreferencesConstants.ENABLE_ASSERTIONS_DEFAULT));
		fShowInAllViewsCheckBox.setSelection(DefaultScope.INSTANCE.getNode(JUnitPlugin.PLUGIN_ID)
				.getBoolean(JUnitUIPreferencesConstants.SHOW_IN_ALL_VIEWS, JUnitUIPreferencesConstants.SHOW_IN_ALL_VIEWS_DEFAULT));
		fJavaFilterTable.performDefaults();
	}

}
