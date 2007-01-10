/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.preference.IPreferencePageContainer;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.AbstractSaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.util.PixelConverter;

/**
 * Preference configuration UI for the clean up save participant.
 * 
 * @since 3.3
 */
public class CleanUpSaveParticipantPreferenceConfiguration extends AbstractSaveParticipantPreferenceConfiguration {
	
	private static final int INDENT= 10;
	
	private IScopeContext fContext;
	private Map fSettings;
	private Text fSelectedActionsText;
	private Button fFormatCodeButton;
	private Button fOrganizeImportsButton;
	private Shell fShell;
	private Link fFormatConfigLink;
	private Link fOrganizeImportsConfigLink;
	private IPreferencePageContainer fContainer;
	private Button fAdditionalActionButton;
	private Composite fAdvancedComposite;
	private ControlEnableState fAdvancedCompositeEnableState;
	
	/**
	 * {@inheritDoc}
	 */
	public Control createConfigControl(final Composite parent, IPreferencePageContainer container) {
		fContainer= container;
		fShell= parent.getShell();
		
		final Composite composite= new Composite(parent, SWT.NONE);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalIndent= INDENT;
		composite.setLayoutData(gridData);
		GridLayout gridLayout= new GridLayout(1, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		composite.setLayout(gridLayout);
		
		fFormatCodeButton= new Button(composite, SWT.CHECK);
		fFormatCodeButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_FormatSource_Checkbox);
		fFormatCodeButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fFormatCodeButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 */
			public void widgetSelected(SelectionEvent e) {
				changeSettingsValue(CleanUpConstants.FORMAT_SOURCE_CODE, fFormatCodeButton.getSelection());
			}
		});
		
		PixelConverter pixelConverter= new PixelConverter(parent);
		int heightOneHalf= (int)Math.round(pixelConverter.convertHeightInCharsToPixels(1) * 1.5);
		
		fFormatConfigLink= new Link(composite, SWT.NONE);
		fFormatConfigLink.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_ConfigureFormatter_Link);
		GridData gridData2= new GridData(SWT.LEFT, SWT.TOP, false, true);
		gridData2.horizontalIndent= 20;
		gridData2.minimumHeight= heightOneHalf;
		fFormatConfigLink.setLayoutData(gridData2);
		
		fOrganizeImportsButton= new Button(composite, SWT.CHECK);
		fOrganizeImportsButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_OrganizeImports_Checkbox);
		fOrganizeImportsButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fOrganizeImportsButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 */
			public void widgetSelected(SelectionEvent e) {
				changeSettingsValue(CleanUpConstants.ORGANIZE_IMPORTS, fOrganizeImportsButton.getSelection());
			}
		});
		
		fOrganizeImportsConfigLink= new Link(composite, SWT.NONE);
		fOrganizeImportsConfigLink.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_ConfigureImports_Link);
		GridData gridData3= new GridData(SWT.LEFT, SWT.TOP, false, true);
		gridData3.horizontalIndent= 20;
		gridData3.minimumHeight= heightOneHalf;
		fOrganizeImportsConfigLink.setLayoutData(gridData3);
		
		fAdditionalActionButton= new Button(composite, SWT.CHECK);
		fAdditionalActionButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_AdditionalActions_Checkbox);
		
		fAdvancedComposite= createAdvancedComposite(composite);
		fAdditionalActionButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 */
			public void widgetSelected(SelectionEvent e) {
				changeSettingsValue(CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, fAdditionalActionButton.getSelection());
			}
		});
		
		return composite;
	}
	
	private Composite createAdvancedComposite(final Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalIndent= INDENT;
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout(2, false));
		
		fSelectedActionsText= new Text(composite, SWT.BORDER | SWT.FLAT | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
		final GridData data= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		data.heightHint= new PixelConverter(composite).convertHeightInCharsToPixels(8);
		fSelectedActionsText.setLayoutData(data);
		
		Button configureButton= new Button(composite, SWT.NONE);
		configureButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_Configure_Button);
		configureButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		configureButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 */
			public void widgetSelected(SelectionEvent e) {
				new CleanUpSaveParticipantConfigurationModifyDialog(parent.getShell(), fSettings, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_CleanUpSaveParticipantConfiguration_Title).open();
				settingsChanged();
			}
			
		});
		
		return composite;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initialize(final IScopeContext context, IAdaptable element) {
		fContext= context;
		fSettings= CleanUpPreferenceUtil.loadSaveParticipantOptions(context);
		
		settingsChanged();
		
		IJavaProject javaProject= null;
		if (element != null) {
			IProject project= (IProject)element.getAdapter(IProject.class);
			if (project != null) {
				IJavaProject jProject= JavaCore.create(project);
				if (jProject != null && jProject.exists()) {
					javaProject= jProject;
				}
			}
		}
		
		configurePreferenceLink(fFormatConfigLink, javaProject, CodeFormatterPreferencePage.PREF_ID, CodeFormatterPreferencePage.PROP_ID);
		configurePreferenceLink(fOrganizeImportsConfigLink, javaProject, ImportOrganizePreferencePage.PREF_ID, ImportOrganizePreferencePage.PROP_ID);
		
		super.initialize(context, element);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void performDefaults() {
		fSettings= CleanUpConstants.getSaveParticipantSettings();
		settingsChanged();
		
		super.performDefaults();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void performOk() {
		super.performOk();
		
		if (!ProjectScope.SCOPE.equals(fContext.getName()) || hasSettingsInScope(fContext))
			CleanUpPreferenceUtil.saveSaveParticipantOptions(fContext, fSettings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void enableProjectSettings() {
		super.enableProjectSettings();
		
		CleanUpPreferenceUtil.saveSaveParticipantOptions(fContext, fSettings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void disableProjectSettings() {
		super.disableProjectSettings();
		
		IEclipsePreferences node= fContext.getNode(JavaUI.ID_PLUGIN);
		
		Map settings= CleanUpConstants.getSaveParticipantSettings();
		for (Iterator iterator= settings.keySet().iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			node.remove(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + key);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected String getPostSaveListenerId() {
		return CleanUpPostSaveListener.POSTSAVELISTENER_ID;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected String getPostSaveListenerName() {
		return SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_CleanUpActionsTopNodeName_Checkbox;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void enableConfigControl(boolean isEnabled) {
		super.enableConfigControl(isEnabled);
		
		updateAdvancedEnableState();
	}
	
	private void settingsChanged() {
		fFormatCodeButton.setSelection(CleanUpConstants.TRUE.equals(fSettings.get(CleanUpConstants.FORMAT_SOURCE_CODE)));
		fOrganizeImportsButton.setSelection(CleanUpConstants.TRUE.equals(fSettings.get(CleanUpConstants.ORGANIZE_IMPORTS)));
		fAdditionalActionButton.setSelection(CleanUpConstants.TRUE.equals(fSettings.get(CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS)));
		
		updateAdvancedEnableState();
		
		Map settings= new HashMap(fSettings);
		settings.put(CleanUpConstants.FORMAT_SOURCE_CODE, CleanUpConstants.FALSE);
		settings.put(CleanUpConstants.ORGANIZE_IMPORTS, CleanUpConstants.FALSE);
		
		final ICleanUp[] cleanUps= CleanUpRefactoring.createCleanUps(settings);
		
		boolean hasCleanUp= false;
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < cleanUps.length; i++) {
			String[] descriptions= cleanUps[i].getDescriptions();
			if (descriptions != null) {
				for (int j= 0; j < descriptions.length; j++) {
					buf.append('-').append(' ').append(descriptions[j]).append('\n');
					hasCleanUp= true;
				}
			}
		}
		if (!hasCleanUp) {
			buf.append(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_NoActionEnabled_Info);
		}
		fSelectedActionsText.setText(buf.toString());
	}
	
	private void updateAdvancedEnableState() {
		boolean additionalEnabled= CleanUpConstants.TRUE.equals(fSettings.get(CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS));
		if (additionalEnabled) {
			if (fAdvancedCompositeEnableState != null) {
				fAdvancedCompositeEnableState.restore();
				fAdvancedCompositeEnableState= null;
			}
		} else {
			if (fAdvancedComposite.isEnabled())
				fAdvancedCompositeEnableState= ControlEnableState.disable(fAdvancedComposite);
		}
	}
	
	private void configurePreferenceLink(Link link, final IJavaProject javaProject, final String preferenceId, final String propertyId) {
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fContainer instanceof IWorkbenchPreferenceContainer) {
					IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer)fContainer;
					if (javaProject != null) {
						container.openPage(propertyId, null);
					} else {
						container.openPage(preferenceId, null);
					}
				} else {
					PreferencesUtil.createPreferenceDialogOn(fShell, preferenceId, null, null);
				}
			}
		});
	}
	
	private void changeSettingsValue(String key, boolean enabled) {
		String value;
		if (enabled) {
			value= CleanUpConstants.TRUE;
		} else {
			value= CleanUpConstants.FALSE;
		}
		fSettings.put(key, value);
		settingsChanged();
	}
}