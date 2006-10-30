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
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

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

import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.ISaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.preferences.CleanUpPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpModifyDialog;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
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
	
	private static final String CLEANUP_PAGE_SETTINGS_KEY= "cleanup_page"; //$NON-NLS-1$
	private static final String DIALOGSTORE_LASTSAVELOADPATH= JavaUI.ID_PLUGIN + ".cleanup"; //$NON-NLS-1$

	private ComboDialogField fProfileSelectionField;
	private PreferencesAccess fPreferencesAccess;
	private ProfileManager fProfileManager;
	private ProfileStore fProfileStore;
	private IPreferenceChangeListener fPreferenceListener;

	/**
	 * {@inheritDoc}
	 */
	public Control createControl(Composite parent, PreferencePage page) {
		IPreferencePageContainer container= page.getContainer();
		IWorkingCopyManager workingCopyManager;
		if (container instanceof IWorkbenchPreferenceContainer) {
			workingCopyManager= ((IWorkbenchPreferenceContainer)container).getWorkingCopyManager();
		} else {
			workingCopyManager= new WorkingCopyManager(); // non shared
		}
		fPreferencesAccess= PreferencesAccess.getWorkingCopyPreferences(workingCopyManager);

		CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
		fProfileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
		List profiles= null;
		try {
			profiles= fProfileStore.readProfiles(new InstanceScope());
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		if (profiles == null)
			profiles= new ArrayList();
		CleanUpProfileManager.addBuiltInProfiles(profiles, versioner);

		fProfileManager= new ProfileManager(profiles, new InstanceScope(), fPreferencesAccess, versioner, CleanUpProfileManager.KEY_SETS, CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, "cleanup_settings_version") { //$NON-NLS-1$
			public Profile getDefaultProfile() {
				return getProfile(CleanUpProfileManager.DEFAULT_SAVE_PARTICIPANT_PROFILE);
			}
		};

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
		fProfileSelectionField.doFillIntoGrid(composite, 2);
		fillProfileListDialogField();
		fProfileManager.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				fillProfileListDialogField();
			}
		});

		Button edit= new Button(composite, SWT.NONE);
		edit.setText(MultiFixMessages.CleanUpSaveParticipantPreferenceConfiguration_edit_button_label);
		GridData gd= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		gd.widthHint= SWTUtil.getButtonWidthHint(edit);
		edit.setLayoutData(gd);
		edit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				editSelectedProfile();
			}
		});

		final boolean[] listenerEnabled= new boolean[] {true};
		fProfileManager.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				try {
					listenerEnabled[0]= false;
					final int value= ((Integer)arg).intValue();
					switch (value) {
					case ProfileManager.PROFILE_DELETED_EVENT:
					case ProfileManager.PROFILE_RENAMED_EVENT:
					case ProfileManager.PROFILE_CREATED_EVENT:
					case ProfileManager.SETTINGS_CHANGED_EVENT:
						try {
							fProfileStore.writeProfiles(fProfileManager.getSortedProfiles(), fPreferencesAccess.getInstanceScope());
							fProfileManager.commitChanges(fPreferencesAccess.getInstanceScope());
						} catch (CoreException x) {
							JavaPlugin.log(x);
						}
						break;
					case ProfileManager.SELECTION_CHANGED_EVENT:
						fProfileManager.commitChanges(fPreferencesAccess.getInstanceScope());
						break;
					}
				} finally {
					listenerEnabled[0]= true;
				}
			}
		});

		fPreferenceListener= new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				if (listenerEnabled[0]) {
					if (CleanUpConstants.CLEANUP_PROFILES.equals(event.getKey())) {
						try {
							String id= fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
							if (id == null)
								fProfileManager.getDefaultProfile().getID();
							
							List oldProfiles= fProfileManager.getSortedProfiles();
							Profile[] oldProfilesArray= (Profile[])oldProfiles.toArray(new Profile[oldProfiles.size()]);
							for (int i= 0; i < oldProfilesArray.length; i++) {
								if (oldProfilesArray[i] instanceof CustomProfile) {
									fProfileManager.deleteProfile((CustomProfile)oldProfilesArray[i]);
								}
							}

							List newProfiles= fProfileStore.readProfilesFromString((String)event.getNewValue());
							for (Iterator iterator= newProfiles.iterator(); iterator.hasNext();) {
								CustomProfile profile= (CustomProfile)iterator.next();
								fProfileManager.addProfile(profile);
							}

							Profile profile= fProfileManager.getProfile(id);
							if (profile != null) {
								fProfileManager.setSelected(profile);
							} else {
								fProfileManager.setSelected(fProfileManager.getDefaultProfile());
							}
						} catch (CoreException e) {
							JavaPlugin.log(e);
						}
					} else if (CleanUpConstants.CLEANUP_ON_SAVE_PROFILE.equals(event.getKey())) {
						if (event.getNewValue() == null) {
							fProfileManager.setSelected(fProfileManager.getDefaultProfile());
						} else {
    						Profile profile= fProfileManager.getProfile((String)event.getNewValue());
    						if (profile != null) {
    							fProfileManager.setSelected(profile);
    						}
						}
					}
				}
			}
		};
		fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).addPreferenceChangeListener(fPreferenceListener);

		return composite;
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (fPreferenceListener != null) {
			fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN).removePreferenceChangeListener(fPreferenceListener);
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
		Profile profile= fProfileManager.getDefaultProfile();
		if (profile != null) {
			int defaultIndex= fProfileManager.getSortedProfiles().indexOf(profile);
			if (defaultIndex != -1) {
				fProfileManager.setSelected(profile);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void performOk() {
		dispose();
		try {
			fPreferencesAccess.applyChanges();
		} catch (BackingStoreException e) {
			JavaPlugin.log(e);
		}
	}

	private void editSelectedProfile() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		CleanUpModifyDialog dialog= new CleanUpModifyDialog(shell, fProfileManager.getSelected(), fProfileManager, fProfileStore, false, CLEANUP_PAGE_SETTINGS_KEY, DIALOGSTORE_LASTSAVELOADPATH);
		dialog.open();
	}

	private void fillProfileListDialogField() {
		fProfileSelectionField.setDialogFieldListener(null);
		fProfileSelectionField.setItems(fProfileManager.getSortedDisplayNames());
		fProfileSelectionField.selectItem(fProfileManager.getSelected().getName());
		fProfileSelectionField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				fProfileManager.setSelected((Profile)fProfileManager.getSortedProfiles().get(fProfileSelectionField.getSelectionIndex()));
			}
		});
	}
}
