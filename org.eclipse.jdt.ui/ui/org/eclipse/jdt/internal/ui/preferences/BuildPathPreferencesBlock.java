/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
  */
public class BuildPathPreferencesBlock extends OptionsConfigurationBlock {

	// Preference store keys, see JavaCore.getOptions
	
//	private static final String PREF_RESOURCE_FILTER= JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER;
//	private static final String PREF_BUILD_INVALID_CLASSPATH= JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH;
//	private static final String PREF_BUILD_CLEAN_OUTPUT_FOLDER= JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER;
	private static final String PREF_ENABLE_EXCLUSION_PATTERNS= JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS;
	private static final String PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS= JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS;

//	private static final String PREF_PB_INCOMPLETE_BUILDPATH= JavaCore.CORE_INCOMPLETE_CLASSPATH;
//	private static final String PREF_PB_CIRCULAR_BUILDPATH= JavaCore.CORE_CIRCULAR_CLASSPATH;
//	private static final String PREF_PB_INCOMPATIBLE_JDK_LEVEL= JavaCore.CORE_INCOMPATIBLE_JDK_LEVEL;
//	private static final String PREF_PB_DUPLICATE_RESOURCE= JavaCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE;
	

	// values
	
//	private static final String ERROR= JavaCore.ERROR;
//	private static final String WARNING= JavaCore.WARNING;
//	private static final String IGNORE= JavaCore.IGNORE;
//	private static final String ABORT= JavaCore.ABORT;
//	
//	private static final String CLEAN= JavaCore.CLEAN;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;
	

//	private PixelConverter fPixelConverter;

	private IStatus  fResourceFilterStatus;

	public BuildPathPreferencesBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project);
		fResourceFilterStatus= new StatusInfo();
	}
	
	private final String[] KEYS= new String[] {
//		PREF_RESOURCE_FILTER, PREF_BUILD_INVALID_CLASSPATH, PREF_PB_INCOMPLETE_BUILDPATH, PREF_PB_INCOMPATIBLE_JDK_LEVEL,
//		PREF_PB_CIRCULAR_BUILDPATH, PREF_BUILD_CLEAN_OUTPUT_FOLDER, PREF_PB_DUPLICATE_RESOURCE,
		PREF_ENABLE_EXCLUSION_PATTERNS, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS
	};	
	
	protected String[] getAllKeys() {
		return KEYS;	
	}
	
	protected final Map getOptions(boolean inheritJavaCoreOptions) {
		Map map= super.getOptions(inheritJavaCoreOptions);
		return map;
	}
	
	protected final Map getDefaultOptions() {
		Map map= super.getDefaultOptions();
		return map;
	}	
	
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
//		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite othersComposite= createBuildPathTabContent(parent);
		othersComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
				
		validateSettings(null, null);
	
		return othersComposite;
	}

	private Composite createBuildPathTabContent(Composite folder) {
//		String[] abortIgnoreValues= new String[] { ABORT, IGNORE };
//		String[] cleanIgnoreValues= new String[] { CLEAN, IGNORE };
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };
		
//		String[] errorWarning= new String[] { ERROR, WARNING };
		
//		String[] errorWarningLabels= new String[] {
//			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
//			PreferencesMessages.getString("CompilerConfigurationBlock.warning") //$NON-NLS-1$
//		};
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;

		Composite othersComposite= new Composite(folder, SWT.NULL);
		othersComposite.setLayout(layout);
		
//		Label description= new Label(othersComposite, SWT.WRAP);
//		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.build_warnings.description")); //$NON-NLS-1$
//		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
//		gd.horizontalSpan= nColumns;
//		description.setLayoutData(gd);
//				
//		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_incomplete_build_path.label"); //$NON-NLS-1$
//		addComboBox(othersComposite, label, PREF_PB_INCOMPLETE_BUILDPATH, errorWarning, errorWarningLabels, 0);	
//		
//		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_build_path_cycles.label"); //$NON-NLS-1$
//		addComboBox(othersComposite, label, PREF_PB_CIRCULAR_BUILDPATH, errorWarning, errorWarningLabels, 0);
//
//		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_duplicate_resources.label"); //$NON-NLS-1$
//		addComboBox(othersComposite, label, PREF_PB_DUPLICATE_RESOURCE, errorWarning, errorWarningLabels, 0);
//
//		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
//		
//		String[] errorWarningIgnoreLabels= new String[] {
//			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
//			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
//			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
//		};
//		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_check_prereq_binary_level.label");  //$NON-NLS-1$
//		addComboBox(othersComposite, label, PREF_PB_INCOMPATIBLE_JDK_LEVEL, errorWarningIgnore, errorWarningIgnoreLabels, 0);	
//
//		label= PreferencesMessages.getString("CompilerConfigurationBlock.build_invalid_classpath.label"); //$NON-NLS-1$
//		addCheckBox(othersComposite, label, PREF_BUILD_INVALID_CLASSPATH, abortIgnoreValues, 0);
//
//		label= PreferencesMessages.getString("CompilerConfigurationBlock.build_clean_outputfolder.label"); //$NON-NLS-1$
//		addCheckBox(othersComposite, label, PREF_BUILD_CLEAN_OUTPUT_FOLDER, cleanIgnoreValues, 0);

		String label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_exclusion_patterns.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_EXCLUSION_PATTERNS, enableDisableValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_multiple_outputlocations.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS, enableDisableValues, 0);
		
		
//		description= new Label(othersComposite, SWT.WRAP);
//		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.resource_filter.description")); //$NON-NLS-1$
//		gd= new GridData(GridData.FILL);
//		gd.horizontalSpan= nColumns;
//		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
//		description.setLayoutData(gd);
//		
//		label= PreferencesMessages.getString("CompilerConfigurationBlock.resource_filter.label"); //$NON-NLS-1$
//		Text text= addTextField(othersComposite, label, PREF_RESOURCE_FILTER, 0, 0);
//		gd= (GridData) text.getLayoutData();
//		gd.grabExcessHorizontalSpace= true;
//		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(10);
		
		return othersComposite;
	}
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(String changedKey, String newValue) {
		if (changedKey != null) {
			//if (PREF_RESOURCE_FILTER.equals(changedKey)) {
			//	fResourceFilterStatus= validateResourceFilters();
			//} else {
				return;
			//}
		} else {
			updateEnableStates();
//			fResourceFilterStatus= validateResourceFilters();
		}		


		IStatus status= fResourceFilterStatus;//StatusUtil.getMostSevere(new IStatus[] {fResourceFilterStatus });
		fContext.statusChanged(status);
	}
	
	private void updateEnableStates() {
	}
	
//	private IStatus validateResourceFilters() {
//		String text= (String) fWorkingValues.get(PREF_RESOURCE_FILTER);
//		
//		IWorkspace workspace= ResourcesPlugin.getWorkspace();
//
//		String[] filters= getTokens(text, ","); //$NON-NLS-1$
//		for (int i= 0; i < filters.length; i++) {
//			String fileName= filters[i].replace('*', 'x');
//			int resourceType= IResource.FILE;
//			int lastCharacter= fileName.length() - 1;
//			if (lastCharacter >= 0 && fileName.charAt(lastCharacter) == '/') {
//				fileName= fileName.substring(0, lastCharacter);
//				resourceType= IResource.FOLDER;
//			}
//			IStatus status= workspace.validateName(fileName, resourceType);
//			if (status.matches(IStatus.ERROR)) {
//				String message= PreferencesMessages.getFormattedString("CompilerConfigurationBlock.filter.invalidsegment.error", status.getMessage()); //$NON-NLS-1$
//				return new StatusInfo(IStatus.ERROR, message);
//			}
//		}
//		return new StatusInfo();
//	}
			
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
//		String title= PreferencesMessages.getString("CompilerConfigurationBlock.needsbuild.title"); //$NON-NLS-1$
//		String message;
//		if (workspaceSettings) {
//			message= PreferencesMessages.getString("CompilerConfigurationBlock.needsfullbuild.message"); //$NON-NLS-1$
//		} else {
//			message= PreferencesMessages.getString("CompilerConfigurationBlock.needsprojectbuild.message"); //$NON-NLS-1$
//		}
//		return new String[] { title, message };
		return null; 
	}	
	
}
