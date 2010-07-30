/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;

import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;


public class JavaBuildConfigurationBlock extends OptionsConfigurationBlock {

	private static final String SETTINGS_SECTION_NAME= "JavaBuildConfigurationBlock"; //$NON-NLS-1$

	private static final Key PREF_PB_MAX_PER_UNIT= getJDTCoreKey(JavaCore.COMPILER_PB_MAX_PER_UNIT);

	private static final Key PREF_RESOURCE_FILTER= getJDTCoreKey(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER);
	private static final Key PREF_BUILD_INVALID_CLASSPATH= getJDTCoreKey(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH);
	private static final Key PREF_BUILD_CLEAN_OUTPUT_FOLDER= getJDTCoreKey(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER);
	private static final Key PREF_ENABLE_EXCLUSION_PATTERNS= getJDTCoreKey(JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS);
	private static final Key PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS= getJDTCoreKey(JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS);

	private static final Key PREF_PB_INCOMPLETE_BUILDPATH= getJDTCoreKey(JavaCore.CORE_INCOMPLETE_CLASSPATH);
	private static final Key PREF_PB_CIRCULAR_BUILDPATH= getJDTCoreKey(JavaCore.CORE_CIRCULAR_CLASSPATH);
	private static final Key PREF_PB_INCOMPATIBLE_JDK_LEVEL= getJDTCoreKey(JavaCore.CORE_INCOMPATIBLE_JDK_LEVEL);
	private static final Key PREF_PB_DUPLICATE_RESOURCE= getJDTCoreKey(JavaCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE);
	private static final Key PREF_RECREATE_MODIFIED_CLASS_FILES= getJDTCoreKey(JavaCore.CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER);

	private static final Key PREF_PB_STRICTLY_COMPATIBLE_JRE_NOT_AVAILABLE= getJDTLaunchingKey(JavaRuntime.PREF_STRICTLY_COMPATIBLE_JRE_NOT_AVAILABLE);


	// values
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ABORT= JavaCore.ABORT;
	private static final String CLEAN= JavaCore.CLEAN;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;

	private PixelConverter fPixelConverter;

	private FilteredPreferenceTree fFilteredPrefTree;

	private IStatus fMaxNumberProblemsStatus, fResourceFilterStatus;

	public JavaBuildConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);
		fMaxNumberProblemsStatus= new StatusInfo();
		fResourceFilterStatus= new StatusInfo();
	}

	private static Key[] getKeys() {
		Key[] keys= new Key[] {
				PREF_PB_MAX_PER_UNIT, PREF_RESOURCE_FILTER, PREF_BUILD_INVALID_CLASSPATH, PREF_PB_INCOMPLETE_BUILDPATH, PREF_PB_CIRCULAR_BUILDPATH,
				PREF_BUILD_CLEAN_OUTPUT_FOLDER, PREF_PB_DUPLICATE_RESOURCE,
				PREF_PB_INCOMPATIBLE_JDK_LEVEL, PREF_ENABLE_EXCLUSION_PATTERNS, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS, PREF_RECREATE_MODIFIED_CLASS_FILES,
				PREF_PB_STRICTLY_COMPATIBLE_JRE_NOT_AVAILABLE
			};
		return keys;
	}


	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		mainComp.setLayout(layout);

		Composite othersComposite= createBuildPathTabContent(mainComp);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint= fPixelConverter.convertHeightInCharsToPixels(20);
		othersComposite.setLayoutData(gridData);

		validateSettings(null, null, null);

		return mainComp;
	}


	private Composite createBuildPathTabContent(Composite parent) {
		String[] abortIgnoreValues= new String[] { ABORT, IGNORE };
		String[] cleanIgnoreValues= new String[] { CLEAN, IGNORE };
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };
		String[] enableIgnoreValues= new String[] { ENABLED, IGNORE };

		String[] errorWarning= new String[] { ERROR, WARNING };

		String[] errorWarningLabels= new String[] {
			PreferencesMessages.JavaBuildConfigurationBlock_error,
			PreferencesMessages.JavaBuildConfigurationBlock_warning
		};

		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.JavaBuildConfigurationBlock_error,
			PreferencesMessages.JavaBuildConfigurationBlock_warning,
			PreferencesMessages.JavaBuildConfigurationBlock_ignore
		};

		fFilteredPrefTree= new FilteredPreferenceTree(this, parent, null);
		final ScrolledPageContent pageContent= fFilteredPrefTree.getScrolledPageContent();

		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
		layout.marginHeight= 0;
		layout.marginWidth= 0;

		Composite composite= pageContent.getBody();
		composite.setLayout(layout);

		PreferenceTreeNode section;

		String label= PreferencesMessages.JavaBuildConfigurationBlock_section_general;
		Key key= OptionsConfigurationBlock.getLocalKey("JavaBuildConfigurationBlock_section_general"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, key, null, false);
		ExpandableComposite excomposite= getExpandableComposite(key);

		Composite othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));

		label= PreferencesMessages.JavaBuildConfigurationBlock_pb_max_per_unit_label;
		fFilteredPrefTree.addTextField(othersComposite, label, PREF_PB_MAX_PER_UNIT, 0, 0, section, true);
		Text text= getTextControl(PREF_PB_MAX_PER_UNIT);
		GridData gd= (GridData) text.getLayoutData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(8);
		gd.horizontalAlignment= SWT.END;
		text.setTextLimit(6);

		label= PreferencesMessages.JavaBuildConfigurationBlock_enable_exclusion_patterns_label;
		fFilteredPrefTree.addCheckBox(othersComposite, label, PREF_ENABLE_EXCLUSION_PATTERNS, enableDisableValues, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_enable_multiple_outputlocations_label;
		fFilteredPrefTree.addCheckBox(othersComposite, label, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS, enableDisableValues, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_section_build_path_problems;
		key= OptionsConfigurationBlock.getLocalKey("JavaBuildConfigurationBlock_section_build_path_problems"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, key, null, false);
		excomposite= getExpandableComposite(key);

		othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));

		label= PreferencesMessages.JavaBuildConfigurationBlock_build_invalid_classpath_label;
		fFilteredPrefTree.addCheckBox(othersComposite, label, PREF_BUILD_INVALID_CLASSPATH, abortIgnoreValues, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_pb_incomplete_build_path_label;
		fFilteredPrefTree.addComboBox(othersComposite, label, PREF_PB_INCOMPLETE_BUILDPATH, errorWarning, errorWarningLabels, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_pb_build_path_cycles_label;
		fFilteredPrefTree.addComboBox(othersComposite, label, PREF_PB_CIRCULAR_BUILDPATH, errorWarning, errorWarningLabels, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_pb_check_prereq_binary_level_label;
		fFilteredPrefTree.addComboBox(othersComposite, label, PREF_PB_INCOMPATIBLE_JDK_LEVEL, errorWarningIgnore, errorWarningIgnoreLabels, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_pb_strictly_compatible_jre_not_available_label;
		fFilteredPrefTree.addComboBox(othersComposite, label, PREF_PB_STRICTLY_COMPATIBLE_JRE_NOT_AVAILABLE, errorWarningIgnore, errorWarningIgnoreLabels, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_section_output_folder;
		key= OptionsConfigurationBlock.getLocalKey("JavaBuildConfigurationBlock_section_output_folder"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, key, null, false);
		excomposite= getExpandableComposite(key);

		othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));

		label= PreferencesMessages.JavaBuildConfigurationBlock_pb_duplicate_resources_label;
		fFilteredPrefTree.addComboBox(othersComposite, label, PREF_PB_DUPLICATE_RESOURCE, errorWarning, errorWarningLabels, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_build_clean_outputfolder_label;
		fFilteredPrefTree.addCheckBox(othersComposite, label, PREF_BUILD_CLEAN_OUTPUT_FOLDER, cleanIgnoreValues, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_build_recreate_modified;
		fFilteredPrefTree.addCheckBox(othersComposite, label, PREF_RECREATE_MODIFIED_CLASS_FILES, enableIgnoreValues, 0, section);

		label= PreferencesMessages.JavaBuildConfigurationBlock_resource_filter_label;
		fFilteredPrefTree.addTextField(othersComposite, label, PREF_RESOURCE_FILTER, 0, 0, section, true);
		text= getTextControl(PREF_RESOURCE_FILTER);
		gd= (GridData) text.getLayoutData();
		gd.grabExcessHorizontalSpace= true;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(10);

		//TODO: add support for a simple label
		Label description= new Label(othersComposite, SWT.WRAP);
		description.setText(PreferencesMessages.JavaBuildConfigurationBlock_resource_filter_description);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);

		IDialogSettings settingsSection= JavaPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION_NAME);
		restoreSectionExpansionStates(settingsSection);

		return pageContent;
	}

	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (!areSettingsEnabled()) {
			return;
		}

		if (changedKey != null) {
			if (PREF_PB_MAX_PER_UNIT.equals(changedKey)) {
				fMaxNumberProblemsStatus= validateMaxNumberProblems();
			} else if (PREF_RESOURCE_FILTER.equals(changedKey)) {
				fResourceFilterStatus= validateResourceFilters();
			} else {
				return;
			}
		} else {
			updateEnableStates();
			fMaxNumberProblemsStatus= validateMaxNumberProblems();
			fResourceFilterStatus= validateResourceFilters();
		}
		IStatus status= StatusUtil.getMostSevere(new IStatus[] { fMaxNumberProblemsStatus, fResourceFilterStatus });
		fContext.statusChanged(status);
	}

	private void updateEnableStates() {
	}

	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.JavaBuildConfigurationBlock_needsbuild_title;
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.JavaBuildConfigurationBlock_needsfullbuild_message;
		} else {
			message= PreferencesMessages.JavaBuildConfigurationBlock_needsprojectbuild_message;
		}
		return new String[] { title, message };
	}

	private IStatus validateMaxNumberProblems() {
		String number= getValue(PREF_PB_MAX_PER_UNIT);
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.JavaBuildConfigurationBlock_empty_input);
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value <= 0) {
					status.setError(Messages.format(PreferencesMessages.JavaBuildConfigurationBlock_invalid_input, number));
				}
			} catch (NumberFormatException e) {
				status.setError(Messages.format(PreferencesMessages.JavaBuildConfigurationBlock_invalid_input, number));
			}
		}
		return status;
	}

	private IStatus validateResourceFilters() {
		String text= getValue(PREF_RESOURCE_FILTER);

		IWorkspace workspace= ResourcesPlugin.getWorkspace();

		String[] filters= getTokens(text, ","); //$NON-NLS-1$
		for (int i= 0; i < filters.length; i++) {
			String fileName= filters[i].replace('*', 'x');
			int resourceType= IResource.FILE;
			int lastCharacter= fileName.length() - 1;
			if (lastCharacter >= 0 && fileName.charAt(lastCharacter) == '/') {
				fileName= fileName.substring(0, lastCharacter);
				resourceType= IResource.FOLDER;
			}
			IStatus status= workspace.validateName(fileName, resourceType);
			if (status.matches(IStatus.ERROR)) {
				String message= Messages.format(PreferencesMessages.JavaBuildConfigurationBlock_filter_invalidsegment_error, status.getMessage());
				return new StatusInfo(IStatus.ERROR, message);
			}
		}
		return new StatusInfo();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#dispose()
	 */
	public void dispose() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION_NAME);
		storeSectionExpansionStates(settings);
		super.dispose();
	}


}
