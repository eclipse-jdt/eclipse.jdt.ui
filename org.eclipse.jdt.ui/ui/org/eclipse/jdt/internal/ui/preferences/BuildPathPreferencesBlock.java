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
	
	private static final String PREF_ENABLE_EXCLUSION_PATTERNS= JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS;
	private static final String PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS= JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;

	private IStatus  fResourceFilterStatus;

	public BuildPathPreferencesBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project, getKeys());
		fResourceFilterStatus= new StatusInfo();
	}
	
	protected static String[] getKeys() {
		return new String[] {
			PREF_ENABLE_EXCLUSION_PATTERNS, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS
		};	
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
		setShell(parent.getShell());

		Composite othersComposite= createBuildPathTabContent(parent);
		othersComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
				
		validateSettings(null, null);
	
		return othersComposite;
	}

	private Composite createBuildPathTabContent(Composite folder) {
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };

		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;

		Composite othersComposite= new Composite(folder, SWT.NULL);
		othersComposite.setLayout(layout);

		String label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_exclusion_patterns.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_EXCLUSION_PATTERNS, enableDisableValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_multiple_outputlocations.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS, enableDisableValues, 0);
		
		return othersComposite;
	}
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(String changedKey, String newValue) {
		if (changedKey != null) {
			return;
		} else {
			updateEnableStates();
		}		

		IStatus status= fResourceFilterStatus;
		fContext.statusChanged(status);
	}
	
	private void updateEnableStates() {
	}
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null; 
	}	
	
}
