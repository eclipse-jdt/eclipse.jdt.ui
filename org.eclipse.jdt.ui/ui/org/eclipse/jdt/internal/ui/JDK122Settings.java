package org.eclipse.jdt.internal.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.internal.ui.launcher.JDK12PreferencePage;
import org.eclipse.jdt.internal.ui.launcher.VMPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JDKZipFieldEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * @see IRuntimeSettings
 */
public class JDK122Settings implements IRuntimeDefaultPreferences {

	public boolean matches() {
		String vendor= System.getProperty("java.vendor");
		if (!(vendor.startsWith("Sun") || vendor.startsWith("IBM")))
			return false;
		if ("J9".equals(System.getProperty("java.vm.name")))
			return false;
		String version= System.getProperty("java.version");
		if (version == null)
			return false;
		return version.startsWith("1.2") || version.startsWith("1.3");
		
	}
	public void setDefaultPreferences(IPreferenceStore preferences) {
		File javaHome= new File (System.getProperty("java.home"));
		File java= new File(javaHome.getParent(), File.separator+"bin"+File.separator+"java");
		File javaExe= new File(javaHome.getParent(), File.separator+"bin"+File.separator+"java.exe");
		if (javaExe.isFile() || java.isFile())
			javaHome= new File(javaHome.getParent());

		File rtJar= new File(javaHome, "jre"+File.separator+"lib"+ File.separator+"rt.jar");
		if (!rtJar.exists())
			rtJar= new File(javaHome, "lib"+ File.separator+"rt.jar");
			
		if (rtJar.exists()) {	
			preferences.setDefault(JavaBasePreferencePage.PROP_JDK, rtJar.getAbsolutePath());
		}
		
		File javaSource= new File(javaHome, "src.jar");
		if (!javaSource.exists())
			javaSource= new File(javaHome.getParent(), "src.jar");
		if (!javaSource.exists())
			javaSource= new File(javaHome, "lib"+ File.separator+"src.jar");
		if (javaSource.exists()) {
			preferences.setDefault(JDKZipFieldEditor.PROP_SOURCE, javaSource.getAbsolutePath());
			preferences.setDefault(JDKZipFieldEditor.PROP_PREFIX, "src");
		}
		
		preferences.setDefault(JDK12PreferencePage.PREF_LOCATION, javaHome.getAbsolutePath());

		String[] vms= JavaRuntime.getJavaRuntimes();
		for (int i= 0; i < vms.length; i++) {
			if ("JDK 1.2.2".equals(vms[i])) {
				preferences.setDefault(VMPreferencePage.PREF_VM, "JDK 1.2.2");
				break;
			}
		}
	}
}