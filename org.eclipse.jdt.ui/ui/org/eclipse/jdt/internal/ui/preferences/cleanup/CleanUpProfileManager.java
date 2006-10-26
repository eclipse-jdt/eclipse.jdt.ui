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
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.formatter.IProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;

public class CleanUpProfileManager extends ProfileManager {
	
	private final static String ECLIPSE_PROFILE= "org.eclipse.jdt.ui.default.eclipse_clean_up_profile"; //$NON-NLS-1$
	private final static String SAVE_PARTICIPANT_PROFILE= "org.eclipse.jdt.ui.default.save_participant_clean_up_profile"; //$NON-NLS-1$
	public final static String DEFAULT_PROFILE= ECLIPSE_PROFILE;
	public final static String DEFAULT_SAVE_PARTICIPANT_PROFILE= SAVE_PARTICIPANT_PROFILE;
  
	public static KeySet[] KEY_SETS= {
		new KeySet(JavaUI.ID_PLUGIN, new ArrayList(CleanUpConstants.getEclipseDefaultSettings().keySet()))		
	};
	
	public final static String PROFILE_KEY= "cleanup_profile"; //$NON-NLS-1$
	private final static String FORMATTER_SETTINGS_VERSION= "cleanup_settings_version";  //$NON-NLS-1$
	private final PreferencesAccess fPreferencesAccess;

	public CleanUpProfileManager(List profiles, IScopeContext context, PreferencesAccess preferencesAccess, IProfileVersioner profileVersioner) {
	    super(addBuiltInProfiles(profiles, profileVersioner), context, preferencesAccess, profileVersioner, KEY_SETS, PROFILE_KEY, FORMATTER_SETTINGS_VERSION);
		fPreferencesAccess= preferencesAccess;
    }
	
	public static List addBuiltInProfiles(List profiles, IProfileVersioner profileVersioner) {
		final Profile eclipseProfile= new BuiltInProfile(ECLIPSE_PROFILE, CleanUpMessages.CleanUpProfileManager_ProfileName_EclipseBuildIn, CleanUpConstants.getEclipseDefaultSettings(), 2, profileVersioner.getCurrentVersion(), profileVersioner.getProfileKind());
		profiles.add(eclipseProfile);
		
		final Profile saveParticipantProfile= new BuiltInProfile(SAVE_PARTICIPANT_PROFILE, CleanUpMessages.CleanUpProfileManager_save_participant_profileName, CleanUpConstants.getSaveParticipantSettings(), 1, profileVersioner.getCurrentVersion(), profileVersioner.getProfileKind());
		profiles.add(saveParticipantProfile);
		
		return profiles;
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ProfileManager#getDefaultProfile()
     */
    public Profile getDefaultProfile() {
    	return getProfile(DEFAULT_PROFILE);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void updateProfilesWithName(String oldName, Profile newProfile, boolean applySettings) {
        super.updateProfilesWithName(oldName, newProfile, applySettings);
        
        IEclipsePreferences node= fPreferencesAccess.getInstanceScope().getNode(JavaUI.ID_PLUGIN);
        String name= node.get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
        if (name != null && name.equals(oldName)) {
        	if (newProfile == null) {
        		node.remove(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE);	
        	} else {
    			node.put(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, newProfile.getID());
        	}
    	}
    }

}
