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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
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
		public abstract void setName(String name);
		
		public abstract Map getSettings();
		public abstract void setSettings(Map settings);
		
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
		
		protected BuiltInProfile(String ID, String name, Map settings) {
			fName= name;
			fID= ID;
			fSettings= settings;
		}
		
		public String getName() { 
			return fName;	
		}
		
		public void setName(String name) {
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
				return getName().compareToIgnoreCase(((Profile)o).getName());
			}
			return -1;
		}

	}

	/**
	 * Represents a user-defined profile. A custom profile can be modified after instantiation.
	 */
	public final static class CustomProfile extends Profile {
		private String fName;
		private Map fSettings;
		private ProfileManager fManager;
		private int fVersion;

		public CustomProfile(String name, Map settings, int version) {
			fName= name;
			fSettings= settings;
			fVersion= version;
		}
		
		public String getName() {
			return fName;
		}
		
		public void setName(String name) {
			final String trimmed= name.trim();
			if (trimmed.equals(getName())) 
				return;
			
			final String oldID= getID();
			fName= trimmed;
			
			if (fManager != null) { 
				fManager.fProfiles.remove(oldID);
				fManager.fProfiles.put(getID(), this);
				Collections.sort(fManager.fProfilesByName);
				notifyObservers(PROFILE_RENAMED_EVENT);
			}
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
		
		private void notifyObservers(int message) {
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
		
		public final int compareTo(Object o) {
			if (o instanceof CustomProfile) {
				return getName().compareToIgnoreCase(((Profile)o).getName());
			}
			return 1;
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
	public final static String DEFAULT_PROFILE= "org.eclipse.jdt.ui.default_profile"; //$NON-NLS-1$
	public final static String JAVA_PROFILE= "org.eclipse.jdt.ui.default.sun_profile"; //$NON-NLS-1$
	
	
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
	private final static List fCoreKeys= new ArrayList(DefaultCodeFormatterConstants.getDefaultSettings().keySet()); 

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
	 */
	public ProfileManager(List profiles) {
		fProfiles= new HashMap();
		fProfilesByName= new ArrayList();
	
		for (final Iterator iter = profiles.iterator(); iter.hasNext();) {
			final CustomProfile profile= (CustomProfile) iter.next();
			profile.setManager(this);
			fProfiles.put(profile.getID(), profile);
			fProfilesByName.add(profile);
		}

		addBuiltinProfiles(fProfiles, fProfilesByName);
		
		Collections.sort(fProfilesByName);
		
		final String id= getUIPreferenceStore().getString(PROFILE_KEY);
		fSelected= (Profile)fProfiles.get(id);
		if (fSelected == null) {
			fSelected= (Profile)fProfiles.get(DEFAULT_PROFILE);
		}
	}
	

	/**
	 * Notify observers with a message. The message must be one of the following:
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
	
	
	/**
	 * Update all formatter settings with the settings of the specified profile. 
	 */
	private void writeToPreferenceStore(Profile profile) {
	
		final Hashtable actualOptions= JavaCore.getOptions();
		final Map profileOptions= profile.getSettings();
		
		boolean hasChanges= false;
		
		for (final Iterator keyIter = fCoreKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			final String oldVal= (String) actualOptions.get(key);
			final String val= (String) profileOptions.get(key);
			if (!val.equals(oldVal)) {
				hasChanges= true;
				actualOptions.put(key, val);
			}
		}
		
		if (hasChanges) {
			JavaCore.setOptions(actualOptions);
		}
		
		new CommentFormattingContext().mapToStore(profile.getSettings(), getUIPreferenceStore());
		
		final String oldProfile= getUIPreferenceStore().getString(PROFILE_KEY);
		if (!profile.getID().equals(oldProfile)) {
			getUIPreferenceStore().setValue(PROFILE_KEY, profile.getID());
		}
	}
	
	
	/**
	 * Add all the built-in profiles to the map and to the list.
	 */
	private void addBuiltinProfiles(Map profiles, List profilesByName) {
		final Profile defaultProfile= new BuiltInProfile(DEFAULT_PROFILE, FormatterMessages.getString("ProfileManager.default_profile.name"), getDefaultSettings()); //$NON-NLS-1$
		profiles.put(defaultProfile.getID(), defaultProfile);
		profilesByName.add(defaultProfile);
		
		final Profile javaProfile= new BuiltInProfile(JAVA_PROFILE, FormatterMessages.getString("ProfileManager.java_conventions_profile.name"), getJavaSettings()); //$NON-NLS-1$
		profiles.put(javaProfile.getID(), javaProfile);
		profilesByName.add(javaProfile);
	}
	
	
	/**
	 * Get the settings for the default profile.
	 */	
	public static Map getDefaultSettings() {
		final Map options= DefaultCodeFormatterConstants.getDefaultSettings();
		new CommentFormattingContext().storeToMap(getUIPreferenceStore(), options, true);
		return options;
	}

	/** 
	 * Get the settings for the Java Conventions profile.
	 */
	public static Map getJavaSettings() {
		final Map options= DefaultCodeFormatterOptions.getJavaConventionsSettings().getMap();
		new CommentFormattingContext().storeToMap(getUIPreferenceStore(), options, true);
		return options;
	}
	
	
	/**
	 * All keys appearing in a profile, sorted alphabetically.
	 */
	public static List getKeys() {
	    return fKeys;
	}
	
	
	/** 
	 * Get an immutable list as view on all profiles, sorted alphabetically. Unless the set 
	 * of profiles has been modified between the two calls, the sequence is guaranteed to 
	 * correspond to the one returned by <code>getSortedNames</code>.
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
	 */
	public Profile getProfile(String ID) {
		return (Profile)fProfiles.get(ID);
	}
	
	/**
	 * Activate the selected profile, update all necessary options in
	 * preferences and save profiles to disk.
	 */
	public void commitChanges() {
		if (fSelected != null) {
			writeToPreferenceStore(fSelected);
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
	 */
	public boolean containsName(String name) {
		return fProfiles.containsKey(ID_PREFIX + name);
	}
	
	/**
	 * Add a new custom profile to this profile manager.
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
	 */
	private static IPreferenceStore getUIPreferenceStore() {
		return PreferenceConstants.getPreferenceStore();
	}
}
