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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.WorkingCopyManager;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.ISaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.preferences.CleanUpPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Preference configuration UI for the clean up save participant.
 * 
 * @since 3.3
 */
public class CleanUpSaveParticipantPreferenceConfiguration implements ISaveParticipantPreferenceConfiguration {

	private ComboDialogField fProfileSelectionField;
	private IPreferenceChangeListener fPreferenceListener;
	private PreferencesAccess fPreferencesAccess;

	/**
	 * {@inheritDoc}
	 */
	public Control createControl(Composite parent) {
		fPreferencesAccess= PreferencesAccess.getWorkingCopyPreferences(new WorkingCopyManager());
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(3, false));
		
		Link link= new Link(composite, SWT.NONE);
		link.setText(MultiFixMessages.CleanUpSaveParticipantPreferenceConfiguration_clean_up_preference_link);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(composite.getShell(), CleanUpPreferencePage.PREF_ID, null, null);
			}
		});
		GridData data= new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalSpan= 3;
		link.setLayoutData(data);
		
		fProfileSelectionField= new ComboDialogField(SWT.READ_ONLY);
		fProfileSelectionField.setLabelText(MultiFixMessages.CleanUpSaveParticipantPreferenceConfiguration_use_clean_up_profile_label);
		populateProfileSelection();
		fProfileSelectionField.doFillIntoGrid(composite, 2);
		
		fPreferenceListener= new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				if (CleanUpConstants.CLEANUP_PROFILES.equals(event.getKey())) {
					String value= new InstanceScope().getNode(JavaUI.ID_PLUGIN).get(CleanUpConstants.CLEANUP_PROFILES, null);
					fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).put(CleanUpConstants.CLEANUP_PROFILES, value);
					
					fProfileSelectionField.setDialogFieldListener(null);
					populateProfileSelection();
				} else if (CleanUpConstants.CLEANUP_ON_SAVE_PROFILE.equals(event.getKey())) {
					String value= new InstanceScope().getNode(JavaUI.ID_PLUGIN).get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
					if (value != null) {
						fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).put(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, value);
					} else {
						fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).remove(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE);
					}
					
					fProfileSelectionField.setDialogFieldListener(null);
					populateProfileSelection();
				}
			}
		};
		new InstanceScope().getNode(JavaUI.ID_PLUGIN).addPreferenceChangeListener(fPreferenceListener);
				
		return composite;
	}

	private void populateProfileSelection() {
	    Hashtable profilesTable= loadProfiles();
		final Collection profiles= profilesTable.values();
		
		String[] items= new String[profiles.size()];
		int i= 0;
		for (Iterator iterator= profiles.iterator(); iterator.hasNext();) {
	        Profile profile= (Profile)iterator.next();
	        items[i]= profile.getName();
	        i++;
        }
		Arrays.sort(items);
		fProfileSelectionField.setItems(items);
		
		IEclipsePreferences node= fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN);
		String id= node.get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
		if (id == null) {
			id= fPreferencesAccess.getDefaultScope().getNode(JavaUI.ID_PLUGIN).get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
			if (id == null) {
				id= CleanUpProfileManager.DEFAULT_SAVE_PARTICIPANT_PROFILE;
			}
		}
		Profile selectedProfile= (Profile)profilesTable.get(id);
		if (selectedProfile != null) {
			fProfileSelectionField.selectItem(selectedProfile.getName());
		} else {
			fProfileSelectionField.setTextWithoutUpdate(MultiFixMessages.CleanUpSaveParticipantPreferenceConfiguration_unknown_profile_name);
		}
		fProfileSelectionField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				String name= fProfileSelectionField.getItems()[fProfileSelectionField.getSelectionIndex()];
				Profile profile= getProfileWithName(name, profiles);
				if (profile != null) {
					fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).put(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, profile.getID());
				}
            }
		});
    }
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (fPreferenceListener != null) {
			new InstanceScope().getNode(JavaUI.ID_PLUGIN).removePreferenceChangeListener(fPreferenceListener);
			fPreferenceListener= null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void initialize() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void performDefaults() {
		fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).remove(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE);
		fProfileSelectionField.setDialogFieldListener(null);
		populateProfileSelection();
	}

	/**
	 * {@inheritDoc}
	 */
	public void performOk() {
		try {
	        fPreferencesAccess.applyChanges();
        } catch (BackingStoreException e) {
	        JavaPlugin.log(e);
        }
	}
	
	private Hashtable loadProfiles() {
		IScopeContext instanceScope= fPreferencesAccess.getInstanceScope();
		
        CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
		ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
		
		List list= null;
        try {
            list= profileStore.readProfiles(instanceScope);
        } catch (CoreException e1) {
            JavaPlugin.log(e1);
        }
        if (list == null)
        	list= new ArrayList();
        
		CleanUpProfileManager.addBuiltInProfiles(list, versioner);
		
		Hashtable profileIdsTable= new Hashtable();
		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
            Profile profile= (Profile)iterator.next();
            profileIdsTable.put(profile.getID(), profile);
        }
     
		return profileIdsTable;
    }
	
	private Profile getProfileWithName(String name, Collection profiles) {
		for (Iterator iterator= profiles.iterator(); iterator.hasNext();) {
            Profile profile= (Profile)iterator.next();
            if (profile.getName().equals(name))
            	return profile;
        }
		return null;
    }
}
