package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Preference for experimental code assist method completion.
 */
public class ExperimentalPreference {

	public static final String CODE_ASSIST_EXPERIMENTAL= "org.eclipse.jdt.ui.text.codeassist.experimental"; //$NON-NLS-1$
	
	public static boolean fillArgumentsOnMethodCompletion(IPreferenceStore store) {
		return store.getBoolean(CODE_ASSIST_EXPERIMENTAL);
	}

}

