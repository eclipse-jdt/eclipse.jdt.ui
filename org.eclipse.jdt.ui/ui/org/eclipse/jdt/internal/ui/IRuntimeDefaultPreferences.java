/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui;


import org.eclipse.jface.preference.IPreferenceStore;

/**
 * A IRuntimeSettings object initializes preferences by looking at the
 * JDK we're running on.
 */

public interface IRuntimeDefaultPreferences {
	/**
	 * returns whether this IRuntimeSetting matches the runtime the
	 * executing program is running on.
	 */
	boolean matches();
	
	/**
	 * set the default preferences by looking at the java system variables
	 * and knowledge about what which JDK this IRuntimeSettings object
	 * represents.
	 */
	void setDefaultPreferences(IPreferenceStore preferences);
}