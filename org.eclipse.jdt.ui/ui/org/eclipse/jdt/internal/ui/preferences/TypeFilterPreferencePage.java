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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.filtertable.FilterManager;
import org.eclipse.jdt.internal.ui.filtertable.JavaFilterTable;
import org.eclipse.jdt.internal.ui.filtertable.JavaFilterTable.ButtonLabel;
import org.eclipse.jdt.internal.ui.filtertable.JavaFilterTable.FilterTableConfig;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/*
 * The page for setting the type filters
 */
public class TypeFilterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String TYPE_FILTER_PREF_PAGE_ID= "org.eclipse.jdt.ui.preferences.TypeFilterPreferencePage"; //$NON-NLS-1$

	private static final String PREF_FILTER_ENABLED= PreferenceConstants.TYPEFILTER_ENABLED;
	private static final String PREF_FILTER_DISABLED= PreferenceConstants.TYPEFILTER_DISABLED;

	private static final String ITEM_SEPARATOR = ";"; //$NON-NLS-1$

	private JavaFilterTable fFilterTable;
	private SelectionButtonDialogField fHideForbiddenField;
	private SelectionButtonDialogField fHideDiscouragedField;

	public TypeFilterPreferencePage() {
		super();
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.TypeFilterPreferencePage_description);

		fFilterTable= new JavaFilterTable(this,
				new FilterManager(PREF_FILTER_ENABLED, PREF_FILTER_DISABLED, ITEM_SEPARATOR),
				new FilterTableConfig()
						.setLabelText(PreferencesMessages.TypeFilterPreferencePage_list_label)
						.setAddFilter(new ButtonLabel(PreferencesMessages.TypeFilterPreferencePage_add_button))
						.setEditFilter(new ButtonLabel(PreferencesMessages.TypeFilterPreferencePage_edit_button))
						.setAddPackage(new ButtonLabel(PreferencesMessages.TypeFilterPreferencePage_addpackage_button))
						.setRemove(new ButtonLabel(PreferencesMessages.TypeFilterPreferencePage_remove_button))
						.setSelectAll(new ButtonLabel(PreferencesMessages.TypeFilterPreferencePage_selectall_button))
						.setDeselectAll(new ButtonLabel(PreferencesMessages.TypeFilterPreferencePage_deselectall_button))
						.setAddTypeDialog(new JavaFilterTable.DialogLabels(PreferencesMessages.TypeFilterInputDialog_title, PreferencesMessages.TypeFilterInputDialog_message))
						.setAddPackageDialog(new JavaFilterTable.DialogLabels(PreferencesMessages.TypeFilterPreferencePage_choosepackage_label,
								PreferencesMessages.TypeFilterPreferencePage_choosepackage_description))
						.setHelpContextId(IJavaHelpContextIds.TYPE_FILTER_PREFERENCE_PAGE)
						.setShowParents(true));

		fHideForbiddenField= new SelectionButtonDialogField(SWT.CHECK);
		fHideForbiddenField.setLabelText(PreferencesMessages.TypeFilterPreferencePage_hideForbidden_label);

		fHideDiscouragedField= new SelectionButtonDialogField(SWT.CHECK);
		fHideDiscouragedField.setLabelText(PreferencesMessages.TypeFilterPreferencePage_hideDiscouraged_label);

		initialize(false);
	}

	/*
	 * @see PreferencePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.TYPE_FILTER_PREFERENCE_PAGE);
	}

	@Override
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;

		composite.setLayout(layout);

		fFilterTable.createTable(composite);

		Label spacer= new Label(composite, SWT.LEFT );
		GridData gd= new GridData(SWT.DEFAULT, convertHeightInCharsToPixels(1) / 2);
		gd.horizontalSpan= 2;
		spacer.setLayoutData(gd);

		String label= PreferencesMessages.TypeFilterPreferencePage_restricted_link;
		Map<String, String> targetInfo= new java.util.HashMap<>(2);
		targetInfo.put(ProblemSeveritiesPreferencePage.DATA_SELECT_OPTION_KEY,	JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE);
		targetInfo.put(ProblemSeveritiesPreferencePage.DATA_SELECT_OPTION_QUALIFIER, JavaCore.PLUGIN_ID);
		createPreferencePageLink(composite, label, targetInfo);

		fHideForbiddenField.doFillIntoGrid(composite, 2);
		fHideDiscouragedField.doFillIntoGrid(composite, 2);

		Dialog.applyDialogFont(composite);
		return composite;
	}

	private void createPreferencePageLink(Composite composite, String label, final Map<String, String> targetInfo) {
		final Link link= new Link(composite, SWT.NONE);
		link.setText(label);
		link.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false, 2, 1));
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, targetInfo);
			}
		});
	}

	private void initialize(boolean fromDefault) {
		if (fromDefault) {
			fFilterTable.performDefaults();
		}

		boolean hideForbidden= getJDTCoreOption(JavaCore.CODEASSIST_FORBIDDEN_REFERENCE_CHECK, fromDefault);
		fHideForbiddenField.setSelection(hideForbidden);
		boolean hideDiscouraged= getJDTCoreOption(JavaCore.CODEASSIST_DISCOURAGED_REFERENCE_CHECK, fromDefault);
		fHideDiscouragedField.setSelection(hideDiscouraged);
	}

	private boolean getJDTCoreOption(String option, boolean fromDefault) {
		Object value= fromDefault ? JavaCore.getDefaultOptions().get(option) : JavaCore.getOption(option);
		return JavaCore.ENABLED.equals(value);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

    /*
     * @see PreferencePage#performDefaults()
     */
    @Override
	protected void performDefaults() {
    	initialize(true);

		super.performDefaults();
    }

    /*
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    @Override
	public boolean performOk() {
  		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();

		fFilterTable.performOk(prefs);
		JavaPlugin.flushInstanceScope();

		Hashtable<String, String> coreOptions= JavaCore.getOptions();
		String hideForbidden= fHideForbiddenField.isSelected() ? JavaCore.ENABLED : JavaCore.DISABLED;
		coreOptions.put(JavaCore.CODEASSIST_FORBIDDEN_REFERENCE_CHECK, hideForbidden);
		String hideDiscouraged= fHideDiscouragedField.isSelected() ? JavaCore.ENABLED : JavaCore.DISABLED;
		coreOptions.put(JavaCore.CODEASSIST_DISCOURAGED_REFERENCE_CHECK, hideDiscouraged);
		JavaCore.setOptions(coreOptions);

        return true;
    }

}

