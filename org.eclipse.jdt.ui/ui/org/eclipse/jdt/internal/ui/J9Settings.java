/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui;

import java.io.File;import org.eclipse.jdt.internal.ui.preferences.JDKZipFieldEditor;import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @see IRuntimeSettings
 */
public class J9Settings implements IRuntimeDefaultPreferences {

	public boolean matches() {
		return "J9".equals(System.getProperty("java.vm.name"));		
	}
	public void setDefaultPreferences(IPreferenceStore preferences) {
		/*File javaHome= new File (System.getProperty("java.home"));
		File rtJar= new File(javaHome, "lib"+File.separator+"jclMax"+File.separator+"classes.zip");
		if (rtJar.exists()) {	
			preferences.setDefault(JavaBasePreferencePage.PROP_JDK, rtJar.getAbsolutePath());
		}
		
		File javaSource= new File(javaHome, "src.jar");
		if (!javaSource.exists()) {
			javaSource= new File(rtJar.getParent(), "source"+File.separator+ "source.zip");
		}
		if (javaSource.exists()) {
			preferences.setDefault(JDKZipFieldEditor.PROP_SOURCE, javaSource.getAbsolutePath());
			preferences.setDefault(JDKZipFieldEditor.PROP_PREFIX, "");
		}
		
		org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixMeSoon("removed for testing");

		preferences.setDefault(J9PreferencePage.PREF_LOCATION, javaHome.getAbsolutePath());
		
		
		String[] vms= JavaRuntime.getJavaRuntimes();
		for (int i= 0; i < vms.length; i++) {
			if ("J9".equals(vms[i])) {
				preferences.setDefault(VMPreferencePage.PREF_VM, "J9");
				break;
			}
		}*/
	}
}