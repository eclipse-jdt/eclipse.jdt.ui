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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;


/**
 * The model for the set of profiles which are available in the workbench.
 */
public class ProfileManager extends Observable {
	
    /**
     * A prefix which is prepended to every ID of a user-defined profile, in order
     * to differentiate it from a built-in profile.
     */
	private final static String ID_PREFIX= "_"; //$NON-NLS-1$
	
	/**
	 * Represents a profile with a unique ID, a name and a map 
	 * containing the code formatter settings.
	 */
	public static abstract class Profile implements Comparable {
		
		public abstract String getName();
		public abstract Profile rename(String name);
		
		public abstract Map getSettings();
		public abstract void setSettings(Map settings);
		
		public int getVersion() {
			return ProfileVersioner.CURRENT_VERSION;
		}
		
		public boolean hasEqualSettings(Map otherMap, List allKeys) {
			Map settings= getSettings();
			for (Iterator iter= allKeys.iterator(); iter.hasNext(); ){
				String key= (String) iter.next();
				Object other= otherMap.get(key);
				Object curr= settings.get(key);
				if (other == null) {
					if (curr != null) {
						return false;
					}
				} else if (!other.equals(curr)) {
					return false;
				}
			}
			return true;
		}
		
		public abstract boolean isProfileToSave();
		
		public abstract String getID();
	}
	
	/**
	 * Represents a built-in profile. The state of a built-in profile 
	 * cannot be changed after instantiation.
	 */
	public final static class BuiltInProfile extends Profile {
		private final String fName;
		private final String fID;
		private final Map fSettings;
		private final int fOrder;
		
		protected BuiltInProfile(String ID, String name, Map settings, int order) {
			fName= name;
			fID= ID;
			fSettings= settings;
			fOrder= order;
		}
		
		public String getName() { 
			return fName;	
		}
		
		public Profile rename(String name) {
			return this;
		}
		
		public Map getSettings() {
			return fSettings;
		}
	
		public void setSettings(Map settings) {
		}
	
		public String getID() { 
			return fID; 
		}
		
		public final int compareTo(Object o) {
			if (o instanceof BuiltInProfile) {
				return fOrder - ((BuiltInProfile)o).fOrder;
			}
			return -1;
		}

		public boolean isProfileToSave() {
			return false;
		}
	
	}

	/**
	 * Represents a user-defined profile. A custom profile can be modified after instantiation.
	 */
	public static class CustomProfile extends Profile {
		private String fName;
		private Map fSettings;
		protected ProfileManager fManager;
		private int fVersion;

		public CustomProfile(String name, Map settings, int version) {
			fName= name;
			fSettings= settings;
			fVersion= version;
		}
		
		public String getName() {
			return fName;
		}
		
		public Profile rename(String name) {
			final String trimmed= name.trim();
			if (trimmed.equals(getName())) 
				return this;
			
			final String oldID= getID();
			fName= trimmed;
			
			if (fManager != null) { 
				fManager.fProfiles.remove(oldID);
				fManager.fProfiles.put(getID(), this);
				Collections.sort(fManager.fProfilesByName);
				notifyObservers(PROFILE_RENAMED_EVENT);
			}
			return this;
		}
		
		public Map getSettings() { 
			return fSettings;
		}
		
		public void setSettings(Map settings) {
			if (settings == null)
				throw new IllegalArgumentException();
			fSettings= settings;
			notifyObservers(SETTINGS_CHANGED_EVENT);
		}
		
		public String getID() { 
			return ID_PREFIX + fName;
		}
		
		public void setManager(ProfileManager profileManager) {
			fManager= profileManager;
		}
		
		public ProfileManager getManager() {
			return fManager;
		}
		
		protected void notifyObservers(int message) {
			if (fManager != null)
				fManager.notifyObservers(message);
		}
		
		public void remove() {
			if (fManager != null) {
				fManager.fProfiles.remove(getID());
				fManager.fProfilesByName.remove(this);
				fManager= null;
			}
		}

		public int getVersion() {
			return fVersion;
		}
		
		public void setVersion(int version)	{
			fVersion= version;
		}
		
		public int compareTo(Object o) {
			if (o instanceof SharedProfile) {
				return -1;
			}
			if (o instanceof CustomProfile) {
				return getName().compareToIgnoreCase(((Profile)o).getName());
			}
			return 1;
		}
		
		public boolean isProfileToSave() {
			return true;
		}

	}
	
	public final static class SharedProfile extends CustomProfile {
		
		public SharedProfile(Map options) {
			super(FormatterMessages.getString("ProfileManager.unnamed_profile.name0"), options, ProfileVersioner.CURRENT_VERSION); //$NON-NLS-1$
		}
		
		public Profile rename(String name) {
			final String oldID= getID();
			CustomProfile profile= new CustomProfile(name.trim(), getSettings(), getVersion());

			if (fManager != null) { 
				fManager.fProfiles.remove(oldID);
				fManager.fProfiles.put(profile.getID(), profile);
				fManager.fProfilesByName.remove(this);
				fManager.fProfilesByName.add(profile);
				Collections.sort(fManager.fProfilesByName);
				fManager.setSelected(profile);
				notifyObservers(PROFILE_CREATED_EVENT);
				notifyObservers(SELECTION_CHANGED_EVENT);
			}
			return this;
		}
				
		public String getID() { 
			return SHARED_PROFILE;
		}
		
		public final int compareTo(Object o) {
			return 1;
		}
		
		public boolean isProfileToSave() {
			return false;
		}
	}
	

	/**
	 * The possible events for observers listening to this class.
	 */
	public final static int SELECTION_CHANGED_EVENT= 1;
	public final static int PROFILE_DELETED_EVENT= 2;
	public final static int PROFILE_RENAMED_EVENT= 3;
	public final static int PROFILE_CREATED_EVENT= 4;
	public final static int SETTINGS_CHANGED_EVENT= 5;
	
	
	/**
	 * The key of the preference where the selected profile is stored.
	 */
	private final static String PROFILE_KEY= PreferenceConstants.FORMATTER_PROFILE; 

	
	/**
	 * The keys of the built-in profiles
	 */
	public final static String ECLIPSE21_PROFILE= "org.eclipse.jdt.ui.default_profile"; //$NON-NLS-1$
	public final static String JAVA_PROFILE= "org.eclipse.jdt.ui.default.sun_profile"; //$NON-NLS-1$
	public final static String SHARED_PROFILE= "org.eclipse.jdt.ui.default.shared"; //$NON-NLS-1$
	
	
	/**
	 * A map containing the available profiles, using the IDs as keys.
	 */
	protected final Map fProfiles;
	
	
	
	
	/**
	 * The available profiles, sorted by name.
	 */
	protected final List fProfilesByName;
	

	/**
	 * The currently selected profile. 
	 */
	private Profile fSelected;
	

	/**
	 * The keys of the options to be saved with each profile
	 */
	private final static List fUIKeys= Arrays.asList(new CommentFormattingContext().getPreferenceKeys()); 
	private final static List fCoreKeys= new ArrayList(DefaultCodeFormatterConstants.getJavaConventionsSettings().keySet());

	/**
	 * All keys appearing in a profile, sorted alphabetically
	 */
	private final static List fKeys;
	
	static {
	    fKeys= new ArrayList();
	    fKeys.addAll(fUIKeys);
	    fKeys.addAll(fCoreKeys);
	    Collections.sort(fKeys);
	}
	

	/**
	 * Create and initialize a new profile manager.
	 * @param profiles Initial custom profiles (List of type <code>CustomProfile</code>)
	 */
	public ProfileManager(List profiles, IScopeContext context) {
		fProfiles= new HashMap();
		fProfilesByName= new ArrayList();
	
		addBuiltinProfiles(fProfiles, fProfilesByName);
		
		for (final Iterator iter = profiles.iterator(); iter.hasNext();) {
			final CustomProfile profile= (CustomProfile) iter.next();
			profile.setManager(this);
			fProfiles.put(profile.getID(), profile);
			fProfilesByName.add(profile);
		}
		
		Collections.sort(fProfilesByName);
		
		String profileId= new InstanceScope().getNode(JavaUI.ID_PLUGIN).get(PROFILE_KEY, JAVA_PROFILE);
		Profile profile= (Profile) fProfiles.get(profileId);
		if (profile == null) {
			profile= (Profile) fProfiles.get(JAVA_PROFILE);
		}
		fSelected= profile;
		
		if (context.getName() == ProjectScope.SCOPE) {
			
			Map map= readFromPreferenceStore(context, profile);
			if (map != null) {
				Profile matching= null;
				for (int i= 0; matching == null && i < fProfilesByName.size(); i++) {
					Profile curr= (Profile) fProfilesByName.get(i);
					if (curr.hasEqualSettings(map, getKeys())) {
						matching= curr;
					}
				}
				if (matching == null) {
					// current settings do not currespond to any profile -> create a 'team' profile
					SharedProfile shared= new SharedProfile(map);
					shared.setManager(this);
					fProfiles.put(shared.getID(), shared);
					fProfilesByName.add(shared); // add last
					matching= shared;
				}
				fSelected= matching;
			}
		}
	}
	

	/**
	 * Notify observers with a message. The message must be one of the following:
	 * @param message Message to send out
	 * 
	 * @see #SELECTION_CHANGED_EVENT
	 * @see #PROFILE_DELETED_EVENT
	 * @see #PROFILE_RENAMED_EVENT
	 * @see #PROFILE_CREATED_EVENT
	 * @see #SETTINGS_CHANGED_EVENT
	 */
	protected void notifyObservers(int message) {
		setChanged();
		notifyObservers(new Integer(message));
	}
	
	public boolean hasProjectSpecificSettings(IScopeContext context) {
		IEclipsePreferences corePrefs= context.getNode(JavaCore.PLUGIN_ID);
		for (final Iterator keyIter = fCoreKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			Object val= corePrefs.get(key, null);
			if (val != null) {
				return true;
			}
		}
		
		IEclipsePreferences uiPrefs= context.getNode(JavaUI.ID_PLUGIN);
		for (final Iterator keyIter = fUIKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			Object val= uiPrefs.get(key, null);
			if (val != null) {
				return true;
			}
		}
		return false;
	}

	
	/**
	 * Only to read project specific settings to find out to what profile it matches.
	 * @param context The project context
	 */
	public Map readFromPreferenceStore(IScopeContext context, Profile workspaceProfile) {
		final Map profileOptions= new HashMap();
		
		boolean hasValues= false;
		IEclipsePreferences corePrefs= context.getNode(JavaCore.PLUGIN_ID);
		for (final Iterator keyIter = fCoreKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			Object val= corePrefs.get(key, null);
			if (val != null) {
				hasValues= true;
			} else {
				val= workspaceProfile.getSettings().get(key);
			}
			profileOptions.put(key, val);
		}
		
		IEclipsePreferences uiPrefs= context.getNode(JavaUI.ID_PLUGIN);
		
		for (final Iterator keyIter = fUIKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			Object val= uiPrefs.get(key, null);
			if (val != null) {
				hasValues= true;
			} else {
				val= workspaceProfile.getSettings().get(key);
			}
			profileOptions.put(key, val);
		}
		
		if (!hasValues) {
			return null;
		}
		ProfileVersioner.setLatestCompliance(profileOptions);
		return profileOptions;
	}
	
	
	/**
	 * Update all formatter settings with the settings of the specified profile. 
	 * @param profile The profilde to write to the preference store
	 */
	private void writeToPreferenceStore(Profile profile, IScopeContext context) {
		final Map profileOptions= profile.getSettings();
		
		IEclipsePreferences corePrefs= context.getNode(JavaCore.PLUGIN_ID);
		
		for (final Iterator keyIter = fCoreKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			final String oldVal= corePrefs.get(key, null);
			final String val= (String) profileOptions.get(key);
			if (val == null) {
				if (oldVal != null) {
					corePrefs.remove(key);
				}
			} else if (!val.equals(oldVal)) {
				corePrefs.put(key, val);
			}
		}
		try {
			corePrefs.flush();
		} catch (BackingStoreException e) {
			JavaPlugin.log(e);
		}
		
		IEclipsePreferences uiPrefs= context.getNode(JavaUI.ID_PLUGIN);
		
		for (final Iterator keyIter = fUIKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			final String oldVal= uiPrefs.get(key, null);
			final String val= (String) profileOptions.get(key);
			if (val == null) {
				if (oldVal != null) {
					corePrefs.remove(key);
				}
			} else if (!val.equals(oldVal)) {
				uiPrefs.put(key, val);
			}
		}
		
		if (context.getName() == InstanceScope.SCOPE) {
			final String oldProfile= uiPrefs.get(PROFILE_KEY, null);
			if (!profile.getID().equals(oldProfile)) {
				uiPrefs.put(PROFILE_KEY, profile.getID());
			}
		}
		try {
			uiPrefs.flush();
		} catch (BackingStoreException e) {
			JavaPlugin.log(e);
		}
	}
	
	
	/**
	 * Add all the built-in profiles to the map and to the list.
	 * @param profiles The map to add the profiles to
	 * @param profilesByName List of profiles by
	 */
	private void addBuiltinProfiles(Map profiles, List profilesByName) {
		final Profile javaProfile= new BuiltInProfile(JAVA_PROFILE, FormatterMessages.getString("ProfileManager.java_conventions_profile.name"), getJavaSettings(), 1); //$NON-NLS-1$
		profiles.put(javaProfile.getID(), javaProfile);
		profilesByName.add(javaProfile);
		
		final Profile eclipse21Profile= new BuiltInProfile(ECLIPSE21_PROFILE, FormatterMessages.getString("ProfileManager.default_profile.name"), getEclipse21Settings(), 2); //$NON-NLS-1$
		profiles.put(eclipse21Profile.getID(), eclipse21Profile);
		profilesByName.add(eclipse21Profile);
	}
	
	
	/**
	 * @return Returns the settings for the default profile.
	 */	
	public static Map getEclipse21Settings() {
		final Map options= DefaultCodeFormatterConstants.getEclipse21Settings();
		new CommentFormattingContext().storeToMap(getUIPreferenceStore(), options, true);

		ProfileVersioner.setLatestCompliance(options);
		return options;
	}

	/** 
	 * @return Returns the settings for the Java Conventions profile.
	 */
	public static Map getJavaSettings() {
		final Map options= DefaultCodeFormatterConstants.getJavaConventionsSettings();
		new CommentFormattingContext().storeToMap(getUIPreferenceStore(), options, true);

		ProfileVersioner.setLatestCompliance(options);
		return options;
	}
	
	
	/**
	 * @return All keys appearing in a profile, sorted alphabetically.
	 */
	public static List getKeys() {
	    return fKeys;
	}
	
	
	/** 
	 * Get an immutable list as view on all profiles, sorted alphabetically. Unless the set 
	 * of profiles has been modified between the two calls, the sequence is guaranteed to 
	 * correspond to the one returned by <code>getSortedNames</code>.
	 * @return Al list of elements of type <code>Profile</code>
	 * 
	 * @see #getSortedNames()
	 */
	public List getSortedProfiles() {
		return Collections.unmodifiableList(fProfilesByName);
	}

	/**
	 * Get the names of all profiles stored in this profile manager, sorted alphabetically. Unless the set of 
	 * profiles has been modified between the two calls, the sequence is guaranteed to correspond to the one 
	 * returned by <code>getSortedProfiles</code>.
	 * @return All names, sorted alphabetically
	 * @see #getSortedProfiles()  
	 */	
	public String [] getSortedNames() {
		final String [] sortedNames= new String[fProfilesByName.size()];
		int i= 0;
		for (final Iterator iter = fProfilesByName.iterator(); iter.hasNext();) {
			sortedNames[i++]= ((Profile) iter.next()).getName();
		}
		return sortedNames;
	}
	
	/**
	 * Get the profile for this profile id.
	 * @param ID The profile ID
	 * @return The profile with the given ID or <code>null</code> 
	 */
	public Profile getProfile(String ID) {
		return (Profile)fProfiles.get(ID);
	}
	
	/**
	 * Activate the selected profile, update all necessary options in
	 * preferences and save profiles to disk.
	 */
	public void commitChanges(IScopeContext scopeContext) {
		if (fSelected != null) {
			writeToPreferenceStore(fSelected, scopeContext);
		}
	}
	
	public void clearAllSettings(IScopeContext context) {
		IEclipsePreferences corePrefs= context.getNode(JavaCore.PLUGIN_ID);
		for (final Iterator keyIter = fCoreKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			corePrefs.remove(key);
		}
		
		IEclipsePreferences uiPrefs= context.getNode(JavaUI.ID_PLUGIN);
		for (final Iterator keyIter = fUIKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			uiPrefs.remove(key);
		}
	}
	
	/**
	 * Get the currently selected profile.
	 * @return The currently selected profile.
	 */
	public Profile getSelected() {
		return fSelected;
	}

	/**
	 * Set the selected profile. The profile must already be contained in this profile manager.
	 * @param profile The profile to select
	 */
	public void setSelected(Profile profile) {
		final Profile newSelected= (Profile)fProfiles.get(profile.getID());
		if (newSelected != null && !newSelected.equals(fSelected)) {
			fSelected= newSelected;
			notifyObservers(SELECTION_CHANGED_EVENT);
		}
	}

	/**
	 * Check whether a user-defined profile in this profile manager
	 * already has this name.
	 * @param name The name to test for
	 * @return Returns <code>true</code> if a profile with the given name exists
	 */
	public boolean containsName(String name) {
		return fProfiles.containsKey(ID_PREFIX + name);
	}
	
	/**
	 * Add a new custom profile to this profile manager.
	 * @param profile The profile to add
	 */	
	public void addProfile(CustomProfile profile) {
		profile.setManager(this);
		final CustomProfile oldProfile= (CustomProfile)fProfiles.get(profile.getID());
		if (oldProfile != null) {
			oldProfile.remove();
		}
		fProfiles.put(profile.getID(), profile);
		fProfilesByName.add(profile);
		Collections.sort(fProfilesByName);
		fSelected= profile;
		notifyObservers(PROFILE_CREATED_EVENT);
	}
	
	/**
	 * Delete the currently selected profile from this profile manager. The next profile
	 * in the list is selected.
	 * @return true if the profile has been successfully removed, false otherwise.
	 */
	public boolean deleteSelected() {
		if (!(fSelected instanceof CustomProfile)) 
			return false;
		
		int index= fProfilesByName.indexOf(fSelected);
		((CustomProfile)fSelected).remove();
		
		if (index >= fProfilesByName.size())
			index--;
		fSelected= (Profile)fProfilesByName.get(index);

		notifyObservers(PROFILE_DELETED_EVENT);
		return true;
	}

	/**
	 * Get the UI preference store.
	 * @return Returns the preference store
	 */
	private static IPreferenceStore getUIPreferenceStore() {
		return PreferenceConstants.getPreferenceStore();
	}
}
