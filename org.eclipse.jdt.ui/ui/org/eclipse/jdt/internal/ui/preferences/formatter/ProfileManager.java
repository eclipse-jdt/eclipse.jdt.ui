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
import java.util.Collection;
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

public class ProfileManager extends Observable {
	
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
		
		public final int compareTo(Object o) {
			return getName().compareTo(((Profile)o).getName());
		}
	}
	
	/**
	 * Represents a built-in profile. The state of a built-in profile 
	 * cannot be changed after instantiation.
	 */
	public static class BuiltInProfile extends Profile {
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
	}

	/**
	 * Represents a user-defined profile. A custom profile can be modified after instantiation.
	 */
	public static class CustomProfile extends Profile {
		private String fName;
		private Map fSettings;
		private ProfileManager fManager;

		public CustomProfile(String name, Map settings) {
			fName= name;
			fSettings= settings;
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
	private final String PROFILE_KEY= PreferenceConstants.FORMATTER_PROFILE; 

	
	
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
	 * The profiles, sorted by name. If this is null, the collection has to be updated. 
	 */
	private ArrayList fSortedProfiles;
	
	
	/**
	 * The profile names, sorted by names.
	 */
	private String [] fSortedNames;
	
	
	/**
	 * The currently selected profile. 
	 */
	protected Profile fSelected;
	

	/**
	 * The keys of the options to be saved with each profile
	 */
	protected final List fUIKeys, fCoreKeys;
	
	/**
	 * The preference stores for Core and UI options.
	 */
	
	
	/**
	 * Create and initialize a new profile manager.
	 */
	public ProfileManager(Collection profiles) {
		fUIKeys= getUIKeys();
		fCoreKeys= getCoreKeys();
		fProfiles= new HashMap();
	
		addBuiltinProfiles(fProfiles);
		for (Iterator iter = profiles.iterator(); iter.hasNext();) {
			final Profile profile= (Profile) iter.next();
			fProfiles.put(profile.getID(), profile);
		}
		
		final String id= getUIPreferenceStore().getString(PROFILE_KEY);
		fSelected= (Profile)fProfiles.get(id);
		if (fSelected == null) {
			fSelected= (Profile)fProfiles.get(DEFAULT_PROFILE);
		}
	}
	

	/**
	 * Notify observers with a message.
	 */
	protected void notifyObservers(int message) {
		setChanged();
		notifyObservers(new Integer(message));
	}
	
	
	/**
	 * Update all formatter settings with the settings of the specified profile. 
	 *  
	 * @param Profile The profile
	 */
	private void writeToPreferenceStore(Profile profile) {
	
		final Hashtable actualOptions= JavaCore.getOptions();
		final Map profileOptions= profile.getSettings();
		
		boolean hasChanges= false;
		
		for (final Iterator keyIter = fCoreKeys.iterator(); keyIter.hasNext(); ) {
			final String key= (String) keyIter.next();
			final String oldVal= (String) actualOptions.get(key);
			String val= (String) profileOptions.get(key);
			if (!val.equals(oldVal)) {
				hasChanges= true;
				actualOptions.put(key, val);
			}
		}
		
		//TODO: remove after transition is done
//		if (DefaultCodeFormatterConstants.TRUE.equals(profileOptions.get(DefaultCodeFormatterConstants.FORMATTER_CONVERT_OLD_TO_NEW))) {
//			profileOptions.put(DefaultCodeFormatterConstants.FORMATTER_CONVERT_OLD_TO_NEW, DefaultCodeFormatterConstants.FALSE);
//			hasChanges= true;
//		}
		

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
	 * Add all the built-in profiles to the map
	 * 
	 * @param map The map where the profiles are to be added.
	 */

	private void addBuiltinProfiles(Map map) {
		map.put(DEFAULT_PROFILE, new BuiltInProfile(DEFAULT_PROFILE, "Default", getDefaultSettings()));
		map.put(JAVA_PROFILE, new BuiltInProfile(JAVA_PROFILE, "Java Conventions", getJavaSettings()));
	}
	
	
	
	public static Map getDefaultSettings() {
		final Map options= DefaultCodeFormatterConstants.getDefaultSettings();
		new CommentFormattingContext().storeToMap(getUIPreferenceStore(), options, true);
		return options;
	}
	
	public static Map getJavaSettings() {
		final Map options= DefaultCodeFormatterOptions.getJavaConventionsSettings().getMap();
			// TODO: change to DefaultCodeFormatterConstants.getJavaConventionsSettings();

		new CommentFormattingContext().storeToMap(getUIPreferenceStore(), options, true);
		return options;
	}
	
	private List getCoreKeys() {
		return new ArrayList(DefaultCodeFormatterConstants.getDefaultSettings().keySet());
	}
	
	private List getUIKeys() {
		return Arrays.asList( new CommentFormattingContext().getPreferenceKeys());
	}
	
	public List getSortedProfiles() {
		if (fSortedProfiles == null) {
			fSortedProfiles= new ArrayList(fProfiles.values());
			Collections.sort(fSortedProfiles);
		}
		return fSortedProfiles;
	}
	
	public String [] getSortedNames() {
		if (fSortedNames == null) {
			final List sortedProfiles= getSortedProfiles();
			fSortedNames= new String[sortedProfiles.size()];
			for (int i = 0; i < fSortedNames.length; i++) {
				fSortedNames[i]= ((Profile)sortedProfiles.get(i)).getName();
			}
		}
		return fSortedNames;
	}
	
	
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

	public void setSelected(Profile profile) {
		final Profile newSelected= (Profile)fProfiles.get(profile.getID());
		if (newSelected != null && !newSelected.equals(fSelected)) {
			fSelected= newSelected;
			notifyObservers(SELECTION_CHANGED_EVENT);
		}
	}
	
	public boolean containsName(String name) {
		return fProfiles.containsKey(ID_PREFIX + name);
	}
	
	public Profile createProfile(String name, Map settings) {
		if (name == null || settings == null) return null;
		final Profile profile= new CustomProfile(name, settings);
		fProfiles.put(profile.getID(), profile);
		fSortedProfiles= null;
		fSortedNames= null;
		fSelected= profile;
		notifyObservers(PROFILE_CREATED_EVENT);
		return profile;
	}
	
	public void addProfile(CustomProfile profile) {
		fProfiles.put(profile.getID(), profile);
		profile.setManager(this);
		fSortedProfiles= null;
		fSortedNames= null;
		fSelected= profile;
		notifyObservers(PROFILE_CREATED_EVENT);
	}
	
	public void deleteProfile(CustomProfile profile) {
		if (!fProfiles.containsKey(profile.getID())) 
			return;
		fProfiles.remove(profile);
		profile.setManager(null);
	}
	
	public boolean deleteSelected() {
		if (fSelected instanceof BuiltInProfile) 
			return false;

		int index= getSortedProfiles().indexOf(fSelected);
		
		fProfiles.remove(fSelected.getID());

		fSortedProfiles= null;
		fSortedNames= null;	
		
		if (index >= getSortedProfiles().size()) 
				--index;
		fSelected= (Profile)getSortedProfiles().get(index);

		notifyObservers(PROFILE_DELETED_EVENT);
		return true;
	}
	
	public static IPreferenceStore getUIPreferenceStore() {
		return PreferenceConstants.getPreferenceStore();
	}

}
