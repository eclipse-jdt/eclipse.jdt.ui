/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpMessages;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.BuiltInProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.KeySet;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;

public class CleanUpPreferenceUtil {

	public static final String SAVE_PARTICIPANT_KEY_PREFIX= CleanUpPreferenceUtilCore.SAVE_PARTICIPANT_KEY_PREFIX;

	public static Map<String, String> loadOptions(IScopeContext context) {
    	return loadOptions(context, CleanUpConstants.CLEANUP_PROFILE, CleanUpConstants.DEFAULT_PROFILE);
    }

	private static Map<String, String> loadOptions(IScopeContext context, String profileIdKey, String defaultProfileId) {
    	IEclipsePreferences contextNode= context.getNode(JavaUI.ID_PLUGIN);
    	String id= contextNode.get(profileIdKey, null);

    	if (id != null && ProjectScope.SCOPE.equals(context.getName())) {
    		return loadFromProject(context);
    	}

		IScopeContext instanceScope= InstanceScope.INSTANCE;
    	if (id == null) {
    		if (ProjectScope.SCOPE.equals(context.getName())) {
    			id= instanceScope.getNode(JavaUI.ID_PLUGIN).get(profileIdKey, null);
    		}
    		if (id == null) {
				id= DefaultScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).get(profileIdKey, defaultProfileId);
    		}
    	}

    	for (Profile profile : getBuiltInProfiles()) {
			if (id.equals(profile.getID()))
				return profile.getSettings();
		}

    	if (CleanUpConstants.SAVE_PARTICIPANT_PROFILE.equals(id))
    		return JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS).getMap();

    	CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
        ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);

        List<Profile> list= null;
        try {
            list= profileStore.readProfiles(instanceScope);
        } catch (CoreException e1) {
            JavaPlugin.log(e1);
        }
        if (list == null)
        	return null;

		for (Profile profile : list) {
			if (id.equals(profile.getID()))
				return profile.getSettings();
		}

    	return null;
    }

	private static Map<String, String> loadFromProject(IScopeContext context) {
		final Map<String, String> profileOptions= new HashMap<>();
		IEclipsePreferences uiPrefs= context.getNode(JavaUI.ID_PLUGIN);

    	CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();

    	CleanUpOptions defaultOptions= JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);

    	boolean hasValues= false;
		for (KeySet keySet : CleanUpProfileManager.KEY_SETS) {
			IEclipsePreferences preferences= context.getNode(keySet.getNodeName());
			for (String key : keySet.getKeys()) {
				String val= preferences.get(key, null);
				if (val != null) {
					hasValues= true;
				} else {
					val= defaultOptions.getValue(key);
				}
				profileOptions.put(key, val);
			}
		}

		if (!hasValues)
			return null;

		int version= uiPrefs.getInt(CleanUpConstants.CLEANUP_SETTINGS_VERSION_KEY, versioner.getFirstVersion());
		if (version == versioner.getCurrentVersion())
			return profileOptions;

		CustomProfile profile= new CustomProfile("tmp", profileOptions, version, versioner.getProfileKind()); //$NON-NLS-1$
		versioner.update(profile);
		return profile.getSettings();
    }

	public static Map<String, String> loadSaveParticipantOptions(IScopeContext context) {
		IEclipsePreferences node;
		if (hasSettingsInScope(context)) {
			node= context.getNode(JavaUI.ID_PLUGIN);
		} else {
			IScopeContext instanceScope= InstanceScope.INSTANCE;
			if (hasSettingsInScope(instanceScope)) {
				node= instanceScope.getNode(JavaUI.ID_PLUGIN);
			} else {
				return JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS).getMap();
			}
		}

		Map<String, String> result= new HashMap<>();
		for (String key : JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS).getKeys()) {
			result.put(key, node.get(SAVE_PARTICIPANT_KEY_PREFIX + key, CleanUpOptions.FALSE));
		}

		return result;
	}

    public static void saveSaveParticipantOptions(IScopeContext context, Map<String, String> settings) {
    	IEclipsePreferences node= context.getNode(JavaUI.ID_PLUGIN);
		for (Map.Entry<String, String> entry : settings.entrySet()) {
			String key = entry.getKey();
			node.put(SAVE_PARTICIPANT_KEY_PREFIX + key, entry.getValue());
		}
    }

	public static boolean hasSettingsInScope(IScopeContext context) {
    	IEclipsePreferences node= context.getNode(JavaUI.ID_PLUGIN);

    	for (String key : JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS).getKeys()) {
			if (node.get(SAVE_PARTICIPANT_KEY_PREFIX + key, null) != null)
				return true;
		}

    	return false;
    }

	/**
	 * Returns a list of {@link org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile} stored in the <code>scope</code>,
	 * including the built-in profiles.
	 * @param scope the context from which to retrieve the profiles
	 * @return list of profiles, not null
	 * @since 3.3
	 */
	public static List<Profile> loadProfiles(IScopeContext scope) {

        CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
    	ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);

    	List<Profile> list= null;
        try {
            list= profileStore.readProfiles(scope);
        } catch (CoreException e1) {
            JavaPlugin.log(e1);
        }
        if (list == null) {
        	list= getBuiltInProfiles();
        } else {
        	list.addAll(getBuiltInProfiles());
        }

        return list;
    }

	/**
	 * Returns a list of built in clean up profiles
	 * @return the list of built in profiles, not null
	 * @since 3.3
	 */
	public static List<Profile> getBuiltInProfiles() {
    	ArrayList<Profile> result= new ArrayList<>();

    	Map<String, String> settings= JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS).getMap();
    	final Profile eclipseProfile= new BuiltInProfile(CleanUpConstants.ECLIPSE_PROFILE, CleanUpMessages.CleanUpProfileManager_ProfileName_EclipseBuildIn, settings, 2, CleanUpProfileVersioner.CURRENT_VERSION, CleanUpProfileVersioner.PROFILE_KIND);
    	result.add(eclipseProfile);

    	return result;
    }

	private CleanUpPreferenceUtil() {
	}

}
