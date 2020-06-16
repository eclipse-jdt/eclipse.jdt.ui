/**
 *  Copyright (c) 2018, 2020 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - [CodeMining] Provide Java References/Implementation CodeMinings - Bug 529127
 *     IBM Corporation
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.FilteredPreferenceTree.PreferenceTreeNode;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;

/**
 * Configures Java Editor code mining preferences.
 *
 * @since 3.16
 */
public class JavaEditorCodeMiningConfigurationBlock extends OptionsConfigurationBlock {

	// Preference store keys

	// --------------------- General

	private static final Key PREF_CODEMINING_ENABLED= getJDTUIKey(
			PreferenceConstants.EDITOR_CODEMINING_ENABLED);

	private static final Key PREF_SHOW_CODEMINING_AT_LEAST_ONE= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_CODEMINING_AT_LEAST_ONE);

	private static final Key PREF_SHOW_REFERENCES= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES);

	private static final Key PREF_SHOW_REFERENCES_ON_TYPES= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES_ON_TYPES);

	private static final Key PREF_SHOW_REFERENCES_ON_FIELDS= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES_ON_FIELDS);

	private static final Key PREF_SHOW_REFERENCES_ON_METHODS= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_REFERENCES_ON_METHODS);

	private static final Key PREF_SHOW_IMPLEMENTATIONS= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_IMPLEMENTATIONS);

	private static final Key PREF_SHOW_PARAMETER_NAMES= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_PARAMETER_NAMES);

	private static final String SETTINGS_SECTION_NAME= "JavaEditorCodeMiningConfigurationBlock"; //$NON-NLS-1$

	private static final String[] TRUE_FALSE= new String[] { "true", "false" }; //$NON-NLS-1$ //$NON-NLS-2$

	private PixelConverter fPixelConverter;

	private Button atLeastOneCheckBox;

	private PreferenceTree fFilteredPrefTree;

	public JavaEditorCodeMiningConfigurationBlock(IStatusChangeListener context,
			IWorkbenchPreferenceContainer container) {
		super(context, null, getAllKeys(), container);
	}

	public static Key[] getAllKeys() {
		return new Key[] { PREF_CODEMINING_ENABLED, PREF_SHOW_CODEMINING_AT_LEAST_ONE, PREF_SHOW_REFERENCES, PREF_SHOW_REFERENCES_ON_TYPES, PREF_SHOW_REFERENCES_ON_FIELDS,
				PREF_SHOW_REFERENCES_ON_METHODS,
				PREF_SHOW_IMPLEMENTATIONS, PREF_SHOW_PARAMETER_NAMES };
	}

	@Override
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		Shell shell= parent.getShell();
		setShell(shell);

		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		mainComp.setLayout(layout);

		// Add enabled code mining checkbox
		String text= PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_enableCodeMining_label;
		Button codeMiningEnabledCheckBox= addCheckBoxWithLink(mainComp, text, PREF_CODEMINING_ENABLED, TRUE_FALSE, 0, SWT.DEFAULT, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ("org.eclipse.ui.preferencePages.GeneralTextEditor".equals(e.text)) { //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(shell, e.text, null, null);
				}
			}
		});

		// - Only if there is at least one result
		atLeastOneCheckBox= addCheckBox(mainComp,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showCodeMining_atLeastOne_label,
				PREF_SHOW_CODEMINING_AT_LEAST_ONE, TRUE_FALSE, LayoutUtil.getIndent());

		Composite commonComposite= createCodeMiningContent(mainComp);
		GridData gridData= new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.heightHint= fPixelConverter.convertHeightInCharsToPixels(20);
		commonComposite.setLayoutData(gridData);

		codeMiningEnabledCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnableStates();
			}
		});
		atLeastOneCheckBox.setEnabled(codeMiningEnabledCheckBox.getSelection());
		fFilteredPrefTree.setEnabled(codeMiningEnabledCheckBox.getSelection());
		validateSettings(null, null, null);
		return mainComp;
	}

	private Composite createCodeMiningContent(Composite parent) {
		// Create filtered tree which contains code minings
		fFilteredPrefTree= new PreferenceTree(this, parent,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_common_description, false);
		final ScrolledPageContent sc1= fFilteredPrefTree.getScrolledPageContent();

		int nColumns= 1;
		Composite composite= sc1.getBody();
		GridLayout layout= new GridLayout(nColumns, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		// --- General
		createGeneralSection(nColumns, composite);

		IDialogSettings settingsSection= JavaPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION_NAME);
		restoreSectionExpansionStates(settingsSection);

		return sc1;
	}

	private void createGeneralSection(int nColumns, Composite parent) {
		int defaultIndent= 0;
		int extraIndent= LayoutUtil.getIndent();
		String label= PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_section_general;
		Key twistieKey= OptionsConfigurationBlock.getLocalKey("JavaEditorCodeMiningPreferencePage_section_general"); //$NON-NLS-1$
		PreferenceTreeNode<?> section= fFilteredPrefTree.addExpandableComposite(parent, label, nColumns, twistieKey,
				null, false);
		ExpandableComposite excomposite= getExpandableComposite(twistieKey);

		Composite inner= createInnerComposite(excomposite, nColumns, parent.getFont());

		// - Show references
		PreferenceTreeNode<Button> showReferences= fFilteredPrefTree.addCheckBox(inner,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_label, PREF_SHOW_REFERENCES,
				TRUE_FALSE, defaultIndent, section);
		// - Show references (On types)
		fFilteredPrefTree.addCheckBox(inner,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_onTypes_label,
				PREF_SHOW_REFERENCES_ON_TYPES, TRUE_FALSE, extraIndent, showReferences);
		// - Show references (On fields)
		fFilteredPrefTree.addCheckBox(inner,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_onFields_label,
				PREF_SHOW_REFERENCES_ON_FIELDS, TRUE_FALSE, extraIndent, showReferences);
		// - Show references (On methods)
		fFilteredPrefTree.addCheckBox(inner,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_onMethods_label,
				PREF_SHOW_REFERENCES_ON_METHODS, TRUE_FALSE, extraIndent, showReferences);

		// - Show implementations
		fFilteredPrefTree.addCheckBox(inner,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showImplementations_label,
				PREF_SHOW_IMPLEMENTATIONS, TRUE_FALSE, defaultIndent, section);

		// - Show parameter names
		fFilteredPrefTree.addCheckBox(inner,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showParameterNames_label,
				PREF_SHOW_PARAMETER_NAMES, TRUE_FALSE, defaultIndent, section);

	}

	private void updateEnableStates() {
		boolean enabledCodeMining= getCheckBox(PREF_CODEMINING_ENABLED).getSelection();
		if (enabledCodeMining) {
			// Show references checkboxes
			atLeastOneCheckBox.setEnabled(true);
			fFilteredPrefTree.setEnabled(true);

			boolean showReferences= getCheckBox(PREF_SHOW_REFERENCES).getSelection();
			getCheckBox(PREF_SHOW_REFERENCES_ON_TYPES).setEnabled(showReferences);
			getCheckBox(PREF_SHOW_REFERENCES_ON_FIELDS).setEnabled(showReferences);
			getCheckBox(PREF_SHOW_REFERENCES_ON_METHODS).setEnabled(showReferences);
			// Show implementations checkboxes
			getCheckBox(PREF_SHOW_IMPLEMENTATIONS).getSelection();
			getCheckBox(PREF_SHOW_PARAMETER_NAMES).getSelection();
		} else {
			atLeastOneCheckBox.setEnabled(false);
			fFilteredPrefTree.setEnabled(false);
		}
	}

	private Composite createInnerComposite(ExpandableComposite excomposite, int nColumns, Font font) {
		Composite inner= new Composite(excomposite, SWT.NONE);
		inner.setFont(font);
		inner.setLayout(new GridLayout(nColumns, false));
		excomposite.setClient(inner);
		return inner;
	}

	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (!areSettingsEnabled()) {
			return;
		}
		if (changedKey != null) {
			if (PREF_CODEMINING_ENABLED.equals(changedKey) || PREF_SHOW_REFERENCES.equals(changedKey) || PREF_SHOW_IMPLEMENTATIONS.equals(changedKey)) {
				updateEnableStates();
			}
		} else {
			updateEnableStates();
		}
		fContext.statusChanged(new StatusInfo());
	}

	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}

	@Override
	public void dispose() {
		IDialogSettings section= JavaPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION_NAME);
		storeSectionExpansionStates(section);
		super.dispose();
	}
}
