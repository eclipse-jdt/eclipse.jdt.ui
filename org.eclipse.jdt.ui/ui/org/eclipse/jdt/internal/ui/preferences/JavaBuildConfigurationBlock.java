/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
  */
public class JavaBuildConfigurationBlock extends OptionsConfigurationBlock {
	
	private static final String PREF_PB_MAX_PER_UNIT= JavaCore.COMPILER_PB_MAX_PER_UNIT;
	private static final String PREF_PB_FORBIDDEN_REFERENCE= JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE;


	private static final String PREF_RESOURCE_FILTER= JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER;
	private static final String PREF_BUILD_INVALID_CLASSPATH= JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH;
	private static final String PREF_BUILD_CLEAN_OUTPUT_FOLDER= JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER;
	private static final String PREF_ENABLE_EXCLUSION_PATTERNS= JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS;
	private static final String PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS= JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS;

	private static final String PREF_PB_INCOMPLETE_BUILDPATH= JavaCore.CORE_INCOMPLETE_CLASSPATH;
	private static final String PREF_PB_CIRCULAR_BUILDPATH= JavaCore.CORE_CIRCULAR_CLASSPATH;
	private static final String PREF_PB_INCOMPATIBLE_JDK_LEVEL= JavaCore.CORE_INCOMPATIBLE_JDK_LEVEL;
	private static final String PREF_PB_DUPLICATE_RESOURCE= JavaCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE;

	// values
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ABORT= JavaCore.ABORT;
	private static final String CLEAN= JavaCore.CLEAN;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;

	private PixelConverter fPixelConverter;
	
	private IStatus fMaxNumberProblemsStatus, fResourceFilterStatus;

	public JavaBuildConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project, getKeys());
		fMaxNumberProblemsStatus= new StatusInfo();
		fResourceFilterStatus= new StatusInfo();
	}
	
	private static String[] getKeys() {
		String[] keys= new String[] {
				PREF_PB_MAX_PER_UNIT, PREF_PB_FORBIDDEN_REFERENCE,
				PREF_RESOURCE_FILTER, PREF_BUILD_INVALID_CLASSPATH, PREF_PB_INCOMPLETE_BUILDPATH, PREF_PB_CIRCULAR_BUILDPATH,
				PREF_BUILD_CLEAN_OUTPUT_FOLDER, PREF_PB_DUPLICATE_RESOURCE,
				PREF_PB_INCOMPATIBLE_JDK_LEVEL, PREF_ENABLE_EXCLUSION_PATTERNS, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS,
			};
		return keys;
	}
	
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());
		
		Composite othersComposite= createBuildPathTabContent(parent);

		validateSettings(null, null, null);
	
		return othersComposite;
	}
	

	private Composite createBuildPathTabContent(Composite parent) {
		String[] abortIgnoreValues= new String[] { ABORT, IGNORE };
		String[] cleanIgnoreValues= new String[] { CLEAN, IGNORE };
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };
		
		String[] errorWarning= new String[] { ERROR, WARNING };
		
		String[] errorWarningLabels= new String[] {
			PreferencesMessages.getString("JavaBuildConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("JavaBuildConfigurationBlock.warning") //$NON-NLS-1$
		};
		
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("JavaBuildConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("JavaBuildConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("JavaBuildConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		int nColumns= 3;
		
		final ScrolledPageContent pageContent = new ScrolledPageContent(parent);

		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
		
		Composite composite= pageContent.getBody();
		composite.setLayout(layout);
				
		
		String label= "General"; //$NON-NLS-1$
		ExpandableComposite excomposite= createStyleSection(composite, label, nColumns); 
		
		Composite othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));
		
		GridData gd= new GridData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(6);
		
		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.pb_max_per_unit.label"); //$NON-NLS-1$
		Text text= addTextField(othersComposite, label, PREF_PB_MAX_PER_UNIT, 0, 0);
		text.setTextLimit(6);
		text.setLayoutData(gd);

		label= "Build path problems"; //$NON-NLS-1$
		excomposite= createStyleSection(composite, label, nColumns); 
		
		othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));
		
		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.build_invalid_classpath.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_BUILD_INVALID_CLASSPATH, abortIgnoreValues, 0);

		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.pb_incomplete_build_path.label"); //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_INCOMPLETE_BUILDPATH, errorWarning, errorWarningLabels, 0);	
		
		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.pb_build_path_cycles.label"); //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_CIRCULAR_BUILDPATH, errorWarning, errorWarningLabels, 0);
		
		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.pb_check_prereq_binary_level.label");  //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_INCOMPATIBLE_JDK_LEVEL, errorWarningIgnore, errorWarningIgnoreLabels, 0);	
		
		label= "Output folder"; //$NON-NLS-1$
		excomposite= createStyleSection(composite, label, nColumns); 
		
		othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));
		
		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.pb_duplicate_resources.label"); //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_DUPLICATE_RESOURCE, errorWarning, errorWarningLabels, 0);

		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.build_clean_outputfolder.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_BUILD_CLEAN_OUTPUT_FOLDER, cleanIgnoreValues, 0);

		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.resource_filter.label"); //$NON-NLS-1$
		text= addTextField(othersComposite, label, PREF_RESOURCE_FILTER, 0, 0);
		gd= (GridData) text.getLayoutData();
		gd.grabExcessHorizontalSpace= true;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(10);

		Label description= new Label(othersComposite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("JavaBuildConfigurationBlock.resource_filter.description")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);

		label= "Type restriction"; //$NON-NLS-1$
		excomposite= createStyleSection(composite, label, nColumns); 
		
		othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));
		
		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.pb_forbidden_reference.label"); //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_FORBIDDEN_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= "Build path properties"; //$NON-NLS-1$
		excomposite= createStyleSection(composite, label, nColumns); 
		
		othersComposite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(othersComposite);
		othersComposite.setLayout(new GridLayout(nColumns, false));

		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.enable_exclusion_patterns.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_EXCLUSION_PATTERNS, enableDisableValues, 0);

		label= PreferencesMessages.getString("JavaBuildConfigurationBlock.enable_multiple_outputlocations.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS, enableDisableValues, 0);
				

		return pageContent;
	}
	
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(String changedKey, String oldValue, String newValue) {
		
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
		String title= PreferencesMessages.getString("JavaBuildConfigurationBlock.needsbuild.title"); //$NON-NLS-1$
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.getString("JavaBuildConfigurationBlock.needsfullbuild.message"); //$NON-NLS-1$
		} else {
			message= PreferencesMessages.getString("JavaBuildConfigurationBlock.needsprojectbuild.message"); //$NON-NLS-1$
		}
		return new String[] { title, message };
	}

	private IStatus validateMaxNumberProblems() {
		String number= (String) fWorkingValues.get(PREF_PB_MAX_PER_UNIT);
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.getString("JavaBuildConfigurationBlock.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value <= 0) {
					status.setError(PreferencesMessages.getFormattedString("JavaBuildConfigurationBlock.invalid_input", number)); //$NON-NLS-1$
				}
			} catch (NumberFormatException e) {
				status.setError(PreferencesMessages.getFormattedString("JavaBuildConfigurationBlock.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}

	private IStatus validateResourceFilters() {
		String text= (String) fWorkingValues.get(PREF_RESOURCE_FILTER);
		
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
				String message= PreferencesMessages.getFormattedString("JavaBuildConfigurationBlock.filter.invalidsegment.error", status.getMessage()); //$NON-NLS-1$
				return new StatusInfo(IStatus.ERROR, message);
			}
		}
		return new StatusInfo();
	}
	
		
}
