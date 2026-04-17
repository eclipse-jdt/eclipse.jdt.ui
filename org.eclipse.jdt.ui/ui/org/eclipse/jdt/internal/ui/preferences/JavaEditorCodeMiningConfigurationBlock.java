/**
 *  Copyright (c) 2018, 2026 Angelo ZERR and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
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

	private static final Key PREF_IGNORE_INEXACT_MATCHES= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_IGNORE_INEXACT_MATCHES);

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

	private static final Key PREF_FILTER_IMPLIED_PARAMETER_NAMES= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_FILTER_IMPLIED_PARAMETER_NAMES);

	private static final Key PREF_DEFAULT_FILTER_FOR_PARAMETER_NAMES= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_DEFAULT_FILTER_FOR_PARAMETER_NAMES);

	private static final Key PREF_SHOW_ONE_PARAMETER= getJDTUIKey(
			PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_PARAMETER_NAME_SINGLE_ARG);

	private static final String SETTINGS_SECTION_NAME= "JavaEditorCodeMiningConfigurationBlock"; //$NON-NLS-1$

	private static final String[] TRUE_FALSE= new String[] { "true", "false" }; //$NON-NLS-1$ //$NON-NLS-2$

	private PixelConverter fPixelConverter;

	private Button atLeastOneCheckBox;

	private Button ignoreInexactReferenceMatches;

	private Button selectAllInGeneral;

	private Button deselectAllInGeneral;

	private PreferenceTree fFilteredPrefTree;

	private List<PreferenceStoreEntry> generalCodeMiningOptions= new ArrayList<>();

	public JavaEditorCodeMiningConfigurationBlock(IStatusChangeListener context,
			IWorkbenchPreferenceContainer container) {
		super(context, null, getAllKeys(), container);
	}

	public static Key[] getAllKeys() {
		return new Key[] { PREF_CODEMINING_ENABLED, PREF_SHOW_CODEMINING_AT_LEAST_ONE, PREF_SHOW_REFERENCES, PREF_SHOW_REFERENCES_ON_TYPES, PREF_SHOW_REFERENCES_ON_FIELDS,
				PREF_SHOW_REFERENCES_ON_METHODS,
				PREF_SHOW_IMPLEMENTATIONS, PREF_SHOW_PARAMETER_NAMES, PREF_IGNORE_INEXACT_MATCHES, PREF_FILTER_IMPLIED_PARAMETER_NAMES, PREF_DEFAULT_FILTER_FOR_PARAMETER_NAMES, PREF_SHOW_ONE_PARAMETER };
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

		ignoreInexactReferenceMatches= addCheckBox(mainComp,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_ignoreInexactMatches_label,
				PREF_IGNORE_INEXACT_MATCHES, TRUE_FALSE, LayoutUtil.getIndent());


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
		ignoreInexactReferenceMatches.setEnabled(codeMiningEnabledCheckBox.getSelection());
		selectAllInGeneral.setEnabled(codeMiningEnabledCheckBox.getSelection());
		deselectAllInGeneral.setEnabled(codeMiningEnabledCheckBox.getSelection());
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

	private static class PreferenceStoreEntry {
		PreferenceTreeNode<Button> node;

		Key key;

		PreferenceStoreEntry(PreferenceTreeNode<Button> node, Key key) {
			this.node= node;
			this.key= key;
		}
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

		Group generalGroup= new Group(inner, SWT.NONE);
		generalGroup.setLayout(new GridLayout(nColumns, false));
		GridData groupData= new GridData(GridData.FILL_HORIZONTAL);
		groupData.horizontalSpan= nColumns;
		generalGroup.setLayoutData(groupData);
		generalGroup.setFont(parent.getFont());
		generalCodeMiningOptions.clear();

		// - Show references
		PreferenceTreeNode<Button> showReferences= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_label, PREF_SHOW_REFERENCES,
				TRUE_FALSE, defaultIndent, section);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(showReferences, PREF_SHOW_REFERENCES));
		// - Show references (On types)
		PreferenceTreeNode<Button> onTypes= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_onTypes_label,
				PREF_SHOW_REFERENCES_ON_TYPES, TRUE_FALSE, extraIndent, showReferences);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(onTypes, PREF_SHOW_REFERENCES_ON_TYPES));
		// - Show references (On fields)
		PreferenceTreeNode<Button> onFields= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_onFields_label,
				PREF_SHOW_REFERENCES_ON_FIELDS, TRUE_FALSE, extraIndent, showReferences);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(onFields, PREF_SHOW_REFERENCES_ON_FIELDS));
		// - Show references (On methods)
		PreferenceTreeNode<Button> onMethods= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showReferences_onMethods_label,
				PREF_SHOW_REFERENCES_ON_METHODS, TRUE_FALSE, extraIndent, showReferences);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(onMethods, PREF_SHOW_REFERENCES_ON_METHODS));

		// - Show implementations
		PreferenceTreeNode<Button> impl= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showImplementations_label,
				PREF_SHOW_IMPLEMENTATIONS, TRUE_FALSE, defaultIndent, section);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(impl, PREF_SHOW_IMPLEMENTATIONS));

		// - Show parameter names
		PreferenceTreeNode<Button> paramNames= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_showParameterNames_label,
				PREF_SHOW_PARAMETER_NAMES, TRUE_FALSE, defaultIndent, section);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(paramNames, PREF_SHOW_PARAMETER_NAMES));

		// - Filter implied parameter names
		PreferenceTreeNode<Button> implied= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_filterImpliedParameterNames_label,
				PREF_FILTER_IMPLIED_PARAMETER_NAMES, TRUE_FALSE, extraIndent, section);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(implied, PREF_FILTER_IMPLIED_PARAMETER_NAMES));

		// - Filter known method parameter names
		PreferenceTreeNode<Button> defaultFilter= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_defaultFilterForParameterNames_label,
				PREF_DEFAULT_FILTER_FOR_PARAMETER_NAMES, TRUE_FALSE, extraIndent, section);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(defaultFilter, PREF_DEFAULT_FILTER_FOR_PARAMETER_NAMES));

		// - One parameter
		PreferenceTreeNode<Button> oneParam= fFilteredPrefTree.addCheckBox(generalGroup,
				PreferencesMessages.JavaEditorCodeMiningShowOneParameter_label,
				PREF_SHOW_ONE_PARAMETER, TRUE_FALSE, extraIndent, section);
		generalCodeMiningOptions.add(new PreferenceStoreEntry(oneParam, PREF_SHOW_ONE_PARAMETER));

		Composite compositeForButtons= new Composite(generalGroup, SWT.NONE);
		GridLayout buttonLayout= new GridLayout(2, false);
		compositeForButtons.setLayout(buttonLayout);
		GridData buttonCompositeData= new GridData(GridData.FILL_HORIZONTAL);
		buttonCompositeData.horizontalAlignment= SWT.END;
		compositeForButtons.setLayoutData(buttonCompositeData);

		selectAllInGeneral= new Button(compositeForButtons, SWT.PUSH);
		selectAllInGeneral.setText(PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_section_general_select_all);
		GridData selectAllGridData= new GridData(SWT.END, SWT.CENTER, false, false);
		selectAllGridData.widthHint= fPixelConverter.convertHorizontalDLUsToPixels(70);
		selectAllInGeneral.setLayoutData(selectAllGridData);
		selectAllInGeneral.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (PreferenceStoreEntry entry : generalCodeMiningOptions) {
					Button button= entry.node.getControl();
					setValue(entry.key, true);
					button.setSelection(true);
				}
				updateEnableStates();
			}
		});

		deselectAllInGeneral= new Button(compositeForButtons, SWT.PUSH);
		deselectAllInGeneral.setText(PreferencesMessages.JavaEditorCodeMiningConfigurationBlock_section_general_deselect_all);
		GridData deselectAllData= new GridData(SWT.END, SWT.CENTER, false, false);
		deselectAllData.widthHint= fPixelConverter.convertHorizontalDLUsToPixels(70);
		deselectAllInGeneral.setLayoutData(deselectAllData);
		deselectAllInGeneral.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (PreferenceStoreEntry entry : generalCodeMiningOptions) {
					Button button= entry.node.getControl();
					setValue(entry.key, false);
					button.setSelection(false);
				}
				updateEnableStates();
			}
		});
	}

	private void updateEnableStates() {
		boolean enabledCodeMining= getCheckBox(PREF_CODEMINING_ENABLED).getSelection();
		if (enabledCodeMining) {
			// Show references checkboxes
			atLeastOneCheckBox.setEnabled(true);
			ignoreInexactReferenceMatches.setEnabled(true);
			fFilteredPrefTree.setEnabled(true);
			selectAllInGeneral.setEnabled(true);
			deselectAllInGeneral.setEnabled(true);

			boolean showReferences= getCheckBox(PREF_SHOW_REFERENCES).getSelection();
			getCheckBox(PREF_SHOW_REFERENCES_ON_TYPES).setEnabled(showReferences);
			getCheckBox(PREF_SHOW_REFERENCES_ON_FIELDS).setEnabled(showReferences);
			getCheckBox(PREF_SHOW_REFERENCES_ON_METHODS).setEnabled(showReferences);
			// Show implementations checkboxes
			getCheckBox(PREF_SHOW_IMPLEMENTATIONS).getSelection();
			boolean showParameterNames= getCheckBox(PREF_SHOW_PARAMETER_NAMES).getSelection();

			getCheckBox(PREF_FILTER_IMPLIED_PARAMETER_NAMES).setEnabled(showParameterNames);
			getCheckBox(PREF_DEFAULT_FILTER_FOR_PARAMETER_NAMES).setEnabled(showParameterNames);
			getCheckBox(PREF_SHOW_ONE_PARAMETER).setEnabled(showParameterNames);

		} else {
			atLeastOneCheckBox.setEnabled(false);
			ignoreInexactReferenceMatches.setEnabled(false);
			fFilteredPrefTree.setEnabled(false);
			selectAllInGeneral.setEnabled(false);
			deselectAllInGeneral.setEnabled(false);
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
			if (PREF_CODEMINING_ENABLED.equals(changedKey) || PREF_SHOW_REFERENCES.equals(changedKey) || PREF_SHOW_IMPLEMENTATIONS.equals(changedKey)
					|| PREF_SHOW_PARAMETER_NAMES.equals(changedKey)) {
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
